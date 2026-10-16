package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceAutomation;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceSettingsService;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceWriter;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoSimulatorSwitch;
import uz.azizbek.maktabboshqaruv.service.attendance.DeviceCheckinService;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Attendance automation over HTTP: the device check-in (public, API key), the school's device
 * keys, "✓ Hammasini tasdiqlash", "Maktab sozlamalari", the demo tools and the staff bell.
 */
@RestController
public class AttendanceAutomationController {

    private final DeviceCheckinService checkins;
    private final AttendanceWriter writer;
    private final AttendanceSettingsService settings;
    private final AttendanceAutomation automation;
    private final DemoSimulatorSwitch demo;
    private final LessonSlotRepository lessons;
    private final SchoolAccessService access;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public AttendanceAutomationController(DeviceCheckinService checkins, AttendanceWriter writer,
                                          AttendanceSettingsService settings, AttendanceAutomation automation,
                                          DemoSimulatorSwitch demo, LessonSlotRepository lessons,
                                          SchoolAccessService access, JdbcTemplate jdbc, Clock clock) {
        this.checkins = checkins;
        this.writer = writer;
        this.settings = settings;
        this.automation = automation;
        this.demo = demo;
        this.lessons = lessons;
        this.access = access;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ device (public, API key)

    /** A turnstile / QR / face reader reports an arrival. 401 without a valid school API key. */
    @PostMapping("/api/attendance/checkin")
    public DeviceCheckinService.CheckinResult checkin(@RequestHeader(value = "X-Api-Key", required = false) String apiKey,
                                                      @RequestBody DeviceCheckinService.CheckinRequest request) {
        return checkins.checkin(apiKey, request);
    }

    @GetMapping("/api/device-keys")
    @PreAuthorize("hasRole('ADMIN')")
    public List<DeviceCheckinService.KeyView> keys(@RequestParam Long schoolId) {
        return checkins.list(schoolId);
    }

    public record NewKey(Long schoolId, String name) {
    }

    @PostMapping("/api/device-keys")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceCheckinService.CreatedKey createKey(@RequestBody NewKey body, Authentication auth) {
        if (body.schoolId() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "schoolId kerak");
        return checkins.create(body.schoolId(), body.name(), auth.getName());
    }

    @DeleteMapping("/api/device-keys/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public DeviceCheckinService.KeyView revokeKey(@PathVariable Long id) {
        return checkins.revoke(id);
    }

    // ------------------------------------------------------------------ confirm automatic marks

    /** "✓ Hammasini tasdiqlash": the lesson's 🤖 marks become the caller's MANUAL marks. */
    @PostMapping("/api/attendance/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public Map<String, Integer> confirm(@RequestParam Long lessonSlotId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                       Authentication auth) {
        LessonSlot lesson = lessons.findById(lessonSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars mavjud emas"));
        long schoolId = lesson.getSchoolClass().getAcademicYear().getSchool().getId();
        access.requireSchool(schoolId);
        return Map.of("confirmed", writer.confirmAutomatic(schoolId, lessonSlotId, date, auth.getName()));
    }

    // ------------------------------------------------------------------ school settings

    @GetMapping("/api/school-settings")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    public AttendanceSettingsService.Settings getSettings(@RequestParam Long schoolId) {
        return settings.forSchool(schoolId);
    }

    @PutMapping("/api/school-settings")
    @PreAuthorize("hasRole('ADMIN')")
    public AttendanceSettingsService.Settings saveSettings(@RequestParam Long schoolId,
                                                           @Valid @RequestBody AttendanceSettingsService.Settings body,
                                                           Authentication auth) {
        return settings.save(schoolId, body, auth.getName());
    }

    // ------------------------------------------------------------------ demo

    /** For the yellow banner and the demo buttons: is the simulator on (application.properties only). */
    @GetMapping("/api/system/info")
    public Map<String, Object> info() {
        return Map.of("demoSimulator", demo.enabled(), "demoDelayMinutes", demo.delayMinutes());
    }

    @PostMapping("/api/demo/attendance/backfill")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> backfill(@RequestParam Long schoolId, @RequestParam(defaultValue = "30") int days) {
        if (!demo.enabled()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Demo simulyator yoqilmagan — faqat application.properties orqali yoqiladi");
        }
        return Map.of("inserted", automation.backfillDemo(schoolId, days));
    }

    /** Removes the school's DEMO_SIM marks only (allowed even after the simulator is switched off). */
    @DeleteMapping("/api/demo/attendance")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> clearDemo(@RequestParam Long schoolId) {
        return Map.of("deleted", writer.deleteDemo(schoolId));
    }

    // ------------------------------------------------------------------ staff bell

    /** The caller's in-app messages (newest first); users without an employee have none. */
    @GetMapping("/api/staff-notifications")
    public List<Map<String, Object>> mine(@RequestParam(defaultValue = "false") boolean unreadOnly) {
        Long employeeId = access.caller().employeeId();
        if (employeeId == null) return List.of();
        return jdbc.queryForList("select id, text, link, created_at as \"createdAt\", read_at as \"readAt\" "
                + "from staff_notification where employee_id = ? " + (unreadOnly ? "and read_at is null " : "")
                + "order by created_at desc limit 30", employeeId);
    }

    @PostMapping("/api/staff-notifications/read")
    public Map<String, Integer> markRead(@RequestParam(required = false) Long id) {
        Long employeeId = access.caller().employeeId();
        if (employeeId == null) return Map.of("read", 0);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
        int n = id == null
                ? jdbc.update("update staff_notification set read_at = ? where employee_id = ? and read_at is null", now, employeeId)
                : jdbc.update("update staff_notification set read_at = ? where id = ? and employee_id = ? and read_at is null", now, id, employeeId);
        return Map.of("read", n);
    }
}
