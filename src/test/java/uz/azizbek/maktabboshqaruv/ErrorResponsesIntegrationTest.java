package uz.azizbek.maktabboshqaruv;

import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A wrong request or a database rule is the user's mistake, not the server's: 400 / 409 with a
 * plain reason, never 500, and never the SQL, constraint or class names in the answer.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class ErrorResponsesIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository users;
    @Autowired
    private RoomRepository rooms;
    @Autowired
    private SchoolClassRepository classes;
    @Autowired
    private PasswordEncoder encoder;

    private User admin;
    private ApiClient api;

    @BeforeEach
    void setUp() throws Exception {
        admin = new User();
        admin.setUsername("errors_" + ThreadLocalRandom.current().nextInt(1_000_000));
        admin.setPassword(encoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        admin = users.save(admin);
        api = new ApiClient(port).login(admin.getUsername(), PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        users.delete(admin);
    }

    private static void plain(ApiClient.Response r, int status, String text) {
        assertEquals(status, r.status(), r.body());
        assertTrue(r.body().contains(text), r.body());
        for (String inside : new String[]{"SQL", "constraint", "Exception", "java.", "Kutilmagan"}) {
            assertFalse(r.body().contains(inside), "no internals in the answer: " + r.body());
        }
    }

    @Test
    void deletingWhatOthersPointTo_is409_andSaysWhat() throws Exception {
        long building = rooms.findAll().get(0).getBuilding().getId();
        plain(api.delete("/api/buildings/" + building), 409, "xonalar bog'langan");
        assertTrue(rooms.findByBuildingId(building).size() > 0, "nothing was deleted");
    }

    @Test
    void tooLongText_is400_withTheLimit() throws Exception {
        String name = "x".repeat(300);
        plain(api.send("POST", "/api/schools", "{\"name\":\"" + name + "\",\"address\":\"Toshkent\"}"), 400, "255");
    }

    @Test
    void wrongInput_is400() throws Exception {
        plain(api.get("/api/students/abc"), 400, "id");
        plain(api.get("/api/students?schoolId=abc"), 400, "schoolId");
        plain(api.send("POST", "/api/buildings", "{"), 400, "JSON");
        plain(api.send("POST", "/api/users", "{\"username\":\"q\",\"password\":\"123456\",\"role\":\"KING\"}"), 400, "role");
        plain(api.send("PUT", "/api/students/1", ""), 400, "yuborilmagan");
        long schoolClass = classes.findAll().get(0).getId();
        plain(api.get("/api/attendance/calendar?schoolClassId=" + schoolClass + "&month=bad"), 400, "bad");
        plain(api.get("/api/students?schoolId=1&sort=nope"), 400, "Saralash");
    }
}
