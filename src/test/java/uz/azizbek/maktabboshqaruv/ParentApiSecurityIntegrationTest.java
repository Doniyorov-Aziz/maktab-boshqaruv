package uz.azizbek.maktabboshqaruv;

import uz.azizbek.maktabboshqaruv.entity.ParentTelegramLink;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramInitDataValidator;
import uz.azizbek.maktabboshqaruv.telegram.TelegramProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Mini App API over real HTTP: a parent sees only their own child; another
 * child's id is 403, a forged or stale initData is 401. The test runs in mock
 * mode, where initData is signed with the fixed development key.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class ParentApiSecurityIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private ParentTelegramLinkRepository linkRepository;
    @Autowired
    private TelegramProperties properties;

    private final HttpClient http = HttpClient.newHttpClient();
    private long chatId;
    private Student own;
    private Student foreign;

    @BeforeEach
    void setUp() {
        chatId = 7_700_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        List<Student> students = studentRepository.findAll();
        own = students.get(0);
        foreign = students.stream().filter(s -> !s.getId().equals(own.getId())).findFirst().orElseThrow();
        ParentTelegramLink link = new ParentTelegramLink();
        link.setStudent(own);
        link.setChatId(chatId);
        link.setActive(true);
        link.setLinkedAt(LocalDateTime.now());
        linkRepository.save(link);
    }

    @AfterEach
    void cleanUp() {
        linkRepository.findByChatIdAndActiveTrue(chatId).forEach(linkRepository::delete);
    }

    private String initData(long authDate) {
        return TelegramInitDataValidator.sign(Map.of(
                "auth_date", String.valueOf(authDate),
                "query_id", "AAHtest",
                "user", "{\"id\":" + chatId + ",\"first_name\":\"Ota\",\"language_code\":\"uz\"}"),
                properties.initDataSigningToken());
    }

    private int get(String path, String initData) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (initData != null) req.header("X-Telegram-Init-Data", initData);
        return http.send(req.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void ownChild_200_foreignChild_403() throws Exception {
        String data = initData(Instant.now().getEpochSecond());
        assertEquals(200, get("/api/parent/students/" + own.getId() + "/today", data));
        assertEquals(200, get("/api/parent/students/" + own.getId() + "/grades", data));
        assertEquals(403, get("/api/parent/students/" + foreign.getId() + "/today", data));
        assertEquals(403, get("/api/parent/students/" + foreign.getId() + "/grades", data));
        assertEquals(403, get("/api/parent/students/" + foreign.getId() + "/events", data));
    }

    @Test
    void forgedOrStaleInitData_401() throws Exception {
        String fresh = initData(Instant.now().getEpochSecond());
        String tampered = fresh.replace("Ota", "Boshqa");
        assertEquals(401, get("/api/parent/me", tampered));
        String stale = initData(Instant.now().minusSeconds(25 * 3600).getEpochSecond());
        assertEquals(401, get("/api/parent/me", stale));
        assertEquals(401, get("/api/parent/me", null));
    }
}
