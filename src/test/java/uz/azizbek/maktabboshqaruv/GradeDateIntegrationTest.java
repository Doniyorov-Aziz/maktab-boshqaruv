package uz.azizbek.maktabboshqaruv;

import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.entity.Subject;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.GradeRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.repository.SubjectRepository;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/** Yakshanbaga va kelajak sanaga baho qo'yib bo'lmaydi — 409 aniq xabar bilan. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class GradeDateIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private SubjectRepository subjectRepository;
    @Autowired
    private GradeRepository gradeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private ApiClient api;
    private Student student;
    private Subject subject;

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setUsername("baho_test_" + ThreadLocalRandom.current().nextInt(1_000_000));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.EDITOR);
        user = userRepository.save(user);
        api = new ApiClient(port).login(user.getUsername(), PASSWORD);
        student = studentRepository.findAll().get(0);
        Long schoolId = student.getSchoolClass().getAcademicYear().getSchool().getId();
        subject = subjectRepository.findBySchoolId(schoolId).stream().filter(Subject::isActive).findFirst().orElseThrow();
    }

    @AfterEach
    void cleanUp() {
        userRepository.delete(user);
    }

    private ApiClient.Response grade(LocalDate date) throws Exception {
        return api.post("/api/grades", Map.of("studentId", student.getId(), "subjectId", subject.getId(),
                "gradeDate", date.toString(), "score", 4, "type", "CURRENT", "comment", ""));
    }

    @Test
    void sunday_409() throws Exception {
        LocalDate lastSunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        ApiClient.Response r = grade(lastSunday);
        assertEquals(409, r.status());
        assertEquals("Yakshanba kuni baho qo'yilmaydi", r.body());
    }

    @Test
    void future_409() throws Exception {
        LocalDate nextMonday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        ApiClient.Response r = grade(nextMonday);
        assertEquals(409, r.status());
        assertEquals("Kelajak sanaga baho qo'yilmaydi", r.body());
    }

    @Test
    void pastSchoolDay_ok() throws Exception {
        LocalDate lastSaturday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SATURDAY));
        ApiClient.Response r = grade(lastSaturday);
        assertEquals(201, r.status(), r.body());
        gradeRepository.deleteById(r.json().get("id").asLong());
    }
}
