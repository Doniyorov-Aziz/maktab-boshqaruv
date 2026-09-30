package uz.azizbek.maktabboshqaruv.bot;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * State of an in-progress conversation (writing to the school, an absence
 * request), stored on ParentSession as "k=v;k=v". Values are URL-style
 * escaped so a parent's comment can contain ';' or '='.
 */
public final class PendingData {

    private final Map<String, String> values = new LinkedHashMap<>();

    public static PendingData parse(String raw) {
        PendingData d = new PendingData();
        if (raw == null || raw.isBlank()) return d;
        for (String part : raw.split(";")) {
            int eq = part.indexOf('=');
            if (eq > 0) d.values.put(part.substring(0, eq), decode(part.substring(eq + 1)));
        }
        return d;
    }

    public PendingData put(String key, Object value) {
        if (value == null) values.remove(key);
        else values.put(key, String.valueOf(value));
        return this;
    }

    public String get(String key) {
        return values.get(key);
    }

    public Long getLong(String key) {
        String v = values.get(key);
        try {
            return v == null ? null : Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        values.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(';');
            sb.append(k).append('=').append(encode(v));
        });
        return sb.toString();
    }

    private static String encode(String v) {
        return v.replace("%", "%25").replace(";", "%3B").replace("=", "%3D");
    }

    private static String decode(String v) {
        return v.replace("%3D", "=").replace("%3B", ";").replace("%25", "%");
    }
}
