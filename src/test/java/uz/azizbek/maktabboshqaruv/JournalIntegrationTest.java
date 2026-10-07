package uz.azizbek.maktabboshqaruv;

import tools.jackson.databind.JsonNode;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.GradeService;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.support.MutableClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The class journal: grade rules (Sunday 409 with the exact text, future 409, 1 / 6 → 400),
 * the month of days, the class overview (homeroom teacher and phone, or null), the teacher's
 * current lesson under a fake clock (during a lesson → that lesson, in a free period → 204),
 * another school's class → 403, VIEWER cannot grade.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
@Import(JournalIntegrationTest.FakeClock.class)
class JournalIntegrationTest {

    static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    /** Wednesday — a school day. */
    static final LocalDate DAY = LocalDate.of(2026, 10, 7);

    @TestConfiguration
    static class FakeClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(DAY.atTime(10, 0), TASHKENT);
        }
    }

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final Map<DayOfWeek, String> WEEKDAY = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");

    @LocalServerPort
    private int port;
    @Autowired
    private MutableClock clock;
    @Autowired
    private LessonSlotRepository slots;
    @Autowired
    private StudentRepository students;
    @Autowired
    private SchoolClassRepository classes;
    @Autowired
    private UserRepository users;
    @Autowired
    private EmployeeRepository employees;
    @Autowired
    private PositionRepository positions;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JdbcTemplate jdbc;

    private LessonSlot slot;
    private SchoolClass cls;
    private Long schoolId;
    private final List<User> createdUsers = new ArrayList<>();
    private final List<Employee> createdEmployees = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clock.set(DAY.atTime(10, 0));
        // a lesson of school 1 on Wednesday
        slot = slots.findAll().stream()
                .filter(l -> l.getWeekday().equals("Chorshanba")
                        && l.getSchoolClass().getAcademicYear().getSchool().getId() == 1L)
                .findFirst().orElseThrow();
        cls = slot.getSchoolClass();
        schoolId = cls.getAcademicYear().getSchool().getId();
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from grade where created_by like 'journal_%'");
        users.deleteAll(createdUsers);
        employees.deleteAll(createdEmployees);
    }

    private ApiClient as(Role role, Employee e) throws Exception {
        User u = new User();
        u.setUsername("journal_" + role + "_" + ThreadLocalRandom.current().nextInt(1_000_000));
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmployee(e);
        createdUsers.add(users.save(u));
        return new ApiClient(port).login(u.getUsername(), PASSWORD);
    }

    private Map<String, Object> grade(Student s, LocalDate date, int score) {
        return Map.of("studentId", s.getId(), "subjectId", slot.getSubject().getId(), "gradeDate", date.toString(),
                "score", score, "type", "CURRENT");
    }

    private Student someStudent() {
        return students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId()).get(0);
    }

    @Test
    void gradeRules_sunday409WithText_future409_outOfRange400() throws Exception {
        ApiClient admin = as(Role.ADMIN, null);
        Student s = someStudent();
        LocalDate sunday = DAY.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY));
        ApiClient.Response r = admin.post("/api/grades", grade(s, sunday, 5));
        assertEquals(409, r.status());
        assertEquals(GradeService.SUNDAY_MESSAGE, r.body());
        assertEquals("Uzr, bu kun yakshanba — maktab ishlamaydi. Baho qo'yib bo'lmaydi.", r.body());
        assertEquals(409, admin.post("/api/grades", grade(s, DAY.plusDays(1), 5)).status(), "tomorrow (fake clock)");
        assertEquals(400, admin.post("/api/grades", grade(s, DAY, 1)).status());
        assertEquals(400, admin.post("/api/grades", grade(s, DAY, 6)).status());
        // a normal grade today: saved with its author
        ApiClient.Response ok = admin.post("/api/grades", grade(s, DAY, 5));
        assertEquals(201, ok.status(), ok.body());
        assertTrue(ok.json().get("createdBy").asString().startsWith("journal_"));
    }

    @Test
    void journal_listsEveryDayOfTheMonth_withSundaysAndLessonDays() throws Exception {
        ApiClient viewer = as(Role.VIEWER, null);
        ApiClient admin = as(Role.ADMIN, null);
        Student s = someStudent();
        assertEquals(201, admin.post("/api/grades", grade(s, DAY, 4)).status());

        JsonNode j = viewer.get("/api/grades/journal?classId=" + cls.getId() + "&subjectId=" + slot.getSubject().getId()
                + "&month=2026-10").json();
        assertEquals(31, j.get("days").size());
        Set<String> lessonDays = new HashSet<>();
        for (LessonSlot l : slots.findBySchoolClassId(cls.getId())) {
            if (l.getSubject().getId().equals(slot.getSubject().getId())) lessonDays.add(l.getWeekday());
        }
        for (JsonNode d : j.get("days")) {
            LocalDate date = LocalDate.parse(d.get("date").asString());
            assertEquals(date.getDayOfWeek() == DayOfWeek.SUNDAY, d.get("isSunday").asBoolean(), date.toString());
            assertEquals(date.isAfter(DAY), d.get("isFuture").asBoolean(), date.toString());
            if (date.getDayOfWeek() != DayOfWeek.SUNDAY && !d.get("isHoliday").asBoolean()) {
                assertEquals(lessonDays.contains(WEEKDAY.get(date.getDayOfWeek())), d.get("hasLesson").asBoolean(), date.toString());
            } else {
                assertFalse(d.get("hasLesson").asBoolean());
            }
        }
        assertEquals("Ya", j.get("days").get(3).get("weekday").asString(), "4 Oct 2026 is a Sunday");
        // students alphabetical by surname, the grade is there
        List<String> names = new ArrayList<>();
        for (JsonNode st : j.get("students")) names.add(st.get("fullName").asString());
        List<String> sorted = new ArrayList<>(names);
        sorted.sort(Comparator.naturalOrder());
        assertEquals(students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId()).size(), names.size());
        assertEquals(sorted.get(0).split(" ")[0], names.get(0).split(" ")[0]);
        boolean found = false;
        for (JsonNode g : j.get("grades")) {
            found |= g.get("studentId").asLong() == s.getId() && g.get("date").asString().equals(DAY.toString())
                    && g.get("value").asInt() == 4;
        }
        assertTrue(found);
        assertFalse(j.get("subjectTeacher").isNull());
        // a viewer may look but not grade
        assertEquals(403, viewer.post("/api/grades", grade(s, DAY, 5)).status());
    }

    @Test
    void overview_homeroomTeacherWithPhone_orNull() throws Exception {
        ApiClient viewer = as(Role.VIEWER, null);
        Employee previous = cls.getClassTeacher();
        try {
            Employee t = new Employee();
            t.setSchool(cls.getAcademicYear().getSchool());
            t.setPosition(positions.findAll().get(0));
            t.setFirstName("Dilnoza");
            t.setLastName("Karimova");
            t.setPhone("+998901234567");
            createdEmployees.add(t = employees.save(t));
            cls.setClassTeacher(t);
            classes.save(cls);
            JsonNode o = viewer.get("/api/classes/" + cls.getId() + "/overview").json();
            assertEquals("Karimova Dilnoza", o.get("homeroomTeacher").get("fullName").asString());
            assertEquals("+998901234567", o.get("homeroomTeacher").get("phone").asString());
            assertFalse(o.get("isDayOff").asBoolean());
            assertTrue(o.get("todayLessons").size() > 0, "Wednesday lessons");
            int previousNo = 0;
            for (JsonNode l : o.get("todayLessons")) {
                assertTrue(l.get("lessonNo").asInt() > previousNo);
                previousNo = l.get("lessonNo").asInt();
            }
            assertTrue(o.get("subjects").size() > 0);

            cls.setClassTeacher(null);
            classes.save(cls);
            assertTrue(viewer.get("/api/classes/" + cls.getId() + "/overview").json().get("homeroomTeacher").isNull());

            // Sunday → day off, no lessons
            clock.set(DAY.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).atTime(10, 0));
            JsonNode sun = viewer.get("/api/classes/" + cls.getId() + "/overview").json();
            assertTrue(sun.get("isDayOff").asBoolean());
            assertEquals(0, sun.get("todayLessons").size());
        } finally {
            cls.setClassTeacher(previous);
            classes.save(cls);
        }
    }

    @Test
    void currentLesson_duringTheLesson_andInAFreePeriod() throws Exception {
        Employee teacher = slot.getEmployee();
        ApiClient t = as(Role.EDITOR, teacher);
        List<LessonSlot> day = slots.findOfTeacherOnDay(teacher.getId(), "Chorshanba");
        LessonSlot first = day.get(0);

        clock.set(DAY.atTime(first.getStartTime().plusMinutes(10)));
        ApiClient.Response now = t.get("/api/me/current-lesson");
        assertEquals(200, now.status(), now.body());
        JsonNode cur = now.json();
        assertEquals(first.getSchoolClass().getId(), cur.get("classId").asLong());
        assertEquals(first.getSubject().getId(), cur.get("subjectId").asLong());
        int expectedNo = slots.startTimesOfClass(first.getSchoolClass().getId()).indexOf(first.getStartTime()) + 1;
        assertEquals(expectedNo, cur.get("lessonNo").asInt());

        // 10 minutes after the lesson ended it is still "current" (time to finish the grades)
        clock.set(DAY.atTime(first.getEndTime().plusMinutes(10)));
        boolean nextStartsSoon = day.stream().anyMatch(l -> !l.getStartTime().isAfter(first.getEndTime().plusMinutes(10))
                && l != first && l.getStartTime().isAfter(first.getEndTime().minusMinutes(1)));
        if (!nextStartsSoon) assertEquals(200, t.get("/api/me/current-lesson").status());

        // a free moment: no lesson now, none ended in the last 15 minutes
        LocalTime free = null;
        for (LocalTime m = LocalTime.of(7, 0); m.isBefore(LocalTime.of(19, 0)); m = m.plusMinutes(5)) {
            LocalTime at = m;
            boolean busy = day.stream().anyMatch(l -> !at.isBefore(l.getStartTime())
                    && !at.isAfter(l.getEndTime().plusMinutes(15)));
            if (!busy) {
                free = at;
                break;
            }
        }
        assertNotNull(free);
        clock.set(DAY.atTime(free));
        assertEquals(204, t.get("/api/me/current-lesson").status(), "free at " + free);

        // a user without an employee has no lessons
        assertEquals(204, as(Role.ADMIN, null).get("/api/me/current-lesson").status());
    }

    @Test
    void anotherSchoolsClass_is403() throws Exception {
        SchoolClass other = classes.findAll().stream()
                .filter(c -> !c.getAcademicYear().getSchool().getId().equals(schoolId)).findFirst().orElseThrow();
        Employee e = new Employee();
        e.setSchool(cls.getAcademicYear().getSchool());
        e.setPosition(positions.findAll().get(0));
        e.setFirstName("Scope");
        e.setLastName("Journal");
        e.setPhone("+9989" + (50_000_000 + ThreadLocalRandom.current().nextInt(1_000_000)));
        createdEmployees.add(e = employees.save(e));
        ApiClient own = as(Role.EDITOR, e);
        assertEquals(403, own.get("/api/classes/" + other.getId() + "/overview").status());
        assertEquals(403, own.get("/api/grades/journal?classId=" + other.getId() + "&subjectId=" + slot.getSubject().getId()
                + "&month=2026-10").status());
        assertEquals(200, own.get("/api/classes/" + cls.getId() + "/overview").status());
    }
}
