package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.entity.Region;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.RegionRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.support.ApiClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/** Hududlar ma'lumotnomasi over real HTTP: CRUD, unique SOATO code (409), soft delete, roles. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class RegionIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RegionRepository regionRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<User> users = new ArrayList<>();
    private ApiClient admin;
    private String code;
    private String otherCode;

    @BeforeEach
    void setUp() throws Exception {
        admin = signedIn(Role.ADMIN);
        int n = ThreadLocalRandom.current().nextInt(100_000, 999_999);
        code = "T" + n;
        otherCode = "U" + n;
    }

    @AfterEach
    void cleanUp() {
        regionRepository.findAll().stream()
                .filter(r -> r.getCode().equals(code) || r.getCode().equals(otherCode))
                .forEach(regionRepository::delete);
        userRepository.deleteAll(users);
    }

    private ApiClient signedIn(Role role) throws Exception {
        User u = new User();
        u.setUsername("hudud_test_" + ThreadLocalRandom.current().nextInt(1_000_000));
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRole(role);
        users.add(userRepository.save(u));
        return new ApiClient(port).login(u.getUsername(), PASSWORD);
    }

    @Test
    void crud_uniqueCode_softDelete() throws Exception {
        ApiClient.Response created = admin.post("/api/regions", Map.of("code", code, "nameUz", "Test hudud"));
        assertEquals(201, created.status(), created.body());
        long id = created.json().get("id").asLong();
        assertEquals(code, created.json().get("code").asString());

        // the same code again -> 409
        ApiClient.Response duplicate = admin.post("/api/regions", Map.of("code", code, "nameUz", "Boshqa"));
        assertEquals(409, duplicate.status());
        assertTrue(duplicate.body().contains("allaqachon mavjud"), duplicate.body());

        // empty name -> 400
        assertEquals(400, admin.post("/api/regions", Map.of("code", otherCode, "nameUz", " ")).status());

        assertTrue(admin.get("/api/regions").body().contains(code));
        ApiClient.Response one = admin.get("/api/regions/" + id);
        assertEquals(200, one.status());
        assertEquals("Test hudud", one.json().get("nameUz").asString());

        ApiClient.Response updated = admin.put("/api/regions/" + id, Map.of("code", code, "nameUz", "Yangi nom"));
        assertEquals(200, updated.status(), updated.body());
        assertEquals("Yangi nom", regionRepository.findById(id).orElseThrow().getNameUz(), "saved without save()");

        // changing to a code another region has -> 409
        admin.post("/api/regions", Map.of("code", otherCode, "nameUz", "Ikkinchi"));
        assertEquals(409, admin.put("/api/regions/" + id, Map.of("code", otherCode, "nameUz", "Yangi nom")).status());

        // soft delete: 204, gone from the API, the row stays with state P
        assertEquals(204, admin.delete("/api/regions/" + id).status());
        assertEquals(404, admin.get("/api/regions/" + id).status());
        assertFalse(admin.get("/api/regions").body().contains("\"" + code + "\""));
        assertEquals(Region.DELETED, regionRepository.findById(id).orElseThrow().getState());
        assertEquals(404, admin.delete("/api/regions/" + id).status());
    }

    @Test
    void viewerReads_butCannotChange() throws Exception {
        ApiClient viewer = signedIn(Role.VIEWER);
        assertEquals(200, viewer.get("/api/regions").status());
        assertEquals(403, viewer.post("/api/regions", Map.of("code", code, "nameUz", "Test")).status());
        assertTrue(regionRepository.findAll().stream().noneMatch(r -> r.getCode().equals(code)));
    }
}
