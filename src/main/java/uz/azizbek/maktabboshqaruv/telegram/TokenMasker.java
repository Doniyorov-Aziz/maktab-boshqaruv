package uz.azizbek.maktabboshqaruv.telegram;

import java.util.regex.Pattern;

/**
 * Scrubs bot tokens out of any text before it reaches a log line or the
 * database. HTTP client exceptions quote the request URL, and the Bot API puts
 * the token in the URL path — so every error message is passed through here.
 */
public final class TokenMasker {

    private static final Pattern TOKEN = Pattern.compile("\\d{5,12}:[A-Za-z0-9_-]{20,}");

    private TokenMasker() {
    }

    public static String mask(String text) {
        if (text == null) return null;
        return TOKEN.matcher(text).replaceAll("***:***");
    }

    public static String mask(String text, String token) {
        if (text == null) return null;
        String result = text;
        if (token != null && !token.isBlank()) {
            result = result.replace(token, "***:***");
        }
        return mask(result);
    }
}
