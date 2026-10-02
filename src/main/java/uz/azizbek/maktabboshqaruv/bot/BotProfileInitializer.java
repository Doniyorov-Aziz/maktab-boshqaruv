package uz.azizbek.maktabboshqaruv.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import uz.azizbek.maktabboshqaruv.telegram.TokenMasker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sets the bot's public profile from code at startup (real bot only): the
 * command menu (/start, /menu, /jadval, /davomat, /baholar, /yordam, /stop),
 * the long and short descriptions — in Uzbek by default and in Russian for
 * Russian-language Telegram apps — and the Menu button (the Mini App when
 * TELEGRAM_WEBAPP_URL is set, the command list otherwise). Failures only log.
 */
@Component
public class BotProfileInitializer {

    private static final Logger log = LoggerFactory.getLogger(BotProfileInitializer.class);
    private static final List<String> COMMANDS = List.of("start", "menu", "jadval", "davomat", "baholar", "yordam", "stop");

    @Autowired
    private TelegramProperties properties;

    @Autowired
    private TelegramClient client;

    @EventListener(ApplicationReadyEvent.class)
    public void apply() {
        // The bottom menu shows "📱 Kundalikni ochish" only when a valid https Mini App address is set.
        Keyboards.setWebAppUrl(properties.webappUrlIfValid());
        if (properties.mode() != TelegramProperties.Mode.LIVE) return;
        try {
            BotI18n i18n = BotI18n.get();
            // language_code null = default for everyone; "ru" = Telegram apps set to Russian.
            for (String[] target : new String[][]{{"uz", null}, {"ru", "ru"}}) {
                String lang = target[0];
                client.setMyCommands(COMMANDS.stream()
                        .map(c -> new TelegramModels.BotCommand(c, i18n.t(lang, "cmd." + c))).toList(), target[1]);
                client.setMyDescription(i18n.t(lang, "bot.description"), target[1]);
                client.setMyShortDescription(i18n.t(lang, "bot.short_description"), target[1]);
            }
            String webapp = properties.webappUrlIfValid();
            client.setChatMenuButton(webapp != null
                    ? new TelegramModels.MenuButtonWebApp(i18n.t("uz", "bot.menu_button"), webapp)
                    : new TelegramModels.MenuButtonCommands());
            log.info("Telegram bot profili yangilandi (buyruqlar, tavsif, menyu tugmasi{})", webapp != null ? " — Mini App" : "");
        } catch (Exception e) {
            log.warn("Telegram bot profilini yangilab bo'lmadi: {}", TokenMasker.mask(e.getMessage()));
        }
    }
}
