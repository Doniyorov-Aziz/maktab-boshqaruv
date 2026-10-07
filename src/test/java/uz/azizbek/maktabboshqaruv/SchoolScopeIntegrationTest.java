package uz.azizbek.maktabboshqaruv;

import tools.jackson.databind.JsonNode;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * "Boshqa maktab ma'lumoti ko'rinmasin": a user of school B gets 403 for school A's
 * records whether they are named in the path, a query parameter or a JSON body; their
 * own school works; lists of schools and users show only their school. A user without
 * a school (the super admin) is not limited.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class SchoolScopeIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private StudentRepository students;
    @Autowired
    private UserRepository users;
    @Autowired
    private EmployeeRepository employees;
    @Autowired
    private PositionRepository positions;
    @Autowired
    private PasswordEncoder encoder;

    private Student a;
    private Student b;
    private final List<User> createdUsers = new ArrayList<>();
    private final List<Employee> createdEmployees = new ArrayList<>();

    @BeforeEach
    void setUp() {
        List<Student> all = students.findAll();
        a = all.stream().filter(s -> school(s) == 1L).findFirst().orElseThrow();
        b = all.stream().filter(s -> school(s) != 1L).findFirst().orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        users.deleteAll(createdUsers);
        employees.deleteAll(createdEmployees);
    }

    private static long school(Student s) {
        return s.getSchoolClass().getAcademicYear().getSchool().getId();
    }

    private ApiClient userOf(Long schoolId, Role role) throws Exception {
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        Employee e = null;
        if (schoolId != null) {
            e = new Employee();
            e.setSchool(b.getSchoolClass().getAcademicYear().getSchool().getId().equals(schoolId)
                    ? b.getSchoolClass().getAcademicYear().getSchool() : a.getSchoolClass().getAcademicYear().getSchool());
            e.setPosition(positions.findAll().get(0));
            e.setFirstName("Scope");
            e.setLastName("Test");
            e.setPhone("+9989" + (40_000_000 + n));
            createdEmployees.add(e = employees.save(e));
        }
        User u = new User();
        u.setUsername("scope_" + role + "_" + n);
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmployee(e);
        createdUsers.add(users.save(u));
        return new ApiClient(port).login(u.getUsername(), PASSWORD);
    }

    @Test
    void anotherSchoolsRecords_are403_inPathParamsAndBody() throws Exception {
        long schoolB = school(b);
        ApiClient bAdmin = userOf(schoolB, Role.ADMIN);
        Long classA = a.getSchoolClass().getId();
        Long classB = b.getSchoolClass().getId();

        // path
        assertEquals(403, bAdmin.get("/api/students/" + a.getId()).status());
        assertEquals(200, bAdmin.get("/api/students/" + b.getId()).status());
        assertEquals(403, bAdmin.get("/api/school-classes/" + classA).status());
        assertEquals(403, bAdmin.get("/api/profiles/class/" + classA).status());
        assertEquals(403, bAdmin.get("/api/profiles/student/" + a.getId()).status());
        assertEquals(403, bAdmin.get("/api/telegram/students/" + a.getId()).status());
        assertEquals(403, bAdmin.get("/api/schools/" + school(a)).status());
        assertEquals(200, bAdmin.get("/api/schools/" + schoolB).status());

        // query parameters
        assertEquals(403, bAdmin.get("/api/students?schoolId=" + school(a)).status());
        assertEquals(403, bAdmin.get("/api/attendance/calendar?schoolClassId=" + classA + "&month=2026-09").status());
        assertEquals(200, bAdmin.get("/api/attendance/calendar?schoolClassId=" + classB + "&month=2026-09").status());
        assertEquals(403, bAdmin.get("/api/telegram/parents?classId=" + classA).status());

        // JSON body: moving a student of B into a class of A, or creating one there
        Map<String, Object> intoA = Map.of("firstName", "Test", "lastName", "Scope", "schoolClassId", classA,
                "birthDate", "2015-01-01");
        assertEquals(403, bAdmin.put("/api/students/" + b.getId(), intoA).status());
        assertEquals(403, bAdmin.post("/api/students", intoA).status());

        // lists show only the own school
        JsonNode schools = bAdmin.get("/api/schools").json();
        assertEquals(1, schools.get("totalElements").asInt());
        assertEquals(schoolB, schools.get("content").get(0).get("id").asLong());
        for (JsonNode u : bAdmin.get("/api/users?size=500").json().get("content")) {
            assertNotEquals("admin", u.get("username").asString(), "the super admin is not one of school B's users");
        }

        // an editor of B is limited the same way
        ApiClient bEditor = userOf(schoolB, Role.EDITOR);
        assertEquals(403, bEditor.get("/api/students/" + a.getId()).status());
    }

    @Test
    void userWithoutSchool_isNotLimited() throws Exception {
        ApiClient superAdmin = userOf(null, Role.ADMIN);
        assertEquals(200, superAdmin.get("/api/students/" + a.getId()).status());
        assertEquals(200, superAdmin.get("/api/students/" + b.getId()).status());
        assertTrue(superAdmin.get("/api/schools").json().get("totalElements").asInt() >= 2);
    }
}
