package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.InlineButton;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.springframework.stereotype.Component;

/**
 * "📱 Batafsil": opens the matching Mini App page (charts, calendars and
 * pictures live there now — the bot itself only sends text). Null when the
 * Mini App is not configured, so the button simply does not appear.
 */
@Component
public class MiniAppLinks {

    private final TelegramProperties properties;

    public MiniAppLinks(TelegramProperties properties) {
        this.properties = properties;
    }

    /** @param page Mini App route after "/webapp": "" (today), "/grades", "/attendance", … */
    public InlineButton details(BotContext ctx, String page) {
        String url = properties.webappUrlIfValid();
        return url == null ? null : InlineButton.webApp(ctx.t("notif.btn.app_details"), url + page);
    }
}
