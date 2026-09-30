package uz.azizbek.maktabboshqaruv.bot;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Every parent-facing bot text comes from {@code bot/i18n/messages_<lang>.properties}
 * (uz = O'zbekcha lotin, cy = Ўзбекча кирилл, ru = Русский) — no sentence is
 * hard-coded in Java. {@code {name}} placeholders are filled from key/value
 * pairs; a key missing from a translation falls back to Uzbek.
 */
public final class BotI18n {

    public static final String DEFAULT_LANG = "uz";
    public static final List<String> LANGUAGES = List.of("uz", "cy", "ru");

    private static final BotI18n INSTANCE = new BotI18n();

    private final Map<String, Properties> bundles;

    private BotI18n() {
        bundles = Map.of("uz", load("uz"), "cy", load("cy"), "ru", load("ru"));
    }

    public static BotI18n get() {
        return INSTANCE;
    }

    private static Properties load(String lang) {
        String path = "/bot/i18n/messages_" + lang + ".properties";
        try (InputStream in = BotI18n.class.getResourceAsStream(path)) {
            Properties p = new Properties();
            if (in != null) {
                p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
            return p;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String normalize(String lang) {
        return lang != null && LANGUAGES.contains(lang) ? lang : DEFAULT_LANG;
    }

    public Set<String> keys(String lang) {
        return bundles.get(normalize(lang)).stringPropertyNames();
    }

    public boolean has(String lang, String key) {
        return bundles.get(normalize(lang)).getProperty(key) != null;
    }

    /** Text for {@code key}; {@code args} are alternating placeholder names and values. */
    public String t(String lang, String key, Object... args) {
        String template = bundles.get(normalize(lang)).getProperty(key);
        if (template == null) template = bundles.get(DEFAULT_LANG).getProperty(key);
        if (template == null) return key;
        if (args.length == 0) return template;
        java.util.Map<String, String> values = new java.util.HashMap<>();
        for (int i = 0; i + 1 < args.length; i += 2) {
            values.put(String.valueOf(args[i]), args[i + 1] == null ? "" : String.valueOf(args[i + 1]));
        }
        // Single pass over the template: inserted values are never re-scanned,
        // so a "{...}" typed by a user inside a value stays literal text.
        StringBuilder sb = new StringBuilder(template.length() + 64);
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int end = template.indexOf('}', i);
                if (end > i) {
                    String name = template.substring(i + 1, end);
                    if (values.containsKey(name)) {
                        sb.append(values.get(name));
                        i = end + 1;
                        continue;
                    }
                }
            }
            sb.append(c);
            i++;
        }
        return sb.toString();
    }

    // ---- dates: "30-sentabr, seshanba" / "30 сентября, вторник" ----

    public String dateFull(String lang, LocalDate d) {
        return t(lang, "date.full", "day", d.getDayOfMonth(), "month", t(lang, "month.gen." + d.getMonthValue()),
                "dow", dow(lang, d.getDayOfWeek()));
    }

    public String dateShort(String lang, LocalDate d) {
        return t(lang, "date.short", "day", d.getDayOfMonth(), "month", t(lang, "month.gen." + d.getMonthValue()));
    }

    public String monthYear(String lang, int month, int year) {
        return t(lang, "month." + month) + " " + year;
    }

    public String dow(String lang, DayOfWeek d) {
        return t(lang, "dow." + d.getValue());
    }

    public String dowTitle(String lang, DayOfWeek d) {
        return t(lang, "dow.title." + d.getValue());
    }

    public String dowShort(String lang, DayOfWeek d) {
        return t(lang, "dow.short." + d.getValue());
    }
}
