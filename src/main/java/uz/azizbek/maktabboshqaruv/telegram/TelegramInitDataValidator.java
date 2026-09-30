package uz.azizbek.maktabboshqaruv.telegram;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/**
 * Verifies Telegram Mini App {@code initData} exactly as Telegram documents it
 * (core.telegram.org/bots/webapps#validating-data-received-via-the-mini-app):
 *
 * <pre>
 * data_check_string = all fields except "hash", as "key=value", sorted by key, joined by "\n"
 * secret_key        = HMAC_SHA256(key = "WebAppData", message = bot_token)
 * expected_hash     = hex(HMAC_SHA256(key = secret_key, message = data_check_string))
 * </pre>
 *
 * Also rejects stale data (auth_date older than maxAge) so a leaked initData
 * cannot be replayed forever.
 */
public final class TelegramInitDataValidator {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private TelegramInitDataValidator() {
    }

    public static class InvalidInitData extends RuntimeException {
        public InvalidInitData(String message) {
            super(message);
        }
    }

    public record WebAppUser(long id, String firstName, String username, String languageCode) {
    }

    public static WebAppUser validate(String initData, String botToken, long maxAgeSeconds, Instant now) {
        if (initData == null || initData.isBlank()) throw new InvalidInitData("initData yo'q");
        if (botToken == null || botToken.isBlank()) throw new InvalidInitData("Bot tokeni sozlanmagan");

        Map<String, String> fields = new TreeMap<>();
        for (String pair : initData.split("&")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            fields.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
        }
        String hash = fields.remove("hash");
        if (hash == null) throw new InvalidInitData("hash yo'q");

        StringBuilder check = new StringBuilder();
        fields.forEach((k, v) -> {
            if (check.length() > 0) check.append('\n');
            check.append(k).append('=').append(v);
        });

        byte[] expected = hmac(hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken), check.toString());
        byte[] given;
        try {
            given = HexFormat.of().parseHex(hash);
        } catch (IllegalArgumentException e) {
            throw new InvalidInitData("hash noto'g'ri formatda");
        }
        if (!MessageDigest.isEqual(expected, given)) throw new InvalidInitData("Imzo mos kelmadi");

        long authDate;
        try {
            authDate = Long.parseLong(fields.getOrDefault("auth_date", ""));
        } catch (NumberFormatException e) {
            throw new InvalidInitData("auth_date yo'q");
        }
        long age = now.getEpochSecond() - authDate;
        if (age > maxAgeSeconds) throw new InvalidInitData("initData eskirgan");
        if (age < -60) throw new InvalidInitData("auth_date kelajakda");

        String userJson = fields.get("user");
        if (userJson == null) throw new InvalidInitData("user yo'q");
        JsonNode user = JSON.readTree(userJson);
        if (user.get("id") == null) throw new InvalidInitData("user.id yo'q");
        return new WebAppUser(user.get("id").asLong(), text(user, "first_name"), text(user, "username"),
                text(user, "language_code"));
    }

    /** Builds a correctly signed initData — used by tests and by the mock-mode demo. */
    public static String sign(Map<String, String> fields, String botToken) {
        Map<String, String> sorted = new TreeMap<>(fields);
        StringBuilder check = new StringBuilder();
        StringBuilder query = new StringBuilder();
        sorted.forEach((k, v) -> {
            if (check.length() > 0) check.append('\n');
            check.append(k).append('=').append(v);
            if (query.length() > 0) query.append('&');
            query.append(k).append('=').append(java.net.URLEncoder.encode(v, StandardCharsets.UTF_8));
        });
        String hash = HexFormat.of().formatHex(hmac(hmac("WebAppData".getBytes(StandardCharsets.UTF_8), botToken), check.toString()));
        return query + "&hash=" + hash;
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null || v.isNull() ? null : v.asString();
    }

    private static byte[] hmac(byte[] key, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
}
