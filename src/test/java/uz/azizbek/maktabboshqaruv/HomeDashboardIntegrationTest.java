package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.service.dashboard.TeacherDashboardService;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The home page's numbers in a school of its own (Monday 2031-03-03, 10:40): only classes that
 * took attendance count, "no data" is null not 0, the streak list skips Sundays, mastery /
 * quality use 3–5 / 4–5, automatic marks do not count for teachers, the class teacher is the
 * class's own, the home page follows the role, and another school never leaks in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.attendance-simulator.enabled=false"
})
@ActiveProfiles("local")
@Import(AttendanceAutomationTestBase.FakeClock.class)
@RecordApplicationEvents
class HomeDashboardIntegrationTest extends AttendanceAutomationTestBase {

    @Autowired
    HomeDashboardService home;
    @Autowired
    TeacherDashboardService teacherHome;
    @Autowired
    DashboardCache cache;

    private final List<TestSchool> others = new ArrayList<>();
    private final List<String> users = new ArrayList<>();

    @AfterEach
    void cleanUpMore() {
        users.forEach(u -> jdbc.update("delete from app_user where username = ?", u));
        others.forEach(TestSchool::delete);
        cache.clear();
    }

    /** A mark written straight to the table (any date — the queries do not care about the weekday). */
    void put(int cls, int lessonIdx, int studentIdx, LocalDate date, AttendanceStatus status, String source) {
        jdbc.update("insert into attendance (lesson_slot_id, student_id, record_date, status, source, created_date) values (?, ?, ?, ?, ?, now())",
                lesson(cls, lessonIdx).getId(), student(cls, studentIdx).getId(), Date.valueOf(date), status.name(), source);
    }

    HomeDashboardService.Kpi kpi(String key) {
        return home.kpis(school.id()).stream().filter(k -> k.key().equals(key)).findFirst().orElseThrow();
    }

    @Test
    void todaysRate_onlyFromClassesThatTookIt_andNullWhenNoneDid() {
        at(10, 40);
        assertNull(kpi("attendance").value(), "nothing taken yet: no number, not 0%");

        // class 0 took it: 5 present, 1 absent; class 1 took nothing
        manual(0, 0, AttendanceStatus.PRESENT, 0, 1, 2, 3, 4);
        manual(0, 0, AttendanceStatus.ABSENT, 5);
        HomeDashboardService.Kpi k = kpi("attendance");
        assertEquals(83.3, k.value(), "5 of class 0's 6 — class 1 does not pull it down");
        assertEquals(1, k.extra().get("classesTaken"));
        assertEquals(2L, ((Number) k.extra().get("classesTotal")).longValue());

        HomeDashboardService.TodayMap map = home.todayMap(school.id());
        assertEquals(83.3, map.rate());
        assertEquals(6, map.ring().get("unmarked"), "class 1's six students are not marked yet");
        assertNull(map.classes().get(1).rate());
    }

    @Test
    void absentStreak_countsSchoolDays_sundayIsNoBreak() {
        at(10, 40);
        // Thursday, Friday, Saturday absent; Sunday no school; Monday (today) not marked yet
        for (LocalDate d : List.of(LocalDate.of(2031, 2, 27), LocalDate.of(2031, 2, 28), LocalDate.of(2031, 3, 1))) {
            put(0, 0, 0, d, AttendanceStatus.ABSENT, "MANUAL");
            put(0, 0, 1, d, d.getDayOfMonth() == 28 ? AttendanceStatus.PRESENT : AttendanceStatus.ABSENT, "MANUAL");
        }
        var streak = home.attention(school.id(), null, true).stream()
                .filter(g -> g.kind().equals("ABSENT_STREAK")).findFirst().orElseThrow();
        assertEquals(1, streak.total(), "student 1 came on Friday");
        assertEquals("3 kun", streak.items().get(0).detail());
        assertEquals(student(0, 0).getId(), ((Number) streak.items().get(0).ref().get("studentId")).longValue());
    }

