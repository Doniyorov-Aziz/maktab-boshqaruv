package uz.azizbek.maktabboshqaruv;

import uz.azizbek.maktabboshqaruv.dto.AttendanceBulkRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.AttendanceService;
import uz.azizbek.maktabboshqaruv.service.NotificationSender;
import uz.azizbek.maktabboshqaruv.service.TelegramBotService;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end in telegram.mock=true mode against the seeded database:
 * parent links via deep-link code -> attendance saved -> AFTER_COMMIT listener
 * writes a PENDING outbox row -> sender delivers through the mock client -> SENT.
 * The scheduled sender is pushed an hour out so the test drives it by hand.
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.bot-username=maktab_test_bot",
        "telegram.send-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class TelegramMockFlowIntegrationTest {

    // A date no seeder ever writes, so the test never collides with seeded attendance.
    private static final LocalDate DATE = LocalDate.of(2000, 1, 3);

    @Autowired
    private LessonSlotRepository lessonSlotRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private ParentTelegramLinkRepository linkRepository;
    @Autowired
    private NotificationLogRepository notificationLogRepository;
    @Autowired
    private NotificationSettingsRepository settingsRepository;
    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private TelegramBotService botService;
    @Autowired
    private NotificationSender sender;

    private long chatId;
    private LessonSlot slot;
    private Student student;
    private NotificationSettings createdSettings;

    @BeforeEach
    void setUp() {
        chatId = 9_000_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        slot = lessonSlotRepository.findAll().stream()
                .filter(l -> !studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(l.getSchoolClass().getId()).isEmpty())
                .findFirst().orElseThrow(() -> new IllegalStateException("Seed ma'lumot topilmadi"));
        student = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(slot.getSchoolClass().getId()).get(0);

        // Make the test independent of the time of day it runs at.
        School school = slot.getSchoolClass().getAcademicYear().getSchool();
        if (settingsRepository.findBySchoolId(school.getId()).isEmpty()) {
            NotificationSettings s = NotificationSettings.defaults(school, LocalTime.of(22, 0), LocalTime.of(7, 0));
            s.setQuietHoursEnabled(false);
            createdSettings = settingsRepository.save(s);
        }
    }

    @AfterEach
    void cleanUp() {
        notificationLogRepository.deleteAll(rows());
        attendanceRepository.findByLessonSlotIdAndRecordDate(slot.getId(), DATE).forEach(attendanceRepository::delete);
        linkRepository.findByStudentIdAndChatId(student.getId(), chatId).ifPresent(linkRepository::delete);
        if (createdSettings != null) settingsRepository.delete(createdSettings);
    }

    private List<NotificationLog> rows() {
        return notificationLogRepository.findAll().stream().filter(n -> n.getChatId() == chatId).toList();
    }

    private void parentOpensDeepLink() {
        botService.handleUpdate(new TelegramModels.Update(1, new TelegramModels.Message(1L,
                new TelegramModels.User(chatId, false, "Test ota", "test_ota"),
                new TelegramModels.Chat(chatId, "private"), "/start " + student.getTelegramLinkCode(), null)));
        assertTrue(linkRepository.findByStudentIdAndChatId(student.getId(), chatId).orElseThrow().getActive());
    }

    private AttendanceBulkRequestDto mark(AttendanceStatus status, Long... extraStudentIds) {
        AttendanceBulkRequestDto request = new AttendanceBulkRequestDto();
        request.setLessonSlotId(slot.getId());
        request.setRecordDate(DATE);
        List<AttendanceBulkRequestDto.Entry> entries = new ArrayList<>();
        AttendanceBulkRequestDto.Entry e = new AttendanceBulkRequestDto.Entry();
        e.setStudentId(student.getId());
        e.setStatus(status);
        entries.add(e);
        for (Long id : extraStudentIds) {
            AttendanceBulkRequestDto.Entry x = new AttendanceBulkRequestDto.Entry();
            x.setStudentId(id);
            x.setStatus(AttendanceStatus.ABSENT);
            entries.add(x);
        }
        request.setEntries(entries);
        return request;
    }

    @Test
    void absentAttendance_goesPendingThenSent_andResaveDoesNotDuplicate() {
        assertNotNull(student.getTelegramLinkCode(), "backfill/PrePersist must give every student a code");
        parentOpensDeepLink();

        attendanceService.bulkMark(mark(AttendanceStatus.ABSENT));

        List<NotificationLog> queued = rows();
        assertEquals(1, queued.size());
        assertEquals(NotificationType.ATTENDANCE_ABSENT, queued.get(0).getType());
        assertEquals(NotificationStatus.PENDING, queued.get(0).getStatus());
        assertTrue(queued.get(0).getText().contains("kelmadi"));

        // Same roster saved again with the same status -> no new message.
        attendanceService.bulkMark(mark(AttendanceStatus.ABSENT));
        assertEquals(1, rows().size());

        sender.sendDue();

        NotificationLog sent = rows().get(0);
        assertEquals(NotificationStatus.SENT, sent.getStatus());
        assertNotNull(sent.getSentAt());
    }

    @Test
    void rolledBackSave_producesNoNotification() {
        parentOpensDeepLink();

        // Second entry references a non-existent student -> exception -> whole transaction rolls back.
        assertThrows(IllegalStateException.class,
                () -> attendanceService.bulkMark(mark(AttendanceStatus.ABSENT, Long.MAX_VALUE)));

        assertTrue(attendanceRepository.findByLessonSlotIdAndRecordDate(slot.getId(), DATE).isEmpty());
        assertTrue(rows().isEmpty(), "AFTER_COMMIT listener must not fire on rollback");
    }
}
