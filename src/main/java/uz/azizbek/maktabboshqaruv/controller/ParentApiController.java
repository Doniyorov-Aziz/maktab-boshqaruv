package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.service.parent.ParentAccessService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.*;
import uz.azizbek.maktabboshqaruv.telegram.TelegramInitDataValidator;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only API of the Mini App "Farzandim kundaligi". No JWT here: every call
 * carries Telegram's signed initData (header X-Telegram-Init-Data), verified
 * with the bot token; the Telegram user id is the parent's private chat id,
 * and only children actively linked to that chat are ever returned.
 */
@RestController
@RequestMapping("/api/parent")
public class ParentApiController {

    public static final String HEADER = "X-Telegram-Init-Data";

    @Autowired
    private TelegramProperties properties;
    @Autowired
    private ParentAccessService access;
    @Autowired
    private ParentDataService data;
    @Autowired
    private Clock clock;

    private long chatId(String initData) {
        return TelegramInitDataValidator.validate(initData, properties.initDataSigningToken(),
                properties.getInitDataMaxAgeSeconds(), clock.instant()).id();
    }

    private Student student(String initData, Long studentId) {
        return access.requireLinked(chatId(initData), studentId);
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(HEADER) String initData) {
        long chatId = chatId(initData);
        ParentSession session = access.session(chatId);
        List<Student> students = access.linkedStudents(chatId);
        Student selected = students.isEmpty() ? null : access.selected(session);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("language", session.lang());
        result.put("selectedStudentId", selected == null ? null : selected.getId());
        result.put("children", students.stream().map(data::child).toList());
        return result;
    }

    @GetMapping("/students/{id}/today")
    public TodaySummary today(@RequestHeader(HEADER) String initData, @PathVariable Long id) {
        Student s = student(initData, id);
        return data.todaySummary(s, access.session(chatId(initData)));
    }

    @GetMapping("/students/{id}/schedule")
    public List<DaySchedule> schedule(@RequestHeader(HEADER) String initData, @PathVariable Long id,
                                      @RequestParam(defaultValue = "today") String day) {
        Student s = student(initData, id);
        LocalDate today = data.today();
        return switch (day) {
            case "tomorrow" -> List.of(data.day(s, today.plusDays(1)));
            case "week" -> data.week(s, today);
            default -> List.of(data.day(s, today));
        };
    }

    @GetMapping("/students/{id}/attendance")
    public Map<String, Object> attendance(@RequestHeader(HEADER) String initData, @PathVariable Long id,
                                          @RequestParam(required = false) String month) {
        Student s = student(initData, id);
        YearMonth ym = month == null ? YearMonth.from(data.today()) : YearMonth.parse(month);
        AttendanceSummary summary = data.attendance(s, "month", ym.toString(), ym.toString());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("incidents", data.incidents(s, summary.from(), summary.to()));
        result.put("bySubject", data.absencesBySubject(s, summary.from(), summary.to()));
        return result;
    }

    @GetMapping("/students/{id}/grades")
    public Map<String, Object> grades(@RequestHeader(HEADER) String initData, @PathVariable Long id) {
        Student s = student(initData, id);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("recent", data.recentGrades(s, 30));
        result.put("subjects", data.subjectAverages(s));
        result.put("quarter", data.quarterGrades(s));
        return result;
    }

    @GetMapping("/students/{id}/grades/{subjectId}")
    public SubjectDetail subject(@RequestHeader(HEADER) String initData, @PathVariable Long id, @PathVariable Long subjectId) {
        return data.subjectDetail(student(initData, id), subjectId);
    }

    @GetMapping("/students/{id}/announcements")
    public List<AnnouncementView> announcements(@RequestHeader(HEADER) String initData, @PathVariable Long id) {
        Student s = student(initData, id);
        return data.announcements(s, access.session(chatId(initData)));
    }

    @ExceptionHandler(TelegramInitDataValidator.InvalidInitData.class)
    public ResponseEntity<String> invalid(TelegramInitDataValidator.InvalidInitData e) {
        return ResponseEntity.status(401).body("Telegram ma'lumoti tasdiqlanmadi: " + e.getMessage());
    }

    @ExceptionHandler(ParentAccessService.AccessDenied.class)
    public ResponseEntity<String> denied(ParentAccessService.AccessDenied e) {
        return ResponseEntity.status(403).body("Bu ma'lumot sizga ochiq emas");
    }

    @ExceptionHandler(org.springframework.web.bind.MissingRequestHeaderException.class)
    public ResponseEntity<String> missingHeader() {
        return ResponseEntity.status(401).body("Mini App faqat Telegram ichida ochiladi");
    }
}
