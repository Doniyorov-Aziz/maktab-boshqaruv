package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.service.parent.ParentDataService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.DaySchedule;
import uz.azizbek.maktabboshqaruv.service.parent.ParentViews.LessonView;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
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
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static uz.azizbek.maktabboshqaruv.service.TelegramTestFixtures.*;

/** The morning digest: school days at 07:30 only — never on Sunday or a holiday. */
@ExtendWith(MockitoExtension.class)
class BotScheduledJobsTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);
    private static final LocalDate SUNDAY = LocalDate.of(2026, 10, 4);
    private static final long CHAT = 555L;

    @Mock
    private ParentTelegramLinkRepository linkRepository;
    @Mock
    private ParentSessionRepository sessionRepository;
    @Mock
    private NotificationLogRepository notificationLogRepository;
    @Mock
    private CalendarEventRepository calendarEventRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private BotSettingService botSettingService;
    @Mock
    private ParentDataService data;
    @Mock
    private WeeklyReportCardService weeklyCards;

    @InjectMocks
    private BotScheduledJobs jobs;

    private Student ali;
    private School school;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jobs, "properties", new TelegramProperties());
        school = school();
        ali = student(schoolClass(school, 5L, 5, "A"), 100L, "Ali", "Valiyev");
    }

    private void oneLinkedParent() {
        when(linkRepository.findAllActive()).thenReturn(List.of(link(ali, CHAT)));
        when(botSettingService.getOrDefault(school)).thenReturn(BotSetting.defaults(school));
    }

    private static DaySchedule lessons(LocalDate date, int count) {
        List<LessonView> list = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            LocalTime start = LocalTime.of(8, 30).plusMinutes(55L * i);
            list.add(new LessonView(i + 1, start, start.plusMinutes(45), (long) i, i == 0 ? "Geografiya" : "Fan " + i,
                    "O'qituvchi", "201", false, null));
        }
        return new DaySchedule(date, date.getDayOfWeek(), list, null, false, List.of());
    }

    @Test
    void sunday_noDigest() {
        assertEquals(0, jobs.runMorningDigests(SUNDAY.atTime(7, 35), false));
        assertEquals(0, jobs.runMorningDigests(SUNDAY.atTime(7, 35), true), "even when forced");
        verifyNoInteractions(notificationService, linkRepository);
    }

    @Test
    void holiday_noDigest() {
        oneLinkedParent();
        when(data.day(ali, MONDAY)).thenReturn(new DaySchedule(MONDAY, DayOfWeek.MONDAY, List.of(), "Mustaqillik kuni", false, List.of()));
        assertEquals(0, jobs.runMorningDigests(MONDAY.atTime(7, 35), false));
        verifyNoInteractions(notificationService);
    }

    @Test
    void beforeDigestTime_nothingYet() {
        oneLinkedParent();
        assertEquals(0, jobs.runMorningDigests(MONDAY.atTime(7, 0), false));
        verifyNoInteractions(notificationService);
    }

    @Test
    @SuppressWarnings("unchecked")
    void schoolDay_digestWithLessonsAndFirstLesson() {
        oneLinkedParent();
        when(data.day(ali, MONDAY)).thenReturn(lessons(MONDAY, 5));
        when(data.announcements(eq(ali), any())).thenReturn(List.of());
        when(notificationService.enqueueDirect(eq(school), eq(ali), eq(CHAT), eq(NotificationType.MORNING_DIGEST),
                eq(100L), eq(MONDAY), any(), any())).thenReturn(1);

        assertEquals(1, jobs.runMorningDigests(MONDAY.atTime(7, 35), false));

        ArgumentCaptor<Function<String, String>> text = ArgumentCaptor.forClass(Function.class);
        verify(notificationService).enqueueDirect(any(), any(), anyLong(), any(), anyLong(), any(), text.capture(), any());
        String uz = text.getValue().apply("uz");
        assertTrue(uz.contains("Xayrli tong"), uz);
        assertTrue(uz.contains("Ali</b>ning <b>5</b> ta darsi"), uz);
        assertTrue(uz.contains("08:30") && uz.contains("Geografiya"), uz);
        assertTrue(text.getValue().apply("ru").contains("Доброе утро"));
    }

    @Test
    void parentSwitchedDigestOff_skipped() {
        when(linkRepository.findAllActive()).thenReturn(List.of(link(ali, CHAT)));
        ParentSession session = new ParentSession();
        session.setChatId(CHAT);
        session.setNotifyMorningDigest(false);
        when(sessionRepository.findAll()).thenReturn(List.of(session));
        assertEquals(0, jobs.runMorningDigests(MONDAY.atTime(7, 35), false));
        verifyNoInteractions(notificationService);
    }
}
