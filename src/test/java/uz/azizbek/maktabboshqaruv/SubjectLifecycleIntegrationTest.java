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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Fanlar o'chirilmaydi: nofaol fan yangi bahoda tanlanmaydi (409), eski baho esa
 * fan nomini (va "nofaol" belgisini) qaytaradi. Over real HTTP as an EDITOR.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class SubjectLifecycleIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private ApiClient api;
    private Student student;
    private Long schoolId;
    private String name;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setUsername("fan_test_" + ThreadLocalRandom.current().nextInt(1_000_000));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.ADMIN);
        user = userRepository.save(user);
        api = new ApiClient(port).login(user.getUsername(), PASSWORD);
        student = studentRepository.findAll().get(0);
        schoolId = student.getSchoolClass().getAcademicYear().getSchool().getId();
        name = "Astronomiya " + ThreadLocalRandom.current().nextInt(1_000_000);
    }

    @AfterEach
    void cleanUp() {
        userRepository.delete(user);
    }

    /** The last weekday (Mon–Sat) that is not in the future. */
    private static LocalDate lastSchoolDay() {
        LocalDate d = LocalDate.now();
        while (d.getDayOfWeek() == DayOfWeek.SUNDAY) d = d.minusDays(1);
        return d;
    }

    private ApiClient.Response grade(long subjectId) throws Exception {
        return api.post("/api/grades", Map.of("studentId", student.getId(), "subjectId", subjectId,
                "gradeDate", lastSchoolDay().toString(), "score", 5, "type", "CURRENT", "comment", ""));
    }

    @Test
    void inactiveSubject_notOfferedForNewGrades_oldGradeKeepsItsName() throws Exception {
        ApiClient.Response created = api.post("/api/subjects", Map.of("name", name, "schoolId", schoolId));
        assertEquals(201, created.status(), created.body());
        long subjectId = created.json().get("id").asLong();

        ApiClient.Response first = grade(subjectId);
        assertEquals(201, first.status(), first.body());
        long gradeId = first.json().get("id").asLong();

        // "O'chirish" = nofaol qilish
        assertEquals(200, api.delete("/api/subjects/" + subjectId).status());
        assertTrue(subjectRepository.findById(subjectId).isPresent(), "the row is kept");

        ApiClient.Response refused = grade(subjectId);
        assertEquals(409, refused.status());
        assertTrue(refused.body().contains("nofaol"), refused.body());

        // pickers do not offer it, the "show inactive" list does
        assertFalse(api.get("/api/subjects?schoolId=" + schoolId + "&size=2000").body().contains(name));
        assertTrue(api.get("/api/subjects?schoolId=" + schoolId + "&size=2000&includeInactive=true").body().contains(name));

        // the old grade still shows the subject's name, marked inactive
        JsonNode book = api.get("/api/grades/gradebook?schoolClassId=" + student.getSchoolClass().getId()
                + "&subjectId=" + subjectId + "&from=" + lastSchoolDay().minusDays(1) + "&to=" + lastSchoolDay()).json();
        JsonNode old = null;
        for (JsonNode g : book.get("grades")) if (g.get("id").asLong() == gradeId) old = g;
        assertNotNull(old, book.toString());
        assertEquals(name, old.get("subjectName").asString());
        assertFalse(old.get("subjectActive").asBoolean());

        // and it can be brought back
        assertEquals(200, api.put("/api/subjects/" + subjectId + "/activate", null).status());
        assertEquals(201, grade(subjectId).status());
        gradeRepository.findAll().stream().filter(g -> g.getSubject().getId().equals(subjectId)).forEach(gradeRepository::delete);
    }

    @Test
    void renamingAGradedSubject_keepsTheOldNameOnOldGrades() throws Exception {
        long oldId = api.post("/api/subjects", Map.of("name", name, "schoolId", schoolId)).json().get("id").asLong();
        long gradeId = grade(oldId).json().get("id").asLong();

        ApiClient.Response renamed = api.put("/api/subjects/" + oldId, Map.of("name", name + " (yangi)", "schoolId", schoolId));
        assertEquals(200, renamed.status(), renamed.body());
        long newId = renamed.json().get("id").asLong();

        assertNotEquals(oldId, newId, "a graded subject is retired, a new one continues");
        assertFalse(subjectRepository.findById(oldId).orElseThrow().isActive());
        assertEquals(name, gradeRepository.findById(gradeId).orElseThrow().getSubject().getName());
        gradeRepository.deleteById(gradeId);
    }
}
