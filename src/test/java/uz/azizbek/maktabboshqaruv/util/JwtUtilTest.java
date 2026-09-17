package uz.azizbek.maktabboshqaruv.util;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private final String secret =
            Base64.getEncoder().encodeToString("test-secret-key-must-be-at-least-32-bytes!".getBytes());
    private final JwtUtil jwtUtil = new JwtUtil(secret);

    @Test
    void generateToken_roundTripsUsernameAndRole() {
        String token = jwtUtil.generateToken("azizbek", "ADMIN");

        assertTrue(jwtUtil.isTokenValid(token));
        assertEquals("azizbek", jwtUtil.extractUsername(token));
        assertEquals("ADMIN", jwtUtil.extractRole(token));
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertFalse(jwtUtil.isTokenValid("not-a-real-token"));
    }

    @Test
    void isTokenValid_tokenSignedWithDifferentKey_returnsFalse() {
        String otherSecret = Base64.getEncoder().encodeToString("another-secret-key-of-32-bytes!!".getBytes());
        JwtUtil otherJwtUtil = new JwtUtil(otherSecret);
        String token = otherJwtUtil.generateToken("azizbek", "ADMIN");

        assertFalse(jwtUtil.isTokenValid(token));
    }
}
