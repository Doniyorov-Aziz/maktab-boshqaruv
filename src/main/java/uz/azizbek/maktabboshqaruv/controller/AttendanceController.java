package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.AttendanceBulkRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AttendanceRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AttendanceResponseDto;
import uz.azizbek.maktabboshqaruv.dto.AttendanceRosterEntryDto;
import uz.azizbek.maktabboshqaruv.service.AttendanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private uz.azizbek.maktabboshqaruv.service.AttendanceCalendarService calendarService;

    /** Oylik kalendar: ?schoolClassId=5&month=2026-10. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/calendar")
    public uz.azizbek.maktabboshqaruv.service.AttendanceCalendarService.Month calendar(
            @RequestParam Long schoolClassId, @RequestParam String month) {
        return calendarService.month(schoolClassId, java.time.YearMonth.parse(month));
    }

    public record DayMark(@jakarta.validation.constraints.NotNull Long studentId,
                          @jakarta.validation.constraints.NotNull LocalDate date,
                          uz.azizbek.maktabboshqaruv.entity.AttendanceStatus status,
                          @jakarta.validation.constraints.Size(max = 300) String comment) {
    }

    /** One student's whole day from the calendar (status null = clear the day). */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/day")
    public uz.azizbek.maktabboshqaruv.service.AttendanceCalendarService.Cell markDay(@Valid @RequestBody DayMark mark) {
        return calendarService.setDay(mark.studentId(), mark.date(), mark.status(), mark.comment());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<AttendanceResponseDto> getAllAttendance(@RequestParam Long schoolId, Pageable pageable) {
        return attendanceService.getAllAttendance(schoolId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/today-lessons")
    public List<uz.azizbek.maktabboshqaruv.dto.TodayLessonDto> getTodayLessons(
            @RequestParam Long schoolId, @RequestParam(required = false) Long employeeId) {
        return attendanceService.getTodayLessons(schoolId, employeeId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/roster")
    public List<AttendanceRosterEntryDto> getRoster(@RequestParam Long lessonSlotId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return attendanceService.getRoster(lessonSlotId, date);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/bulk")
    public List<AttendanceRosterEntryDto> bulkMark(@Valid @RequestBody AttendanceBulkRequestDto request) {
        return attendanceService.bulkMark(request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<AttendanceResponseDto> createAttendance(@Valid @RequestBody AttendanceRequestDto request) {
        AttendanceResponseDto created = attendanceService.createAttendance(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<AttendanceResponseDto> updateAttendance(@PathVariable Long id, @Valid @RequestBody AttendanceRequestDto request) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.ok("ID " + id + " bilan davomat yozuvi muvaffaqiyatli o'chirildi");
    }
}
