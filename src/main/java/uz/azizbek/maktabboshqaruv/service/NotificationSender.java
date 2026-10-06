package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.NotificationLog;
import uz.azizbek.maktabboshqaruv.entity.ParentSession;
import uz.azizbek.maktabboshqaruv.repository.ParentSessionRepository;
import uz.azizbek.maktabboshqaruv.entity.NotificationStatus;
import uz.azizbek.maktabboshqaruv.entity.NotificationType;
import uz.azizbek.maktabboshqaruv.repository.NotificationLogRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drains the outbox. Telegram limits: ~30 msg/s per bot overall and 1 msg/s
 * per chat — we stay under with a configurable global rate (25/s default) and
 * at most one message per chat per second.
 *
 *  - 429: pause the whole sender for retry_after seconds; the message stays
 *    PENDING and the attempt is not counted (it was Telegram's limit, not a failure).
 *  - 403: the parent blocked the bot — FAILED, their links go inactive and
 *    the rest of their queue is SKIPPED.
 *  - anything else: retried with backoff, FAILED after maxAttempts (3).
 */
@Service
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);
    private static final long PER_CHAT_INTERVAL_MS = 1000;
    private static final long RETRY_BACKOFF_SECONDS = 30;
    private static final int MAX_TEXT = 4000;
    private static final String BUNDLE_SEPARATOR = "➖➖➖";

    @Autowired
    private NotificationLogRepository notificationLogRepository;

    @Autowired
    private ParentSessionRepository sessionRepository;

    @Autowired
    private WeeklyReportCardService weeklyCards;

    @Autowired
    private ParentTelegramLinkRepository linkRepository;

    @Autowired
    private TelegramClient telegramClient;

    @Autowired
    private TelegramProperties telegramProperties;

    @Autowired
    private Clock clock;

    private volatile long pausedUntilMillis = 0;
    private long lastSendMillis = 0;
    private final Map<Long, Long> lastSentPerChat = new ConcurrentHashMap<>();

    @Scheduled(fixedDelayString = "${telegram.send-interval-ms:1000}", initialDelayString = "${telegram.send-initial-delay-ms:5000}")
    public void tick() {
        if (!telegramProperties.isActive()) return;
        try {
            sendDue();
        } catch (Exception e) {
            log.warn("Xabarnomalarni yuborishda xato: {}", TokenMasker.mask(e.getMessage()));
        }
    }

    /** One pass over due messages. Returns how many were delivered. */
    public synchronized int sendDue() {
        if (clock.millis() < pausedUntilMillis) return 0;

        int batch = Math.max(1, telegramProperties.getMaxPerSecond());
        List<NotificationLog> due = notificationLogRepository.findDue(LocalDateTime.now(clock), PageRequest.of(0, batch));
        Set<Long> chatsThisPass = new HashSet<>();
        int delivered = 0;
        for (NotificationLog n : due) {
            if (clock.millis() < pausedUntilMillis) break;
            // Per-chat limit: a second message to the same chat waits for the next pass.
            if (!chatsThisPass.add(n.getChatId())) continue;
            Long last = lastSentPerChat.get(n.getChatId());
            if (last != null && clock.millis() - last < PER_CHAT_INTERVAL_MS) continue;

            throttleGlobal();
            if (Boolean.TRUE.equals(n.getQuietBundle())) {
                delivered += deliverQuietBundle(n.getChatId());
            } else if (deliver(n)) {
                delivered++;
            }
        }
        return delivered;
    }

    /**
     * Quiet hours are over: everything held back for this chat goes out as one
     * message ("🌙 Tunda kelgan xabarlar") instead of a burst of separate ones.
     * A single held message is simply sent as it is.
     */
    int deliverQuietBundle(Long chatId) {
        List<NotificationLog> held = notificationLogRepository.findDueQuietBundle(chatId, LocalDateTime.now(clock));
        List<NotificationLog> live = new ArrayList<>();
        for (NotificationLog n : held) {
            if (stillSubscribed(n)) {
                live.add(n);
            } else {
                skipUnsubscribed(n);
            }
        }
        if (live.isEmpty()) return 0;
        if (live.size() == 1) return deliver(live.get(0)) ? 1 : 0;

        String lang = sessionRepository.findByChatId(chatId).map(ParentSession::lang).orElse(BotI18n.DEFAULT_LANG);
        StringBuilder text = new StringBuilder(BotI18n.get().t(lang, "notif.quiet_bundle_title", "count", live.size()));
        for (NotificationLog n : live) {
            String part = "\n\n" + BUNDLE_SEPARATOR + "\n\n" + n.getText();
            if (text.length() + part.length() > MAX_TEXT) {
                text.append("\n\n…");
                break;
            }
            text.append(part);
        }
        try {
            telegramClient.sendMessage(chatId, text.toString(), null);
            lastSendMillis = clock.millis();
            lastSentPerChat.put(chatId, lastSendMillis);
            LocalDateTime sentAt = LocalDateTime.now(clock);
            for (NotificationLog n : live) {
                n.setAttempts(n.getAttempts() + 1);
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(sentAt);
                n.setLastError(null);
                notificationLogRepository.save(n);
            }
            return live.size();
        } catch (TelegramApiException e) {
            lastSendMillis = clock.millis();
            for (NotificationLog n : live) handleFailure(n, e);
            return 0;
        } catch (RuntimeException e) {
            lastSendMillis = clock.millis();
            for (NotificationLog n : live) handleFailure(n, new TelegramApiException(0, e.getMessage(), null));
            return 0;
        }
    }

    private boolean stillSubscribed(NotificationLog n) {
        // An announcement row names just one of the parent's children, so any active link counts.
        return n.getType() == NotificationType.ANNOUNCEMENT || n.getType() == NotificationType.BROADCAST
                ? !linkRepository.findByChatIdAndActiveTrue(n.getChatId()).isEmpty()
                : linkRepository.findByStudentIdAndChatId(n.getStudent().getId(), n.getChatId())
                .map(l -> Boolean.TRUE.equals(l.getActive()))
                .orElse(false);
    }

    private void skipUnsubscribed(NotificationLog n) {
        n.setStatus(NotificationStatus.SKIPPED);
        n.setLastError("Ota-ona obunani bekor qilgan");
        notificationLogRepository.save(n);
    }

    boolean deliver(NotificationLog n) {
        // The parent may have sent /stop (or been unlinked) after this was queued.
        if (!stillSubscribed(n)) {
            skipUnsubscribed(n);
            return false;
        }

        try {
            byte[] picture = n.getImage() == null ? null : weeklyCards.render(n.getImage());
            if (picture != null) {
                // the picture is drawn now, so it shows the latest data; the text is its caption
                telegramClient.sendPhoto(n.getChatId(), picture, "haftalik-hisobot.png", n.getText(), n.getReplyMarkup());
            } else {
                telegramClient.sendMessage(n.getChatId(), n.getText(), n.getReplyMarkup());
            }
            lastSendMillis = clock.millis();
            lastSentPerChat.put(n.getChatId(), lastSendMillis);
            n.setAttempts(n.getAttempts() + 1);
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(LocalDateTime.now(clock));
            n.setLastError(null);
            notificationLogRepository.save(n);
            return true;
        } catch (TelegramApiException e) {
            lastSendMillis = clock.millis();
            handleFailure(n, e);
            return false;
        } catch (RuntimeException e) {
            lastSendMillis = clock.millis();
            handleFailure(n, new TelegramApiException(0, e.getMessage(), null));
            return false;
        }
    }

    void handleFailure(NotificationLog n, TelegramApiException e) {
        String reason = TokenMasker.mask(e.getMessage(), telegramProperties.getBotToken());

        if (e.isFloodLimit()) {
            int wait = e.getRetryAfter() != null && e.getRetryAfter() > 0 ? e.getRetryAfter() : 5;
            pausedUntilMillis = clock.millis() + wait * 1000L;
            n.setLastError("429: Telegram limiti — " + wait + " soniya kutilmoqda");
            n.setScheduledAt(LocalDateTime.now(clock).plusSeconds(wait));
            notificationLogRepository.save(n);
            log.warn("Telegram 429 (flood limit): yuborish {} soniyaga to'xtatildi", wait);
            return;
        }

        n.setAttempts(n.getAttempts() + 1);

        if (e.isBlocked()) {
            n.setStatus(NotificationStatus.FAILED);
            n.setLastError("403: Ota-ona botni bloklagan — bog'lanish o'chirildi");
            notificationLogRepository.save(n);
            int links = linkRepository.deactivateByChatId(n.getChatId());
            notificationLogRepository.skipPendingForChat(n.getChatId(), "Ota-ona botni bloklagan");
            log.info("Telegram 403: chat={} botni bloklagan, {} ta bog'lanish o'chirildi", n.getChatId(), links);
            return;
        }

        n.setLastError((e.getErrorCode() > 0 ? e.getErrorCode() + ": " : "") + reason);
        if (n.getAttempts() >= telegramProperties.getMaxAttempts()) {
            n.setStatus(NotificationStatus.FAILED);
            log.warn("Xabarnoma #{} {} urinishdan keyin FAILED: {}", n.getId(), n.getAttempts(), reason);
        } else {
            n.setScheduledAt(LocalDateTime.now(clock).plusSeconds(RETRY_BACKOFF_SECONDS * n.getAttempts()));
            log.info("Xabarnoma #{} yuborilmadi ({}-urinish), qayta uriniladi: {}", n.getId(), n.getAttempts(), reason);
        }
        notificationLogRepository.save(n);
    }

    private void throttleGlobal() {
        long minGap = 1000L / Math.max(1, telegramProperties.getMaxPerSecond());
        long wait = lastSendMillis + minGap - clock.millis();
        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public long getPausedUntilMillis() {
        return pausedUntilMillis;
    }
}
