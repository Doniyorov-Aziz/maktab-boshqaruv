package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.DistrictRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.support.ApiClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O'quvchining tug'ilgan joyi, yashash joyi va uy manzili over real HTTP: the seeded hudud → tuman
 * reference, saving a kvartira and a hovli address, the rules that keep them consistent, the profile.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class StudentAddressIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private DistrictRepository districtRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User user;
    private ApiClient api;
    private final List<Long> students = new ArrayList<>();
    private final List<Long> districts = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setUsername("manzil_test_" + ThreadLocalRandom.current().nextInt(1_000_000));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.ADMIN);
        user = userRepository.save(user);
        api = new ApiClient(port).login(user.getUsername(), PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        students.forEach(studentRepository::deleteById);
        districts.forEach(districtRepository::deleteById);
        userRepository.delete(user);
    }

    private long regionId(String name) throws Exception {
        for (JsonNode r : api.get("/api/regions").json()) {
            if (r.get("nameUz").asString().equals(name)) return r.get("id").asLong();
        }
        throw new AssertionError("hudud yo'q: " + name);
    }

    private long districtId(long regionId, String name) throws Exception {
        for (JsonNode d : api.get("/api/regions/" + regionId + "/districts").json()) {
            if (d.get("nameUz").asString().equals(name)) return d.get("id").asLong();
        }
        throw new AssertionError("tuman yo'q: " + name);
    }

    private Map<String, Object> student(SchoolClass c) {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Manzil");
        body.put("lastName", "Sinov");
        body.put("birthDate", "2014-05-01");
        body.put("schoolClassId", c.getId());
        return body;
    }

    @Test
    void kvartiraThenHovli_birthAndResidence_showInListAndProfile() throws Exception {
        long toshkent = regionId("Toshkent shahri");
        long samarqand = regionId("Samarqand viloyati");
        long chilonzor = districtId(toshkent, "Chilonzor tumani");
        long urgut = districtId(samarqand, "Urgut tumani");
        SchoolClass c = schoolClassRepository.findAll().get(0);

        Map<String, Object> body = student(c);
        body.put("birthRegionId", samarqand);
        body.put("birthDistrictId", urgut);
        body.put("residenceRegionId", toshkent);
        body.put("residenceDistrictId", chilonzor);
        body.put("mahalla", "Navro'z mahallasi");
        body.put("street", "Bunyodkor ko'chasi");
        body.put("housingType", "KVARTIRA");
        body.put("houseNumber", "12");
        body.put("apartmentNumber", "45");
        ApiClient.Response created = api.post("/api/students", body);
        assertEquals(201, created.status(), created.body());
        long id = created.json().get("id").asLong();
        students.add(id);
        assertEquals("Samarqand viloyati, Urgut tumani", created.json().get("birthPlace").asString());
        assertEquals("Toshkent shahri, Chilonzor tumani", created.json().get("residencePlace").asString());
        assertEquals("Navro'z mahallasi, Bunyodkor ko'chasi, 12-dom, 45-kvartira", created.json().get("address").asString());
        assertEquals(toshkent, created.json().get("residenceRegionId").asLong(), "the edit form gets the hudud back");

        JsonNode profile = api.get("/api/profiles/students/" + id).json();
        assertEquals("Samarqand viloyati, Urgut tumani", profile.get("birthPlace").asString());
        assertEquals("Navro'z mahallasi, Bunyodkor ko'chasi, 12-dom, 45-kvartira", profile.get("address").asString());
        assertEquals("KVARTIRA", profile.get("housingType").asString());

        // a hovli has a house number and no apartment
        body.put("housingType", "HOVLI");
        body.put("houseNumber", "17");
        assertEquals(400, api.put("/api/students/" + id, body).status(), "hovli with an apartment number");
        body.put("apartmentNumber", null);
        ApiClient.Response hovli = api.put("/api/students/" + id, body);
        assertEquals(200, hovli.status(), hovli.body());
        assertEquals("Navro'z mahallasi, Bunyodkor ko'chasi, 17-uy", hovli.json().get("address").asString());

        // the list (what the table shows) carries the place too
        String list = api.get("/api/students?schoolId=" + c.getAcademicYear().getSchool().getId()
                + "&schoolClassId=" + c.getId() + "&size=2000").body();
        assertTrue(list.contains("Toshkent shahri, Chilonzor tumani"), list);
    }

    @Test
    void inconsistentAddresses_areRefused() throws Exception {
        long toshkent = regionId("Toshkent shahri");
        long samarqand = regionId("Samarqand viloyati");
        long urgut = districtId(samarqand, "Urgut tumani");
        SchoolClass c = schoolClassRepository.findAll().get(0);

        Map<String, Object> wrongRegion = student(c);
        wrongRegion.put("residenceRegionId", toshkent);
        wrongRegion.put("residenceDistrictId", urgut);
        ApiClient.Response r1 = api.post("/api/students", wrongRegion);
        assertEquals(400, r1.status());
        assertTrue(r1.body().contains("tegishli emas"), r1.body());

        Map<String, Object> noApartment = student(c);
        noApartment.put("housingType", "KVARTIRA");
        noApartment.put("houseNumber", "3");
        assertEquals(400, api.post("/api/students", noApartment).status());

        Map<String, Object> noType = student(c);
        noType.put("houseNumber", "3");
        assertEquals(400, api.post("/api/students", noType).status());

        // no address at all is still fine (quick entry, older records)
        ApiClient.Response plain = api.post("/api/students", student(c));
        assertEquals(201, plain.status(), plain.body());
        students.add(plain.json().get("id").asLong());
        assertTrue(plain.json().get("address").isNull());
    }

    @Test
    void districtCrud_softDelete() throws Exception {
        long toshkent = regionId("Toshkent shahri");
        String name = "Sinov tumani " + ThreadLocalRandom.current().nextInt(1_000_000);
        ApiClient.Response created = api.post("/api/districts", Map.of("regionId", toshkent, "nameUz", name));
        assertEquals(201, created.status(), created.body());
        long id = created.json().get("id").asLong();
        districts.add(id);
        assertEquals("Toshkent shahri", created.json().get("regionName").asString());

        assertEquals(409, api.post("/api/districts", Map.of("regionId", toshkent, "nameUz", name)).status());
        assertTrue(api.get("/api/regions/" + toshkent + "/districts").body().contains(name));

        assertEquals(204, api.delete("/api/districts/" + id).status());
        assertFalse(api.get("/api/regions/" + toshkent + "/districts").body().contains(name));
        assertEquals(404, api.get("/api/districts/" + id).status());
    }
}
