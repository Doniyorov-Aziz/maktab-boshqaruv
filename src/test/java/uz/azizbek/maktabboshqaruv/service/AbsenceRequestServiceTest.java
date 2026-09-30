package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.AbsenceRequestRepository;
import uz.azizbek.maktabboshqaruv.repository.AttendanceRepository;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uz.azizbek.maktabboshqaruv.service.TelegramTestFixtures.*;

@ExtendWith(MockitoExtension.class)
class AbsenceRequestServiceTest {

    @Mock
    private AbsenceRequestRepository repository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private LessonSlotRepository lessonSlotRepository;
    @Mock
    private CalendarEventRepository calendarEventRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AbsenceRequestService service;

    private School school;
    private Student ali;
    private AbsenceRequest request;
    private LessonSlot wed1;
    private LessonSlot wed2;
    private LessonSlot thu1;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "clock",
                Clock.fixed(Instant.parse("2026-09-29T06:00:00Z"), ZoneId.of("Asia/Tashkent")));
        school = school();
        SchoolClass c = schoolClass(school, 5L, 5, "A");
        ali = student(c, 100L, "Ali", "Valiyev");
        wed1 = slot(c, 1L, "Matematika", LocalTime.of(8, 30)); // fixtures use weekday "Chorshanba"
        wed2 = slot(c, 2L, "Fizika", LocalTime.of(9, 25));
        thu1 = slot(c, 3L, "Kimyo", LocalTime.of(8, 30));
        thu1.setWeekday("Payshanba");

        request = new AbsenceRequest();
        request.setId(7L);
        request.setSchool(school);
        request.setStudent(ali);
        request.setChatId(555L);
        request.setDateFrom(LocalDate.of(2026, 9, 30)); // Wednesday
        request.setDateTo(LocalDate.of(2026, 10, 1));   // Thursday
        request.setReason(AbsenceReason.ILLNESS);
        request.setStatus(AbsenceStatus.PENDING);
        when(repository.findById(7L)).thenReturn(Optional.of(request));
        lenient().when(lessonSlotRepository.findBySchoolClassId(5L)).thenReturn(List.of(wed1, wed2, thu1));
    }

    @Test
    void approve_marksEveryLessonOfTheDaysExcused_updatingOrCreating() {
        Attendance existing = new Attendance();
        existing.setLessonSlot(wed1);
        existing.setStudent(ali);
        existing.setRecordDate(LocalDate.of(2026, 9, 30));
        existing.setStatus(AttendanceStatus.ABSENT);
        when(calendarEventRepository.findInRange(anyLong(), any(), any())).thenReturn(List.of());
        when(attendanceRepository.findByLessonSlotIdAndStudentIdAndRecordDate(eq(1L), eq(100L), any())).thenReturn(Optional.of(existing));
        when(attendanceRepository.findByLessonSlotIdAndStudentIdAndRecordDate(eq(2L), eq(100L), any())).thenReturn(Optional.empty());
        when(attendanceRepository.findByLessonSlotIdAndStudentIdAndRecordDate(eq(3L), eq(100L), any())).thenReturn(Optional.empty());

        var dto = service.approve(7L, "sinf_rahbari");

        ArgumentCaptor<Attendance> saved = ArgumentCaptor.forClass(Attendance.class);
        verify(attendanceRepository, times(3)).save(saved.capture());
        assertTrue(saved.getAllValues().stream().allMatch(a -> a.getStatus() == AttendanceStatus.EXCUSED));
        assertSame(existing, saved.getAllValues().get(0), "the ABSENT record is turned into EXCUSED, not duplicated");
        assertEquals(LocalDate.of(2026, 10, 1), saved.getAllValues().get(2).getRecordDate(), "Thursday's lesson is created in advance");
        assertEquals(AbsenceStatus.APPROVED, request.getStatus());
        assertEquals(3, dto.getExcusedLessons());
        verify(notificationService).enqueueDirect(eq(school), eq(ali), eq(555L), eq(NotificationType.ABSENCE_DECISION),
                eq(7L), any(), any(), isNull());
    }

    @Test
    void approve_skipsHolidays() {
        CalendarEvent holiday = new CalendarEvent();
        holiday.setType(CalendarEventType.HOLIDAY);
        holiday.setStartDate(LocalDate.of(2026, 10, 1));
        holiday.setEndDate(LocalDate.of(2026, 10, 1));
        when(calendarEventRepository.findInRange(anyLong(), any(), any())).thenReturn(List.of(holiday));
        when(attendanceRepository.findByLessonSlotIdAndStudentIdAndRecordDate(anyLong(), eq(100L), any())).thenReturn(Optional.empty());

        assertEquals(2, service.approve(7L, "admin").getExcusedLessons(), "only Wednesday's two lessons");
    }

    @Test
    void reject_needsReason_andNotifiesParent() {
        assertThrows(IllegalStateException.class, () -> service.reject(7L, " ", "admin"));
        service.reject(7L, "Ma'lumotnoma kerak", "admin");
        assertEquals(AbsenceStatus.REJECTED, request.getStatus());
        assertEquals("Ma'lumotnoma kerak", request.getDecisionNote());
        verify(attendanceRepository, never()).save(any());
        verify(notificationService).enqueueDirect(any(), any(), eq(555L), eq(NotificationType.ABSENCE_DECISION), eq(7L), any(), any(), isNull());
    }

    @Test
    void decidedRequest_cannotBeDecidedAgain() {
        request.setStatus(AbsenceStatus.APPROVED);
        assertThrows(IllegalStateException.class, () -> service.approve(7L, "admin"));
        assertThrows(IllegalStateException.class, () -> service.reject(7L, "x", "admin"));
    }
}
