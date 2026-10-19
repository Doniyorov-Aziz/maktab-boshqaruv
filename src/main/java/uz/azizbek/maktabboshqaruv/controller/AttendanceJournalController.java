package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.pages.AttendancePageService;
import uz.azizbek.maktabboshqaruv.service.pages.JournalPageService;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;

/**
 * The "Davomat olish" and "Baholar jurnali" pages. Reading is for VIEWER and up (VIEWERs get no
 * phone numbers), marking for EDITOR and up. schoolId / classId / lessonSlotId / studentId /
 * subjectId are checked against the caller's school by the school-scope guard.
 */
@RestController
@RequestMapping("/api/pages")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
public class AttendanceJournalController {

    private final AttendancePageService attendance;
    private final JournalPageService journal;
    private final SchoolAccessService access;

    public AttendanceJournalController(AttendancePageService attendance, JournalPageService journal,
                                       SchoolAccessService access) {
        this.attendance = attendance;
        this.journal = journal;
        this.access = access;
    }

    private boolean phones() {
        return access.caller().role() != Role.VIEWER;
    }

    // ------------------------------------------------------------------ Davomat olish

    /** The class's lessons on that date's weekday, and every class's state for the picker. */
    @GetMapping("/attendance/day")
    public AttendancePageService.Day day(@RequestParam Long schoolId, @RequestParam Long classId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendance.day(schoolId, classId, date);
    }

    @GetMapping("/attendance/roster")
    public AttendancePageService.Roster roster(@RequestParam Long lessonSlotId,
                                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendance.roster(lessonSlotId, date, phones());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/attendance/save")
    public AttendancePageService.Saved save(@RequestParam Long schoolId, @RequestBody AttendancePageService.SaveRequest r) {
        return attendance.save(schoolId, r);
    }

    // ------------------------------------------------------------------ Baholar jurnali

    @GetMapping("/journal/summary")
    public JournalPageService.Summary summary(@RequestParam Long classId, @RequestParam Long subjectId,
                                              @RequestParam String month) {
        return journal.summary(classId, subjectId, YearMonth.parse(month));
    }

    @GetMapping("/journal/day-attendance")
    public Map<Long, String> dayAttendance(@RequestParam Long classId, @RequestParam Long subjectId,
                                           @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return journal.dayAttendance(classId, subjectId, date);
    }

    @GetMapping("/journal/student")
    public JournalPageService.StudentCard student(@RequestParam Long studentId, @RequestParam Long subjectId) {
        return journal.student(studentId, subjectId, phones());
    }

    @GetMapping("/journal/export.xlsx")
    public ResponseEntity<byte[]> xlsx(@RequestParam Long classId, @RequestParam Long subjectId, @RequestParam String month) {
        return file(journal.xlsx(classId, subjectId, YearMonth.parse(month)),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "jurnal-" + month + ".xlsx");
    }

    @GetMapping("/journal/export.pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Long classId, @RequestParam Long subjectId, @RequestParam String month) {
        return file(journal.pdf(classId, subjectId, YearMonth.parse(month)), "application/pdf", "jurnal-" + month + ".pdf");
    }

    private static ResponseEntity<byte[]> file(byte[] body, String type, String name) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(body);
    }
}
