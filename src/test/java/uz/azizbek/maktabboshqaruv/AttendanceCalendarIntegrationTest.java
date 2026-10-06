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
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/** Oylik kalendar: kunni belgilash shu kunning hamma darslariga yoziladi va oyda ko'rinadi. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class AttendanceCalendarIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final Map<DayOfWeek, String> WEEKDAY = Map.of(DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY,
            "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba", DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma",
            DayOfWeek.SATURDAY, "Shanba");

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private StudentRepository studentRepository;
    @Autowired
    private LessonSlotRepository lessonSlotRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private User user;
    private ApiClient api;
    private Student student;
    private LocalDate day;
    private final Map<Long, AttendanceStatus> before = new HashMap<>();

    @BeforeEach
    void setUp() throws Exception {
        user = new User();
        user.setUsername("davomat_test_" + ThreadLocalRandom.current().nextInt(1_000_000));
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setRole(Role.EDITOR);
        user = userRepository.save(user);
        api = new ApiClient(port).login(user.getUsername(), PASSWORD);
        // a student whose class has lessons on the last past Monday
        day = LocalDate.now().minusDays(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        List<LessonSlot> monday = lessonSlotRepository.findAll().stream()
                .filter(l -> "Dushanba".equals(l.getWeekday())).toList();
        Long classId = monday.get(0).getSchoolClass().getId();
        student = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId).get(0);
        attendanceRepository.findByStudentIdAndRecordDate(student.getId(), day)
                .forEach(a -> before.put(a.getLessonSlot().getId(), a.getStatus()));
    }

    @AfterEach
    void cleanUp() {
        // put the day back as it was
        for (Attendance a : attendanceRepository.findByStudentIdAndRecordDate(student.getId(), day)) {
            AttendanceStatus old = before.get(a.getLessonSlot().getId());
            if (old == null) attendanceRepository.delete(a);
            else {
                a.setStatus(old);
                attendanceRepository.save(a);
            }
        }
        userRepository.delete(user);
    }

    @Test
    void markingADay_writesEveryLessonOfThatDay_andShowsInTheMonth() throws Exception {
        ApiClient.Response r = api.put("/api/attendance/day", Map.of("studentId", student.getId(),
                "date", day.toString(), "status", "EXCUSED", "comment", "Shifokorda"));
        assertEquals(200, r.status(), r.body());

        long lessons = lessonSlotRepository.findBySchoolClassId(student.getSchoolClass().getId()).stream()
                .filter(l -> WEEKDAY.get(day.getDayOfWeek()).equals(l.getWeekday())).count();
        List<Attendance> rows = attendanceRepository.findByStudentIdAndRecordDate(student.getId(), day);
        assertEquals(lessons, rows.size());
        assertTrue(rows.stream().allMatch(a -> a.getStatus() == AttendanceStatus.EXCUSED && "Shifokorda".equals(a.getComment())));

        JsonNode month = api.get("/api/attendance/calendar?schoolClassId=" + student.getSchoolClass().getId()
                + "&month=" + YearMonth.from(day)).json();
        JsonNode me = null;
        for (JsonNode s : month.get("students")) if (s.get("studentId").asLong() == student.getId()) me = s;
        assertNotNull(me);
        assertEquals("EXCUSED", me.get("cells").get(day.toString()).get("status").asString());
        assertEquals("Shifokorda", me.get("cells").get(day.toString()).get("comment").asString());
        assertTrue(me.has("guardianName"));
        assertEquals(YearMonth.from(day).lengthOfMonth(), month.get("days").size());
    }

    @Test
    void sundayAndFuture_refused() throws Exception {
        LocalDate sunday = LocalDate.now().with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        assertEquals(409, api.put("/api/attendance/day", Map.of("studentId", student.getId(),
                "date", sunday.toString(), "status", "PRESENT")).status());
        assertEquals(409, api.put("/api/attendance/day", Map.of("studentId", student.getId(),
                "date", LocalDate.now().plusDays(3).toString(), "status", "PRESENT")).status());
    }
}
