package uz.azizbek.maktabboshqaruv.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.telegram.TelegramApiException;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * Long polling: the server calls Telegram (getUpdates with an offset) instead
 * of Telegram calling a webhook, so no public URL or open port is needed.
 * Each call blocks up to pollTimeoutSeconds, which is why the scheduler pool
 * has more than one thread (spring.task.scheduling.pool.size).
 */
@Service
public class TelegramUpdatePoller {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdatePoller.class);

    @Autowired
    private TelegramClient telegramClient;

    @Autowired
    private BotRouter router;

    @Autowired
    private TelegramProperties telegramProperties;

    @Autowired
    private Clock clock;

    private long offset = 0;
    private long backoffUntilMillis = 0;
    private volatile String lastError;

    @Scheduled(fixedDelay = 500, initialDelay = 3000)
    public void poll() {
        if (telegramProperties.mode() != TelegramProperties.Mode.LIVE) return;
        if (clock.millis() < backoffUntilMillis) return;

        List<TelegramModels.Update> updates;
        try {
            updates = telegramClient.getUpdates(offset, telegramProperties.getPollTimeoutSeconds());
            lastError = null;
        } catch (TelegramApiException e) {
            onPollError(e);
            return;
        }

        for (TelegramModels.Update update : updates) {
            // Advance first: a message that crashes the handler must not be re-delivered forever.
            offset = Math.max(offset, update.updateId() + 1);
            try {
                router.handle(update);
            } catch (Exception e) {
                log.warn("Telegram update #{} ni qayta ishlashda xato: {}", update.updateId(), TokenMasker.mask(e.getMessage()));
            }
        }
    }

    private void onPollError(TelegramApiException e) {
        String msg = TokenMasker.mask(e.getMessage(), telegramProperties.getBotToken());
        long pauseSeconds;
        switch (e.getErrorCode()) {
            case 401, 404 -> {
                lastError = "Token noto'g'ri (Telegram " + e.getErrorCode() + ")";
                pauseSeconds = 300;
            }
            case 409 -> {
                lastError = "Boshqa nusxa yoki webhook ishlayapti (409)";
                pauseSeconds = 30;
            }
            case 429 -> {
                lastError = "Telegram limiti (429)";
                pauseSeconds = e.getRetryAfter() != null ? e.getRetryAfter() : 5;
            }
            default -> {
                lastError = "Telegram'ga ulanib bo'lmadi: " + msg;
                pauseSeconds = 10;
            }
        }
        backoffUntilMillis = clock.millis() + pauseSeconds * 1000;
        log.warn("Telegram getUpdates xatosi: {} — {} soniyadan so'ng qayta uriniladi", msg, pauseSeconds);
    }

    public String getLastError() {
        return lastError;
    }
}
