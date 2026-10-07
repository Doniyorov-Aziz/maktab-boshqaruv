package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.KeyboardButton;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The persistent bottom keyboard — the bot's main menu — and the reverse
 * lookup from what the parent typed or tapped to the screen it opens.
 *
 * Layout (two columns, the last item on a full row), with an optional
 * "📱 Kundalikni ochish" Mini App row on top:
 * <pre>
 *   📅 Dars jadvali   | ✅ Davomat
 *   📘 Baholar        | 📊 Hisobot
 *   📢 E'lonlar       | 🗓 Tadbirlar
 *   👩‍🏫 O'qituvchilar  | ✉️ Ma'muriyatga xat
 *   🤒 Sababli ariza  | ⚙️ Sozlamalar
 *   👨‍👩‍👧 Farzandlarim
 * </pre>
 * Buttons carry a Bot API 9.4 color style: sections "primary", the absence
 * request "danger"; Telegram apps older than February 2026 show them plain.
 * Labels are recognised in all three languages, ignoring case, emoji and
 * apostrophes — "davomat", "✅ Davomat" and "DAVOMAT" all open attendance.
 */
public final class Keyboards {

    /** label key -> screen code, in keyboard order. */
    public static final List<String[]> MAIN = List.of(
            new String[]{"kb.schedule", "sch"}, new String[]{"kb.attendance", "att"},
            new String[]{"kb.grades", "gr"}, new String[]{"kb.report", "rep"},
            new String[]{"kb.announcements", "ann"}, new String[]{"kb.events", "ev"},
            new String[]{"kb.teachers", "tch"}, new String[]{"kb.write", "msg"},
            new String[]{"kb.absence", "abs"}, new String[]{"kb.settings", "set"},
            new String[]{"kb.children", "ch"});

    /** Other labels that open a screen when typed (inline menu names, section names). */
    private static final List<String[]> ALIASES = List.of(
            new String[]{"menu.behavior", "beh"}, new String[]{"menu.absence", "abs"},
            new String[]{"menu.school", "info"}, new String[]{"sec.schedule", "sch"},
            new String[]{"sec.attendance", "att"}, new String[]{"sec.grades", "gr"},
            new String[]{"sec.report", "rep"}, new String[]{"sec.behavior", "beh"},
            new String[]{"sec.announcements", "ann"}, new String[]{"sec.events", "ev"},
            new String[]{"sec.teachers", "tch"}, new String[]{"sec.absence", "abs"},
            new String[]{"sec.school", "info"}, new String[]{"sec.settings", "set"},
            new String[]{"sec.children", "ch"}, new String[]{"kb.home", "home"},
            // the label of the old "write to school" button still on parents' keyboards
            new String[]{"kb.write_legacy", "msg"});

    private static final Map<String, String> LABEL_TO_SCREEN = new HashMap<>();
    private static volatile String webAppUrl;

    static {
        BotI18n i18n = BotI18n.get();
        for (String lang : BotI18n.LANGUAGES) {
            for (String[] item : ALIASES) LABEL_TO_SCREEN.put(normalize(i18n.t(lang, item[0])), item[1]);
            for (String[] item : MAIN) LABEL_TO_SCREEN.put(normalize(i18n.t(lang, item[0])), item[1]);
        }
        LABEL_TO_SCREEN.remove("");
    }

    private Keyboards() {
    }

    /** Set at startup from TELEGRAM_WEBAPP_URL (https only); null hides the Mini App row. */
    public static void setWebAppUrl(String url) {
        webAppUrl = url;
    }

    public static TelegramModels.ReplyKeyboardMarkup main(String lang) {
        BotI18n i18n = BotI18n.get();
        List<List<KeyboardButton>> rows = new ArrayList<>();
        String url = webAppUrl;
        if (url != null) rows.add(List.of(KeyboardButton.webApp(i18n.t(lang, "menu.webapp"), url)));
        for (int i = 0; i < MAIN.size(); i += 2) {
            List<KeyboardButton> row = new ArrayList<>();
            row.add(button(i18n, lang, MAIN.get(i)));
            if (i + 1 < MAIN.size()) row.add(button(i18n, lang, MAIN.get(i + 1)));
            rows.add(row);
        }
        return new TelegramModels.ReplyKeyboardMarkup(rows, true, false, true, i18n.t(lang, "kb.placeholder"));
    }

    private static KeyboardButton button(BotI18n i18n, String lang, String[] item) {
        return KeyboardButton.styled(i18n.t(lang, item[0]), "abs".equals(item[1]) ? "danger" : "primary");
    }

    /** Screen code for a keyboard label or a typed section name, or null. */
    public static String screenFor(String label) {
        return label == null ? null : LABEL_TO_SCREEN.get(normalize(label));
    }

    /** Lower case, letters and digits only (emoji, apostrophes and punctuation dropped), single spaces. */
    static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFC).toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        n.codePoints().forEach(cp -> {
            if (Character.isLetterOrDigit(cp)) sb.appendCodePoint(cp);
            else if (Character.isWhitespace(cp)) sb.append(' ');
        });
        return sb.toString().trim().replaceAll("\\s+", " ");
    }
}
