package uz.azizbek.maktabboshqaruv.bot;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Compact, structured callback_data: {@code screen[:key:value]...}, e.g.
 * {@code att:k:m:m:2026-09:v:cal}. Telegram limits callback_data to 64 bytes,
 * which {@link #toString()} enforces. Values never contain ':'.
 *
 * Nothing parsed from here is trusted: a student id ("s") is re-checked
 * against the chat's links by the router before any data is read.
 */
public final class CallbackData {

    public static final int MAX_BYTES = 64;

    private final String screen;
    private final Map<String, String> params;

    private CallbackData(String screen, Map<String, String> params) {
        this.screen = screen;
        this.params = params;
    }

    public static CallbackData of(String screen) {
        return new CallbackData(screen, new LinkedHashMap<>());
    }

    public static CallbackData parse(String raw) {
        if (raw == null || raw.isBlank()) return of("home");
        String[] parts = raw.split(":");
        Map<String, String> params = new LinkedHashMap<>();
        for (int i = 1; i + 1 < parts.length; i += 2) {
            params.put(parts[i], parts[i + 1]);
        }
        return new CallbackData(parts[0], params);
    }

    public CallbackData with(String key, Object value) {
        Map<String, String> copy = new LinkedHashMap<>(params);
        if (value != null) {
            String v = String.valueOf(value);
            if (v.contains(":")) throw new IllegalArgumentException("callback qiymatida ':' bo'lmasligi kerak: " + v);
            copy.put(key, v);
        }
        return new CallbackData(screen, copy);
    }

    public String screen() {
        return screen;
    }

    public String get(String key) {
        return params.get(key);
    }

    public String get(String key, String fallback) {
        String v = params.get(key);
        return v == null ? fallback : v;
    }

    public Long getLong(String key) {
        String v = params.get(key);
        if (v == null) return null;
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public int getInt(String key, int fallback) {
        Long v = getLong(key);
        return v == null ? fallback : v.intValue();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(screen);
        params.forEach((k, v) -> sb.append(':').append(k).append(':').append(v));
        String s = sb.toString();
        if (s.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) {
            throw new IllegalStateException("callback_data 64 baytdan uzun: " + s);
        }
        return s;
    }
}
