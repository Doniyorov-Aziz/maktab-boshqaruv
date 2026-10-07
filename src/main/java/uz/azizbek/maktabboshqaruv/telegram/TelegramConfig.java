package uz.azizbek.maktabboshqaruv.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramConfig {

    private static final Logger log = LoggerFactory.getLogger(TelegramConfig.class);

    public static final ZoneId ZONE = ZoneId.of("Asia/Tashkent");

    /**
     * The application's clock (Asia/Tashkent). For demos and browser tests only: {@code app.fake-now}
     * (e.g. 2026-10-07T10:40) starts the clock at that moment and lets it run on. Empty = real time.
     */
    @Bean
    public Clock clock(@org.springframework.beans.factory.annotation.Value("${app.fake-now:}") String fakeNow) {
        Clock real = Clock.system(ZONE);
        if (fakeNow == null || fakeNow.isBlank()) return real;
        java.time.Instant start = java.time.LocalDateTime.parse(fakeNow.trim()).atZone(ZONE).toInstant();
        log.warn("DIQQAT: soxta vaqt yoqilgan (app.fake-now={}) — faqat sinov uchun", fakeNow.trim());
        return Clock.offset(real, java.time.Duration.between(real.instant(), start));
    }

    /**
     * Mock mode wins over everything; otherwise the real client is only built
     * when there is a token. Without one a no-op client stands in and the
     * outbox marks messages SKIPPED, so the app starts cleanly either way.
     */
    @Bean
    public TelegramClient telegramClient(TelegramProperties properties) {
        TelegramProperties.Mode mode = properties.mode();
        switch (mode) {
            case MOCK -> log.info("Telegram bot: MOCK rejimi — xabarlar faqat logga va bazaga yoziladi");
            case LIVE -> log.info("Telegram bot: yoqilgan (@{}, token: ***)", properties.getBotUsername());
            case NO_TOKEN -> log.warn("Telegram bot: TELEGRAM_ENABLED=true, lekin TELEGRAM_BOT_TOKEN berilmagan — xabarlar SKIPPED bo'ladi");
            case DISABLED -> log.info("Telegram bot: o'chirilgan (TELEGRAM_ENABLED=false) — xabarlar SKIPPED bo'ladi");
        }
        if (mode == TelegramProperties.Mode.MOCK) {
            return new MockTelegramClient();
        }
        if (mode == TelegramProperties.Mode.LIVE) {
            return new HttpTelegramClient(properties);
        }
        return new TelegramClient() {
            @Override
            public Long sendMessage(long chatId, String html, Object replyMarkup) {
                throw new TelegramApiException(0, "Telegram bot sozlanmagan", null);
            }

            @Override
            public List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds) {
                return List.of();
            }
        };
    }
}
