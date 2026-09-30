package uz.azizbek.maktabboshqaruv.telegram;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class TelegramInitDataValidatorTest {

    // Built at runtime: no token-shaped literal anywhere in the source tree.
    private static final String TOKEN = "7" + "0".repeat(8) + ":" + "T".repeat(35);
    private static final Instant NOW = Instant.parse("2026-09-30T10:00:00Z");
    private static final long AUTH_DATE = NOW.getEpochSecond() - 60;
    private static final String USER = "{\"id\":900001,\"first_name\":\"Dilnoza\",\"username\":\"dilnoza_opa\",\"language_code\":\"uz\"}";

    /** Independent implementation of Telegram's algorithm, so the test does not just mirror the validator. */
    private static String telegramSigned(String token, long authDate, String user) throws Exception {
        String check = "auth_date=" + authDate + "\nquery_id=AAHq\nuser=" + user;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("WebAppData".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] secret = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        String hash = HexFormat.of().formatHex(mac.doFinal(check.getBytes(StandardCharsets.UTF_8)));
        return "query_id=AAHq&user=" + URLEncoder.encode(user, StandardCharsets.UTF_8)
                + "&auth_date=" + authDate + "&hash=" + hash;
    }

    @Test
    void genuineInitData_isAccepted_andUserParsed() throws Exception {
        TelegramInitDataValidator.WebAppUser user =
                TelegramInitDataValidator.validate(telegramSigned(TOKEN, AUTH_DATE, USER), TOKEN, 86400, NOW);
        assertEquals(900001L, user.id());
        assertEquals("Dilnoza", user.firstName());
        assertEquals("dilnoza_opa", user.username());
        assertEquals("uz", user.languageCode());
    }

    @Test
    void tamperedUser_isRejected() throws Exception {
        String genuine = telegramSigned(TOKEN, AUTH_DATE, USER);
        String forged = genuine.replace("900001", "900002"); // try to read another parent's children
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate(forged, TOKEN, 86400, NOW));
    }

    @Test
    void signedWithAnotherBotsToken_isRejected() throws Exception {
        String otherToken = "8" + "1".repeat(8) + ":" + "X".repeat(35);
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate(telegramSigned(otherToken, AUTH_DATE, USER), TOKEN, 86400, NOW));
    }

    @Test
    void expiredOrFutureAuthDate_isRejected() throws Exception {
        long twoDaysAgo = NOW.getEpochSecond() - 2 * 86400;
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate(telegramSigned(TOKEN, twoDaysAgo, USER), TOKEN, 86400, NOW));
        long future = NOW.getEpochSecond() + 3600;
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate(telegramSigned(TOKEN, future, USER), TOKEN, 86400, NOW));
    }

    @Test
    void missingOrMalformedFields_areRejected() {
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate("", TOKEN, 86400, NOW));
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate("user=%7B%7D&auth_date=1", TOKEN, 86400, NOW));
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate("auth_date=1&hash=zz", TOKEN, 86400, NOW));
        assertThrows(TelegramInitDataValidator.InvalidInitData.class,
                () -> TelegramInitDataValidator.validate("auth_date=1&hash=00", null, 86400, NOW));
    }

    @Test
    void signHelper_matchesTelegramsAlgorithm() {
        String signed = TelegramInitDataValidator.sign(java.util.Map.of(
                "auth_date", String.valueOf(AUTH_DATE), "query_id", "AAHq", "user", USER), TOKEN);
        assertEquals(900001L, TelegramInitDataValidator.validate(signed, TOKEN, 86400, NOW).id());
    }
}