    @Test
    void masteryAndQuality_areThreeToFiveAndFourToFive() {
        at(10, 40);
        int[] scores = {5, 4, 4, 3, 2}; // mastery 4/5 = 80 %, quality 3/5 = 60 %
        for (int i = 0; i < scores.length; i++) {
            jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, ?, 'CURRENT', now())",
                    student(0, i).getId(), school.subject.getId(), Date.valueOf(LocalDate.of(2031, 3, 3)), scores[i]);
        }
        assertEquals(80.0, kpi("mastery").value());
        assertEquals(60.0, kpi("quality").value());
        HomeDashboardService.SubjectRow s = home.subjectQuality(school.id()).get(0);
        assertEquals(80.0, s.mastery());
        assertEquals(60.0, s.quality());
    }

    @Test
    void teacherDiscipline_countsOnlyManualAndDevice() {
        at(10, 40); // lessons 1 and 2 of today are over
        put(0, 0, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "AUTO_CARRY");
        put(1, 0, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "DEMO_SIM");
        put(0, 1, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "MANUAL");
        put(1, 1, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "DEVICE");
        HomeDashboardService.TeacherRow t = home.teacherDiscipline(school.id(), null).get(0);
        // the fixture has Monday lessons only: last Monday's 6 lessons + today's 4 finished ones
        assertEquals(10, t.lessons());
        assertEquals(2, t.attended(), "the AUTO_CARRY and DEMO_SIM lessons are not 'taken'");
    }

    @Test
    void ranking_showsEachClassWithItsOwnClassTeacher() {
        at(10, 40);
        List<HomeDashboardService.RankingRow> rows = home.classRanking(school.id());
        assertEquals(2, rows.size());
        assertEquals("Test O'qituvchi", rows.get(0).teacher(), "class 0 is led by the fixture's teacher");
        assertNull(rows.get(1).teacher(), "class 1 has no class teacher — nobody else's name appears");
    }

    @Test
    void homePage_followsTheRole() throws Exception {
        String editor = "dash_editor_" + school.id();
        String viewer = "dash_viewer_" + school.id();
        jdbc.update("insert into app_user (username, password, role, employee_id) values (?, 'x', 'EDITOR', ?)", editor, school.teacher.getId());
        jdbc.update("insert into app_user (username, password, role) values (?, 'x', 'VIEWER')", viewer);
        users.add(viewer);

        assertEquals("ADMIN", as("admin", "ROLE_ADMIN", () -> teacherHome.home(school.id())).kind());
        TeacherDashboardService.Home t = as(editor, "ROLE_EDITOR", () -> teacherHome.home(school.id()));
        assertEquals("TEACHER", t.kind());
        assertEquals(school.classes.get(0).getId(), t.ledClassId());
        assertEquals(2, t.classIds().size());
        assertEquals("VIEWER", as(viewer, "ROLE_VIEWER", () -> teacherHome.home(school.id())).kind());

        at(10, 40);
        List<TeacherDashboardService.MyLesson> mine = as(editor, "ROLE_EDITOR", () -> teacherHome.myLessons(school.id(), school.teacher.getId()));
        assertEquals(6, mine.size(), "three lessons in each of the two classes");
        assertEquals("NONE", mine.get(0).state());
    }

    @Test
    void anotherSchool_neverLeaksIn() {
        TestSchool other = TestSchool.create(new TestSchool.Repos(schools, years, positions, employees, subjects, buildings, rooms,
                classes, lessons, students), jdbc, 1, 4);
        others.add(other);
        at(10, 40);
        jdbc.update("insert into attendance (lesson_slot_id, student_id, record_date, status, source, created_date) values (?, ?, ?, 'ABSENT', 'MANUAL', now())",
                other.lessons.get(0)[0].getId(), other.students.get(0).get(0).getId(), Date.valueOf(LocalDate.of(2031, 3, 3)));
        assertNull(kpi("attendance").value(), "the other school's mark is not ours");
        assertEquals(2, home.todayMap(school.id()).classes().size());
        assertEquals(1, home.todayMap(other.id()).classes().size());
        assertEquals(0.0, home.todayMap(other.id()).rate());
    }

    private <T> T as(String username, String role, java.util.concurrent.Callable<T> action) throws Exception {
        var ctx = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var before = ctx.getAuthentication();
        ctx.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                username, null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(role))));
        try {
            return action.call();
        } finally {
            ctx.setAuthentication(before);
        }
    }
}
