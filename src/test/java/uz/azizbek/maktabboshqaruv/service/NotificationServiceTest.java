package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.event.AnnouncementCreatedEvent;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;
import uz.azizbek.maktabboshqaruv.event.GradeSavedEvent;
import uz.azizbek.maktabboshqaruv.repository.*;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static uz.azizbek.maktabboshqaruv.service.TelegramTestFixtures.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private ParentTelegramLinkRepository linkRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private GradeRepository gradeRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private LessonSlotRepository lessonSlotRepository;

    @Mock
    private NotificationSettingsService settingsService;

    @Mock
    private ParentSessionRepository sessionRepository;

    @Mock
    private BotSettingService botSettingService;

    @InjectMocks
    private NotificationService notificationService;

    private TelegramProperties properties;
    private School school;
    private SchoolClass class5a;
    private Student alisher;

    @BeforeEach
    void setUp() {
        properties = new TelegramProperties();
        properties.setMock(true);
        ReflectionTestUtils.setField(notificationService, "telegramProperties", properties);
        useClock(LocalDateTime.of(2026, 9, 30, 10, 0));

        school = school();
        class5a = schoolClass(school, 5L, 5, "A");
        alisher = student(class5a, 100L, "Alisher", "Karimov");
    }

    private void useClock(LocalDateTime at) {
        ZoneId zone = ZoneId.of("Asia/Tashkent");
        ReflectionTestUtils.setField(notificationService, "clock", Clock.fixed(at.atZone(zone).toInstant(), zone));
    }

    private void defaultSettings() {
        when(settingsService.getOrDefault(school))
                .thenReturn(NotificationSettings.defaults(school, LocalTime.of(22, 0), LocalTime.of(7, 0)));
    }

    private Attendance absentAttendance() {
        LessonSlot first = slot(class5a, 1L, "Ona tili", LocalTime.of(8, 30));
        LessonSlot second = slot(class5a, 2L, "Matematika", LocalTime.of(9, 25));
        when(lessonSlotRepository.findBySchoolClassId(5L)).thenReturn(List.of(second, first));

        Attendance a = new Attendance();
        a.setId(500L);
        a.setStudent(alisher);
        a.setLessonSlot(second);
        a.setRecordDate(LocalDate.of(2026, 9, 30));
        a.setStatus(AttendanceStatus.ABSENT);
        when(attendanceRepository.findById(500L)).thenReturn(Optional.of(a));
        return a;
    }

    private List<NotificationLog> savedRows(int expected) {
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(notificationLogRepository, times(expected)).save(captor.capture());
        return captor.getAllValues();
    }

    // --- status did not change -> nothing ---

    @Test
    void attendance_unchangedStatus_createsNothing() {
        int created = notificationService.enqueueAttendance(
                new AttendanceMarkedEvent(500L, AttendanceStatus.ABSENT, AttendanceStatus.ABSENT));

        assertEquals(0, created);
        verifyNoInteractions(attendanceRepository, notificationLogRepository, linkRepository);
    }

    @Test
    void attendance_presentStatus_neverNotifies() {
        assertFalse(AttendanceMarkedEvent.isNotifiable(AttendanceStatus.ABSENT, AttendanceStatus.PRESENT));
        assertFalse(AttendanceMarkedEvent.isNotifiable(null, AttendanceStatus.EXCUSED));
        assertTrue(AttendanceMarkedEvent.isNotifiable(AttendanceStatus.PRESENT, AttendanceStatus.ABSENT));
        assertTrue(AttendanceMarkedEvent.isNotifiable(AttendanceStatus.ABSENT, AttendanceStatus.LATE));
        assertTrue(AttendanceMarkedEvent.isNotifiable(null, AttendanceStatus.LATE));
    }

    // --- happy path ---

    @Test
    void attendance_presentToAbsent_onePendingRowPerParent() {
        absentAttendance();
        defaultSettings();
        when(linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(100L))
                .thenReturn(List.of(link(alisher, 11L), link(alisher, 22L)));

        int created = notificationService.enqueueAttendance(
                new AttendanceMarkedEvent(500L, AttendanceStatus.PRESENT, AttendanceStatus.ABSENT));

        assertEquals(2, created);
        List<NotificationLog> rows = savedRows(2);
        assertEquals(List.of(11L, 22L), rows.stream().map(NotificationLog::getChatId).toList());
        NotificationLog row = rows.get(0);
        assertEquals(NotificationType.ATTENDANCE_ABSENT, row.getType());
        assertEquals(NotificationStatus.PENDING, row.getStatus());
        assertEquals(500L, row.getReferenceId());
        assertEquals(LocalDate.of(2026, 9, 30), row.getRecordDate());
        assertEquals(LocalDateTime.of(2026, 9, 30, 10, 0), row.getScheduledAt());
        assertTrue(row.getText().startsWith("🔴 <b>Alisher Karimov</b> (5-A) bugun 2-darsga (Matematika, 09:25) kelmadi."),
                row.getText());
        assertTrue(row.getText().endsWith("1-maktab</i>"));
    }

    // --- dedup ---

    @Test
    void attendance_sameKeyAlreadyQueued_isNotDuplicated() {
        Attendance a = new Attendance();
        a.setId(500L);
        a.setStudent(alisher);
        a.setRecordDate(LocalDate.of(2026, 9, 30));
        a.setStatus(AttendanceStatus.ABSENT);
        when(attendanceRepository.findById(500L)).thenReturn(Optional.of(a));
        when(notificationLogRepository.existsByStudentIdAndTypeAndReferenceIdAndRecordDate(
                100L, NotificationType.ATTENDANCE_ABSENT, 500L, LocalDate.of(2026, 9, 30))).thenReturn(true);

        int created = notificationService.enqueueAttendance(
                new AttendanceMarkedEvent(500L, AttendanceStatus.PRESENT, AttendanceStatus.ABSENT));

        assertEquals(0, created);
        verify(notificationLogRepository, never()).save(any());
        verifyNoInteractions(linkRepository);
    }

    @Test
    void attendance_statusChangedAgainBeforeListenerRan_isIgnored() {
        Attendance a = new Attendance();
        a.setId(500L);
        a.setStatus(AttendanceStatus.PRESENT);
        when(attendanceRepository.findById(500L)).thenReturn(Optional.of(a));

        assertEquals(0, notificationService.enqueueAttendance(
                new AttendanceMarkedEvent(500L, AttendanceStatus.PRESENT, AttendanceStatus.ABSENT)));
        verify(notificationLogRepository, never()).save(any());
    }

    // --- skipped states ---

    @Test
    void attendance_botDisabled_rowsAreSkippedNotPending() {
        properties.setMock(false); // enabled=false by default
        absentAttendance();
        defaultSettings();
        when(linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(100L)).thenReturn(List.of(link(alisher, 11L)));

        notificationService.enqueueAttendance(new AttendanceMarkedEvent(500L, null, AttendanceStatus.ABSENT));

        NotificationLog row = savedRows(1).get(0);
        assertEquals(NotificationStatus.SKIPPED, row.getStatus());
        assertTrue(row.getLastError().contains("TELEGRAM_ENABLED=false"));
    }

    @Test
    void attendance_typeDisabledInSchoolSettings_rowsAreSkipped() {
        absentAttendance();
        NotificationSettings settings = NotificationSettings.defaults(school, LocalTime.of(22, 0), LocalTime.of(7, 0));
        settings.setAttendanceEnabled(false);
        when(settingsService.getOrDefault(school)).thenReturn(settings);
        when(linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(100L)).thenReturn(List.of(link(alisher, 11L)));

        notificationService.enqueueAttendance(new AttendanceMarkedEvent(500L, null, AttendanceStatus.ABSENT));

        assertEquals(NotificationStatus.SKIPPED, savedRows(1).get(0).getStatus());
    }

    @Test
    void attendance_duringQuietHours_scheduledFor7am() {
        useClock(LocalDateTime.of(2026, 9, 30, 22, 30));
        absentAttendance();
        defaultSettings();
        when(linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(100L)).thenReturn(List.of(link(alisher, 11L)));

        notificationService.enqueueAttendance(new AttendanceMarkedEvent(500L, null, AttendanceStatus.ABSENT));

        NotificationLog row = savedRows(1).get(0);
        assertEquals(NotificationStatus.PENDING, row.getStatus());
        assertEquals(LocalDateTime.of(2026, 10, 1, 7, 0), row.getScheduledAt());
    }

    @Test
    void attendance_noLinkedParents_noRows() {
        absentAttendance();
        when(linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(100L)).thenReturn(List.of());

        assertEquals(0, notificationService.enqueueAttendance(new AttendanceMarkedEvent(500L, null, AttendanceStatus.ABSENT)));
        verify(notificationLogRepository, never()).save(any());
    }

    // --- grades ---

    @Test
    void grade_new_createsGradeNewRow() {
        Subject physics = new Subject();
        physics.setName("Fizika");
        Grade g = new Grade();
        g.setId(77L);
        g.setStudent(alisher);
        g.setSubject(physics);
        g.setScore(5);
        g.setType(GradeType.CURRENT);
        g.setGradeDate(LocalDate.of(2026, 9, 30));
        when(gradeRepository.findById(77L)).thenReturn(Optional.of(g));
        when(botSettingService.getOrDefault(school)).thenReturn(BotSetting.defaults(school));
        defaultSettings();
        when(linkRepository.findByStudentIdAndActiveTrueOrderByLinkedAtAsc(100L)).thenReturn(List.of(link(alisher, 11L)));

        notificationService.enqueueGrade(new GradeSavedEvent(77L, true));

        NotificationLog row = savedRows(1).get(0);
        assertEquals(NotificationType.GRADE_NEW, row.getType());
        assertTrue(row.getText().startsWith("📘 <b>Alisher Karimov</b> Fizika fanidan <b>5</b> baho oldi (joriy baho, 30.09.2026)."));
    }

    // --- announcements: audience ---

    @Test
    void announcement_classAudience_onlyThatClassParents_oncePerChat() {
        SchoolClass class5b = schoolClass(school, 6L, 5, "B");
        Student sibling = student(class5a, 101L, "Dilnoza", "Karimova");
        Announcement an = new Announcement();
        an.setId(9L);
        an.setSchool(school);
        an.setTitle("Ota-onalar yig'ilishi");
        an.setContent("Juma 18:00");
        an.setAudience(AnnouncementAudience.CLASS);
        an.setSchoolClass(class5a);
        when(announcementRepository.findById(9L)).thenReturn(Optional.of(an));
        // parent 11 has two children in 5-A — must get it once
        when(linkRepository.findActiveByClassId(5L))
                .thenReturn(List.of(link(alisher, 11L), link(sibling, 11L), link(sibling, 33L)));
        defaultSettings();

        int created = notificationService.enqueueAnnouncement(new AnnouncementCreatedEvent(9L));

        assertEquals(2, created);
        verify(linkRepository, never()).findActiveBySchoolId(any());
        List<NotificationLog> rows = savedRows(2);
        assertEquals(List.of(11L, 33L), rows.stream().map(NotificationLog::getChatId).toList());
        assertTrue(rows.get(0).getText().startsWith("📢 <b>5-A sinf e'loni</b>: Ota-onalar yig'ilishi\nJuma 18:00"));
        assertNotNull(class5b); // other class exists but is never queried
    }

    @Test
    void announcement_allAudience_usesWholeSchool() {
        Announcement an = new Announcement();
        an.setId(9L);
        an.setSchool(school);
        an.setTitle("Bayram");
        an.setContent("Dam olish kuni");
        an.setAudience(AnnouncementAudience.ALL);
        when(announcementRepository.findById(9L)).thenReturn(Optional.of(an));
        when(linkRepository.findActiveBySchoolId(1L)).thenReturn(List.of(link(alisher, 11L)));
        defaultSettings();

        assertEquals(1, notificationService.enqueueAnnouncement(new AnnouncementCreatedEvent(9L)));
        verify(linkRepository, never()).findActiveByClassId(any());
        assertTrue(savedRows(1).get(0).getText().startsWith("📢 <b>Maktab e'loni</b>: Bayram"));
    }

    @Test
    void announcement_teachersAudience_neverReachesParents() {
        Announcement an = new Announcement();
        an.setId(9L);
        an.setSchool(school);
        an.setAudience(AnnouncementAudience.TEACHERS);
        when(announcementRepository.findById(9L)).thenReturn(Optional.of(an));

        assertEquals(0, notificationService.enqueueAnnouncement(new AnnouncementCreatedEvent(9L)));
        verifyNoInteractions(linkRepository);
        verify(notificationLogRepository, never()).save(any());
    }
}
