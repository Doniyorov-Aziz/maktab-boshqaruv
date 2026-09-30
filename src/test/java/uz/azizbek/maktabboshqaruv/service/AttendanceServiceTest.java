package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.AttendanceBulkRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;
import uz.azizbek.maktabboshqaruv.repository.AttendanceRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uz.azizbek.maktabboshqaruv.service.TelegramTestFixtures.*;

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
    private ApplicationEventPublisher eventPublisher;

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
        when(studentRepository.findById(100L)).thenReturn(Optional.of(student));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private void existing(AttendanceStatus status) {
        Attendance a = new Attendance();
        a.setId(500L);
        a.setLessonSlot(slot);
        a.setStudent(student);
        a.setRecordDate(DATE);
        a.setStatus(status);
        when(attendanceRepository.findByLessonSlotIdAndRecordDate(1L, DATE)).thenReturn(List.of(a));
    }

    private AttendanceBulkRequestDto request(AttendanceStatus status) {
        AttendanceBulkRequestDto.Entry entry = new AttendanceBulkRequestDto.Entry();
        entry.setStudentId(100L);
        entry.setStatus(status);
        AttendanceBulkRequestDto request = new AttendanceBulkRequestDto();
        request.setLessonSlotId(1L);
        request.setRecordDate(DATE);
        request.setEntries(List.of(entry));
        return request;
    }

    @Test
    void resavingSameAbsentStatus_publishesNoEvent() {
        existing(AttendanceStatus.ABSENT);

        attendanceService.bulkMark(request(AttendanceStatus.ABSENT));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void presentToAbsent_publishesEvent() {
        existing(AttendanceStatus.PRESENT);

        attendanceService.bulkMark(request(AttendanceStatus.ABSENT));

        verify(eventPublisher).publishEvent(new AttendanceMarkedEvent(500L, AttendanceStatus.PRESENT, AttendanceStatus.ABSENT));
    }

    @Test
    void markingPresent_publishesNoEvent() {
        existing(AttendanceStatus.ABSENT);

        attendanceService.bulkMark(request(AttendanceStatus.PRESENT));

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void newLateRecord_publishesEvent() {
        when(attendanceRepository.findByLessonSlotIdAndRecordDate(1L, DATE)).thenReturn(List.of());

        attendanceService.bulkMark(request(AttendanceStatus.LATE));

        verify(eventPublisher).publishEvent(new AttendanceMarkedEvent(null, null, AttendanceStatus.LATE));
    }
}
