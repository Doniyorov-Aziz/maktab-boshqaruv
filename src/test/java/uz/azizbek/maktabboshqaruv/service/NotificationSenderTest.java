package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class NotificationSenderTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 30, 10, 0);

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private ParentTelegramLinkRepository linkRepository;

    @Mock
    private TelegramClient telegramClient;

    @InjectMocks
    private NotificationSender sender;

    private Student student;

    @BeforeEach
    void setUp() {
        TelegramProperties properties = new TelegramProperties();
        properties.setMock(true);
        ReflectionTestUtils.setField(sender, "telegramProperties", properties);
        ReflectionTestUtils.setField(sender, "clock", Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
        student = student(schoolClass(school(), 5L, 5, "A"), 100L, "Ali", "Valiyev");
    }

    private NotificationLog pending(long id, long chatId) {
        NotificationLog n = new NotificationLog();
        n.setId(id);
        n.setStudent(student);
        n.setChatId(chatId);
        n.setType(NotificationType.ATTENDANCE_ABSENT);
        n.setText("🔴 test");
        n.setStatus(NotificationStatus.PENDING);
        n.setAttempts(0);
        n.setScheduledAt(NOW);
        return n;
    }

    private void subscribed(long chatId) {
        when(linkRepository.findByStudentIdAndChatId(100L, chatId)).thenReturn(Optional.of(link(student, chatId)));
    }

    @Test
    void success_marksSent() {
        NotificationLog n = pending(1L, 11L);
        when(notificationLogRepository.findDue(any(), any())).thenReturn(List.of(n));
        subscribed(11L);

        assertEquals(1, sender.sendDue());

        verify(telegramClient).sendMessage(11L, "🔴 test", null);
        assertEquals(NotificationStatus.SENT, n.getStatus());
        assertEquals(1, n.getAttempts());
        assertEquals(NOW, n.getSentAt());
    }

    @Test
    void forbidden403_failsAndDeactivatesLinksOfThatChat() {
        NotificationLog n = pending(1L, 11L);
        subscribed(11L);
        doThrow(new TelegramApiException(403, "Forbidden: bot was blocked by the user", null))
                .when(telegramClient).sendMessage(eq(11L), anyString(), isNull());

        assertFalse(sender.deliver(n));

        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertTrue(n.getLastError().startsWith("403"));
        verify(linkRepository).deactivateByChatId(11L);
        verify(notificationLogRepository).skipPendingForChat(eq(11L), anyString());
    }

    @Test
    void tooManyRequests429_waitsRetryAfterAndKeepsMessagePending() {
        NotificationLog n = pending(1L, 11L);
        when(notificationLogRepository.findDue(any(), any())).thenReturn(List.of(n));
        subscribed(11L);
        doThrow(new TelegramApiException(429, "Too Many Requests: retry after 7", 7))
                .when(telegramClient).sendMessage(anyLong(), anyString(), any());

        assertEquals(0, sender.sendDue());

        assertEquals(NotificationStatus.PENDING, n.getStatus());
        assertEquals(0, n.getAttempts(), "a flood-limit pause is not a failed attempt");
        assertEquals(NOW.plusSeconds(7), n.getScheduledAt());
        assertEquals(NOW.atZone(ZONE).toInstant().toEpochMilli() + 7000, sender.getPausedUntilMillis());

        // Clock hasn't moved: the next pass must not even query the outbox.
        assertEquals(0, sender.sendDue());
        verify(notificationLogRepository, times(1)).findDue(any(), any());
        verify(linkRepository, never()).deactivateByChatId(any());
    }

    @Test
    void otherErrors_retriedThenFailedAfterThreeAttempts() {
        NotificationLog n = pending(1L, 11L);
        subscribed(11L);
        doThrow(new TelegramApiException(400, "Bad Request: chat not found", null))
                .when(telegramClient).sendMessage(anyLong(), anyString(), any());

        sender.deliver(n);
        assertEquals(NotificationStatus.PENDING, n.getStatus());
        assertEquals(1, n.getAttempts());
        assertEquals(NOW.plusSeconds(30), n.getScheduledAt());

        sender.deliver(n);
        assertEquals(NotificationStatus.PENDING, n.getStatus());
        assertEquals(2, n.getAttempts());

        sender.deliver(n);
        assertEquals(NotificationStatus.FAILED, n.getStatus());
        assertEquals(3, n.getAttempts());
        assertTrue(n.getLastError().contains("chat not found"));
        verify(linkRepository, never()).deactivateByChatId(any());
    }

    @Test
    void parentUnsubscribedAfterQueueing_isSkipped() {
        NotificationLog n = pending(1L, 11L);
        when(linkRepository.findByStudentIdAndChatId(100L, 11L)).thenReturn(Optional.empty());

        assertFalse(sender.deliver(n));

        assertEquals(NotificationStatus.SKIPPED, n.getStatus());
        verifyNoInteractions(telegramClient);
    }

    @Test
    void oneMessagePerChatPerPass() {
        NotificationLog a = pending(1L, 11L);
        NotificationLog b = pending(2L, 11L);
        when(notificationLogRepository.findDue(any(), any())).thenReturn(List.of(a, b));
        subscribed(11L);

        assertEquals(1, sender.sendDue());

        assertEquals(NotificationStatus.SENT, a.getStatus());
        assertEquals(NotificationStatus.PENDING, b.getStatus());
        verify(telegramClient, times(1)).sendMessage(anyLong(), anyString(), any());
    }
}
