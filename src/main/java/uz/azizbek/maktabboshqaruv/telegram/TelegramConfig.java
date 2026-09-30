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

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
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
            public void sendMessage(long chatId, String html, Object replyMarkup) {
                throw new TelegramApiException(0, "Telegram bot sozlanmagan", null);
            }

            @Override
            public List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds) {
                return List.of();
            }
        };
    }
}
