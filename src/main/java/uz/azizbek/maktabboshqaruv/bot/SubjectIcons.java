package uz.azizbek.maktabboshqaruv.bot;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** One consistent emoji per subject (matched by name fragment), and per event type / grade. */
public final class SubjectIcons {

    private static final Map<String, String> BY_FRAGMENT = new LinkedHashMap<>();

    static {
        BY_FRAGMENT.put("algebra", "➗");
        BY_FRAGMENT.put("geometr", "📐");
        BY_FRAGMENT.put("matem", "🔢");
        BY_FRAGMENT.put("fizika", "⚛️");
        BY_FRAGMENT.put("kimyo", "🧪");
        BY_FRAGMENT.put("biolog", "🧬");
        BY_FRAGMENT.put("tabiiy", "🌱");
        BY_FRAGMENT.put("ona tili", "📝");
        BY_FRAGMENT.put("adabiyot", "📖");
        BY_FRAGMENT.put("o'qish", "📖");
        BY_FRAGMENT.put("ingliz", "🇬🇧");
        BY_FRAGMENT.put("rus", "🇷🇺");
        BY_FRAGMENT.put("nemis", "🇩🇪");
        BY_FRAGMENT.put("tarix", "🏛");
        BY_FRAGMENT.put("geograf", "🌍");
        BY_FRAGMENT.put("informat", "💻");
        BY_FRAGMENT.put("musiqa", "🎵");
        BY_FRAGMENT.put("tasviriy", "🎨");
        BY_FRAGMENT.put("jismoniy", "🏃");
        BY_FRAGMENT.put("texnolog", "🛠");
        BY_FRAGMENT.put("tarbiya", "🤝");
        BY_FRAGMENT.put("huquq", "⚖️");
        BY_FRAGMENT.put("iqtisod", "📈");
        BY_FRAGMENT.put("chqbt", "🎖");
        BY_FRAGMENT.put("astronom", "🔭");
    }

    private SubjectIcons() {
    }

    public static String subject(String name) {
        if (name == null) return "📚";
        String n = name.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, String> e : BY_FRAGMENT.entrySet()) {
            if (n.contains(e.getKey())) return e.getValue();
        }
        return "📚";
    }

    public static String event(String type) {
        if (type == null) return "📌";
        return switch (type) {
            case "HOLIDAY" -> "🎉";
            case "EXAM" -> "📝";
            case "PARENT_MEETING" -> "👨‍👩‍👧";
            case "VACATION" -> "🏖";
            default -> "📌";
        };
    }

    public static String grade(int score) {
        return switch (score) {
            case 5 -> "🟢";
            case 4 -> "🔵";
            case 3 -> "🟡";
            default -> "🔴";
        };
    }

    /** Book colour for a subject average (📗 strong … 📕 weak). */
    public static String book(double average) {
        if (average >= 4.5) return "📗";
        if (average >= 3.5) return "📘";
        if (average >= 2.5) return "📙";
        return "📕";
    }
}
