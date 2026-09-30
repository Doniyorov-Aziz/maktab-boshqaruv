package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels.KeyboardButton;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The persistent bottom keyboard (two columns of the main sections) and the
 * reverse lookup from a tapped label — in any of the three languages — to
 * the screen it opens.
 */
public final class Keyboards {

    /** label key -> screen code, in keyboard order. */
    public static final List<String[]> MAIN = List.of(
            new String[]{"kb.schedule", "sch"}, new String[]{"kb.attendance", "att"},
            new String[]{"kb.grades", "gr"}, new String[]{"kb.report", "rep"},
            new String[]{"kb.announcements", "ann"}, new String[]{"kb.events", "ev"},
            new String[]{"kb.teachers", "tch"}, new String[]{"kb.write", "msg"},
            new String[]{"kb.settings", "set"}, new String[]{"kb.children", "ch"});

    private static final Map<String, String> LABEL_TO_SCREEN = new HashMap<>();

    static {
        BotI18n i18n = BotI18n.get();
        for (String lang : BotI18n.LANGUAGES) {
            for (String[] item : MAIN) {
                LABEL_TO_SCREEN.put(i18n.t(lang, item[0]), item[1]);
            }
        }
    }

    private Keyboards() {
    }

    public static TelegramModels.ReplyKeyboardMarkup main(String lang) {
        BotI18n i18n = BotI18n.get();
        List<List<KeyboardButton>> rows = new java.util.ArrayList<>();
        for (int i = 0; i < MAIN.size(); i += 2) {
            rows.add(List.of(KeyboardButton.of(i18n.t(lang, MAIN.get(i)[0])),
                    KeyboardButton.of(i18n.t(lang, MAIN.get(i + 1)[0]))));
        }
        return new TelegramModels.ReplyKeyboardMarkup(rows, true, false);
    }

    /** Screen code for a reply-keyboard label, or null. */
    public static String screenFor(String label) {
        return label == null ? null : LABEL_TO_SCREEN.get(label.trim());
    }
}
