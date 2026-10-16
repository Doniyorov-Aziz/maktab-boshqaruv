package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AttendanceBulkRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.AttendanceRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceWriter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uz.azizbek.maktabboshqaruv.service.TelegramTestFixtures.*;

/**
 * A lesson save goes through AttendanceWriter as MANUAL marks (the writer's priority and
 * parent-notification rules are tested on a real database in AttendanceAutomationIntegrationTest).
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 30);

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private LessonSlotRepository lessonSlotRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private AttendanceWriter writer;

    @InjectMocks
    private AttendanceService attendanceService;

    private LessonSlot slot;
    private Student student;

    @BeforeEach
    void setUp() {
        SchoolClass c = schoolClass(school(), 5L, 5, "A");
        slot = slot(c, 1L, "Matematika", LocalTime.of(8, 30));
        student = student(c, 100L, "Ali", "Valiyev");
        when(lessonSlotRepository.findById(1L)).thenReturn(Optional.of(slot));
        lenient().when(studentRepository.findById(100L)).thenReturn(Optional.of(student));
    }

    private AttendanceBulkRequestDto request(AttendanceStatus status) {
        AttendanceBulkRequestDto.Entry entry = new AttendanceBulkRequestDto.Entry();
        entry.setStudentId(100L);
        entry.setStatus(status);
        AttendanceBulkRequestDto request = new AttendanceBulkRequestDto();
        request.setLessonSlotId(1L);
        request.setRecordDate(DATE);
        request.setEntries(new java.util.ArrayList<>(List.of(entry)));
        return request;
    }

    @Test
    void bulkMark_savesEveryEntryAsOneManualSave() {
        attendanceService.bulkMark(request(AttendanceStatus.ABSENT));

        verify(writer).saveManual(eq(1L), eq(1L), eq(DATE),
                eq(List.of(new AttendanceWriter.ManualMark(100L, AttendanceStatus.ABSENT, null))), any());
    }

    @Test
    void bulkMark_refusesAStudentOfAnotherClass() {
        Student other = student(schoolClass(school(), 6L, 6, "B"), 101L, "Vali", "Aliyev");
        when(studentRepository.findById(101L)).thenReturn(Optional.of(other));
        AttendanceBulkRequestDto request = request(AttendanceStatus.PRESENT);
        request.getEntries().get(0).setStudentId(101L);

        assertThrows(IllegalStateException.class, () -> attendanceService.bulkMark(request));
        verify(writer, never()).saveManual(anyLong(), anyLong(), any(), any(), any());
    }
}
