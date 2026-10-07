package uz.azizbek.maktabboshqaruv.service.appeal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;

/**
 * Short-lived links for appeal files. A browser's {@code <img>}, {@code <video>} and
 * {@code <audio>} cannot send an Authorization header, so the chat page gets, for each
 * file the signed-in user is allowed to see, a link signed for that one file and that
 * user, valid for 30 minutes. The signature (HMAC-SHA256 with the JWT secret) is checked
 * on every request; nothing about the file location is in the link.
 */
@Component
public class AttachmentLinks {

    static final long TTL_SECONDS = 30 * 60;

    private final byte[] key;
    private final Clock clock;

    public AttachmentLinks(@Value("${jwt.secret}") String secret, Clock clock) {
        this.key = ("appeal-file:" + secret).getBytes(StandardCharsets.UTF_8);
        this.clock = clock;
    }

    /** "?t=<exp>.<user>.<sig>" — the query string for /api/appeals/{id}/attachments/{msgId}. */
    public String token(Long appealId, Long messageId, String username) {
        long exp = clock.instant().getEpochSecond() + TTL_SECONDS;
        String user = Base64.getUrlEncoder().withoutPadding().encodeToString(username.getBytes(StandardCharsets.UTF_8));
        return exp + "." + user + "." + sign(appealId + ":" + messageId + ":" + exp + ":" + username);
    }

    /** The username the token was issued to, or null when it is forged, for another file, or expired. */
    public String verify(Long appealId, Long messageId, String token) {
        if (token == null) return null;
        String[] parts = token.split("\\.");
        if (parts.length != 3) return null;
        try {
            long exp = Long.parseLong(parts[0]);
            if (clock.instant().getEpochSecond() > exp) return null;
            String username = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            String expected = sign(appealId + ":" + messageId + ":" + exp + ":" + username);
            return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))
                    ? username : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
