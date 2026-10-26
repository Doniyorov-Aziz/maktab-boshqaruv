package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.util.JwtUtil;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The demo switched off (GUEST_LOGIN_ENABLED=false, the default): /api/auth/guest-login is 404 and
 * a guest token issued earlier no longer opens anything. Sign-in is limited to 10 attempts per
 * 5 minutes from one address.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.guest.login-enabled=false",
        "app.rate-limit.enabled=true",
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class GuestDemoLimitsIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private JwtUtil jwt;

    @Test
    void guestLoginIs404WhenTheDemoIsOff() throws Exception {
        ApiClient.Response r = new ApiClient(port).post("/api/auth/guest-login", Map.of());
        assertEquals(404, r.status(), r.body());
        assertFalse(new ApiClient(port).get("/api/auth/guest-enabled").json().get("enabled").asBoolean());
        // a token from the time the demo was on
        String old = jwt.generateToken("mehmon", "GUEST", null, 60_000);
        assertEquals(403, new ApiClient(port).withToken(old).get("/api/students?schoolId=1").status());
    }

    @Test
    void theEleventhSignInWithinFiveMinutesIs429() throws Exception {
        ApiClient api = new ApiClient(port);
        for (int i = 0; i < 10; i++) {
            assertEquals(401, api.post("/api/auth/login", Map.of("username", "nobody-" + i, "password", "x")).status());
        }
        ApiClient.Response r = api.post("/api/auth/login", Map.of("username", "nobody", "password", "x"));
        assertEquals(429, r.status());
        assertTrue(r.body().contains("Juda ko'p urinish"), r.body());
    }
}
