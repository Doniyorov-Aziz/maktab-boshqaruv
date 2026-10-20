package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.config.CurrentYearSeeder;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.BuildingService;
import uz.azizbek.maktabboshqaruv.service.BuildingStatsService;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.service.pages.ListPagesService;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.support.MutableClock;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Today is 20.10.2026 (a fake clock): the school year of today is 2026–2027 and current; a year's
 * status follows its dates (the "Joriy" flag does not make a finished year current); with no year
 * of today the page and the dashboard say so and offer 2026–2027; the demo seeder adds that year
 * once — never twice, never far from its latest year — and moves its classes into it; a building's
 * occupancy is its busy lesson slots of the week, "busy now" apart; overlapping terms are a 409.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
class CrudFixIntegrationTest {

    static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @TestConfiguration
    static class FakeClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(TODAY.atTime(10, 0), TASHKENT);
        }
    }

    @LocalServerPort
    int port;
    @Autowired
    MutableClock clock;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    CurrentYearSeeder yearSeeder;
    @Autowired
    ListPagesService pages;
    @Autowired
    HomeDashboardService home;
    @Autowired
    BuildingService buildingService;
    @Autowired
    BuildingStatsService buildingStats;
    @Autowired
    PasswordEncoder encoder;
    @Autowired
    UserRepository users;
    @Autowired
    SchoolRepository schools;
    @Autowired
    AcademicYearRepository years;
    @Autowired
    PositionRepository positions;
    @Autowired
    EmployeeRepository employees;
    @Autowired
    SubjectRepository subjects;
    @Autowired
    BuildingRepository buildings;
    @Autowired
    RoomRepository rooms;
    @Autowired
    SchoolClassRepository classes;
    @Autowired
    LessonSlotRepository lessons;
    @Autowired
    StudentRepository students;

    TestSchool school;

    @BeforeEach
    void setUp() {
        clock.set(TODAY.atTime(10, 0));
        school = TestSchool.create(new TestSchool.Repos(schools, years, positions, employees, subjects, buildings,
                rooms, classes, lessons, students), jdbc, 2, 3);
    }

    @AfterEach
    void cleanUp() {
        school.delete();
    }

    long yearId() {
        return school.classes.get(0).getAcademicYear().getId();
    }

    /** The test school's own year becomes the seeder's "2025-2026" (01.09.2025 – 29.05.2026). */
    void seededPastYear() {
        jdbc.update("update academic_year set title = '2025-2026', start_date = '2025-09-01', end_date = '2026-05-29' where id = ?",
                yearId());
    }

    long addYear(String title, String start, String end, boolean current) {
        jdbc.update("insert into academic_year (school_id, title, start_date, end_date, is_current) values (?, ?, ?::date, ?::date, ?)",
                school.id(), title, start, end, current);
        return jdbc.queryForObject("select id from academic_year where school_id = ? and title = ?", Long.class, school.id(), title);
    }

    // ---------------------------------------------------------------- the year of today

    @Test
    void theSeeder_addsTheYearOfToday_once_andMovesItsClassesIntoIt() {
        seededPastYear();
        AcademicYear y = yearSeeder.ensure(school.school, TODAY).orElseThrow();
        assertEquals("2026-2027", y.getTitle());
        assertEquals(LocalDate.of(2026, 9, 2), y.getStartDate());
        assertEquals(LocalDate.of(2027, 5, 31), y.getEndDate());
        assertTrue(y.isCurrent());
        // the usual calendar: four quarters, three vacations, all inside the year
        List<Map<String, Object>> terms = jdbc.queryForList(
                "select kind, start_date, end_date from academic_term where academic_year_id = ? order by start_date", y.getId());
        assertEquals(7, terms.size());
        assertEquals(4, terms.stream().filter(t -> "QUARTER".equals(t.get("kind"))).count());
        // the classes (and with them students, lessons, attendance) now belong to it
        Integer moved = jdbc.queryForObject("select count(*) from school_class where academic_year_id = ?", Integer.class, y.getId());
        assertEquals(2, moved);

        // run again (every boot): the same year, no second one
        AcademicYear again = yearSeeder.ensure(school.school, TODAY).orElseThrow();
        assertEquals(y.getId(), again.getId());
        assertEquals(2, jdbc.queryForObject("select count(*) from academic_year where school_id = ?", Integer.class, school.id()));
        // a clock far from the latest year (a test on 2031-03-03) adds nothing
        assertEquals(Optional.empty(), yearSeeder.ensure(school.school, LocalDate.of(2031, 3, 3)));
        assertEquals(2, jdbc.queryForObject("select count(*) from academic_year where school_id = ?", Integer.class, school.id()));
    }

    @Test
    void theSeeder_leavesAYearAUserMade_andItsClasses_alone() {
        // the latest year is the user's own ("2025–2026" with an en dash): nothing is created or moved
        jdbc.update("update academic_year set title = '2025–2026', start_date = '2025-09-01', end_date = '2026-05-29' where id = ?",
                yearId());
        assertTrue(yearSeeder.ensure(school.school, TODAY).isEmpty());
        assertEquals(1, jdbc.queryForObject("select count(*) from academic_year where school_id = ?", Integer.class, school.id()));
        assertEquals(2, jdbc.queryForObject("select count(*) from school_class where academic_year_id = ?", Integer.class, yearId()));
    }

    @Test
    void theStatus_followsTheDates_theFlagOnlyPicksBetweenTwo() {
        seededPastYear();
        // the old year is flagged "Joriy" but ended in May: it is FINISHED, the year of today is CURRENT
        jdbc.update("update academic_year set is_current = true where id = ?", yearId());
        long now = addYear("2026–2027", "2026-09-02", "2027-05-31", false);
        long next = addYear("2027–2028", "2027-09-02", "2028-05-25", false);
        Map<Long, ListPagesService.YearCard> cards = pages.years(school.id()).stream()
                .collect(java.util.stream.Collectors.toMap(ListPagesService.YearCard::id, c -> c));
        assertEquals("FINISHED", cards.get(yearId()).status());
        assertEquals("CURRENT", cards.get(now).status());
        assertEquals("PLANNED", cards.get(next).status());
        assertEquals(java.time.temporal.ChronoUnit.DAYS.between(TODAY, LocalDate.of(2027, 9, 2)), cards.get(next).daysToStart());
        assertNull(pages.yearsPage(school.id()).missing());
        assertNull(home.signals(school.id(), null, false).stream().filter(s -> s.kind().equals("NO_CURRENT_YEAR"))
                .findFirst().orElse(null));
    }

    @Test
    void theYearStats_comeFromTheRecordsDatedInsideIt() {
        seededPastYear();
        long now = addYear("2026–2027", "2026-09-02", "2027-05-31", false);
        // the classes stay in the old year; their records of October 2026 count for 2026–2027
        long lesson = school.lessons.get(0)[0].getId();
        long student = school.students.get(0).get(0).getId();
        long other = school.students.get(0).get(1).getId();
        jdbc.update("insert into attendance (record_date, status, lesson_slot_id, student_id, source) values ('2026-10-19', 'PRESENT', ?, ?, 'MANUAL')", lesson, student);
        jdbc.update("insert into attendance (record_date, status, lesson_slot_id, student_id, source) values ('2026-10-19', 'ABSENT', ?, ?, 'MANUAL')", lesson, other);
        jdbc.update("insert into grade (grade_date, score, type, student_id, subject_id, source) values ('2026-10-19', 5, 'CURRENT', ?, ?, 'MANUAL')", student, school.subject.getId());
        jdbc.update("insert into grade (grade_date, score, type, student_id, subject_id, source) values ('2026-10-19', 3, 'CURRENT', ?, ?, 'MANUAL')", other, school.subject.getId());
        Map<Long, ListPagesService.YearCard> cards = pages.years(school.id()).stream()
                .collect(java.util.stream.Collectors.toMap(ListPagesService.YearCard::id, c -> c));
        ListPagesService.YearCard cur = cards.get(now);
        assertTrue(cur.hasData());
        assertEquals(50.0, cur.attendance());
        assertEquals(50.0, cur.quality());
        assertEquals(100.0, cur.mastery());
        assertEquals(1, cur.attendanceRun().size());
        assertEquals(1, cur.classes()); // the class its records name
        // the old year has nothing dated inside it: no numbers, "Bu yil bo'yicha ma'lumot yo'q"
        assertFalse(cards.get(yearId()).hasData());
        assertNull(cards.get(yearId()).attendance());
    }

    @Test
    void noYearOfToday_thePageAndTheDashboardSaySo_andOffer2026_2027() {
        seededPastYear();
        ListPagesService.YearsPage page = pages.yearsPage(school.id());
        assertNotNull(page.missing());
        assertEquals("2026–2027", page.missing().title());
        assertEquals(LocalDate.of(2026, 9, 2), page.missing().startDate());
        assertEquals(LocalDate.of(2027, 5, 31), page.missing().endDate());
        assertEquals(7, page.missing().terms().size());
        HomeDashboardService.Signal s = home.signals(school.id(), null, false).get(0);
        assertEquals("NO_CURRENT_YEAR", s.kind());
        assertEquals("Joriy o'quv yili kiritilmagan", s.title());
        // in the summer no year holds today and none is missing
        clock.set(LocalDate.of(2026, 7, 15).atTime(10, 0));
        assertNull(pages.yearsPage(school.id()).missing());
    }

    // ---------------------------------------------------------------- buildings

    @Test
    void aBuildingsOccupancy_isItsBusySlotsOfTheWeek_andBusyNowIsApart() {
        // the school's week: 3 periods on Monday (TestSchool). Room 301 has them all; a second room none;
        // a third is under repair and counts in neither
        Room free = new Room();
        free.setBuilding(school.room.getBuilding());
        free.setRoomNumber("302");
        free.setCapacity(25);
        rooms.save(free);
        Room repair = new Room();
        repair.setBuilding(school.room.getBuilding());
        repair.setRoomNumber("303");
        repair.setCapacity(20);
        rooms.save(repair);
        jdbc.update("update room set status = 'REPAIR' where id = ?", repair.getId());
        long buildingId = school.room.getBuilding().getId();

        // Tuesday 10:00 — no lesson now, the week is half busy
        BuildingStatsService.Stats st = buildingStats.stats(List.of(buildingId)).get(buildingId);
        assertEquals(50.0, st.weeklyOccupancy());
        assertEquals(3, st.busySlots());
        assertEquals(6, st.totalSlots());
        assertEquals(0, st.busyNow());
        assertEquals(2, st.activeRooms());
        assertEquals(1, st.repairRooms());
        assertEquals(75, st.capacity());
        assertEquals(Map.of("CLASSROOM", 3), st.roomTypes());
        // the list says the weekly share, not "0% band"
        assertEquals(50.0, buildingService.getBuildingById(buildingId).getOccupiedPercentage());

        // Monday 08:40, the first lesson in 301: one of two working rooms is busy now
        clock.set(LocalDate.of(2026, 10, 19).atTime(8, 40));
        st = buildingStats.stats(List.of(buildingId)).get(buildingId);
        assertEquals(1, st.busyNow());
        assertEquals(50.0, st.weeklyOccupancy());
        List<String> states = st.floors().stream().flatMap(f -> f.rooms().stream()).map(BuildingStatsService.RoomCell::status).toList();
        assertEquals(List.of("BUSY", "FREE", "REPAIR"), states);
    }

    // ---------------------------------------------------------------- terms: 409

    @Test
    void overlappingTerms_are409_overHttp_andNothingIsSaved() throws Exception {
        seededPastYear();
        long now = addYear("2026–2027", "2026-09-02", "2027-05-31", false);
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        String password = "pw-" + n;
        User admin = new User();
        admin.setUsername("crudfix_admin_" + n);
        admin.setPassword(encoder.encode(password));
        admin.setRole(Role.ADMIN);
        admin = users.save(admin);
        try {
            ApiClient api = new ApiClient(port).login(admin.getUsername(), password);
            ApiClient.Response r = api.put("/api/academic-years/" + now + "/terms", List.of(
                    Map.of("name", "1-chorak", "kind", "QUARTER", "startDate", "2026-09-02", "endDate", "2026-11-05"),
                    Map.of("name", "Kuzgi ta'til", "kind", "VACATION", "startDate", "2026-11-03", "endDate", "2026-11-09")));
            assertEquals(409, r.status(), r.body());
            assertTrue(r.body().contains("ustma-ust"), r.body());
            assertEquals(0, jdbc.queryForObject("select count(*) from academic_term where academic_year_id = ?", Integer.class, now));
            // the year page over HTTP: 2026–2027 is CURRENT
            ApiClient.Response page = api.get("/api/pages/lists/years-page?schoolId=" + school.id());
            assertEquals(200, page.status(), page.body());
            assertEquals("CURRENT", page.json().get("years").get(0).get("status").asText());
        } finally {
            users.delete(admin);
        }
    }
}
