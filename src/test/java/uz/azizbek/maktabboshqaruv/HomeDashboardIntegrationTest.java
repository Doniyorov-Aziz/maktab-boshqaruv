package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.service.SchoolQuarters;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardMetrics;
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
        "app.demo.enabled=false"
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
    @Autowired
    uz.azizbek.maktabboshqaruv.service.parent.ParentDataService parentData;
    @Autowired
    uz.azizbek.maktabboshqaruv.service.pages.CalendarPageService calendarPage;

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
        return home.kpis(school.id(), "quarter").stream().filter(k -> k.key().equals(key)).findFirst().orElseThrow();
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
        HomeDashboardService.SubjectRow s = home.subjectQuality(school.id(), "quarter").get(0);
        assertEquals(80.0, s.mastery());
        assertEquals(60.0, s.quality());
    }

    @Test
    void inAVacation_theQuarterIsTheSchoolsLastTerm_notTheCalendarOne() {
        at(10, 40); // Monday 2031-03-03 — a vacation in this school's terms
        long yearId = school.classes.get(0).getAcademicYear().getId();
        String term = "insert into academic_term (academic_year_id, name, kind, start_date, end_date) values (?, ?, ?, ?, ?)";
        try {
            jdbc.update(term, yearId, "1-chorak", "QUARTER", Date.valueOf("2030-11-10"), Date.valueOf("2030-12-28"));
            jdbc.update(term, yearId, "2-chorak", "QUARTER", Date.valueOf("2031-01-12"), Date.valueOf("2031-02-25"));
            jdbc.update(term, yearId, "Ta'til", "VACATION", Date.valueOf("2031-02-26"), Date.valueOf("2031-03-08"));
            String grade = "insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, ?, 'CURRENT', now())";
            // 2031-01-05 is in the calendar's 3rd quarter (from Jan 1) but in this school's winter break
            jdbc.update(grade, student(0, 0).getId(), school.subject.getId(), Date.valueOf("2031-01-05"), 2);
            jdbc.update(grade, student(0, 1).getId(), school.subject.getId(), Date.valueOf("2031-02-20"), 5);

            SchoolQuarters.Quarter q = home.currentQuarter(school.id());
            assertEquals(new SchoolQuarters.Quarter(2, LocalDate.of(2031, 1, 12), LocalDate.of(2031, 2, 25)), q);
            assertEquals(100.0, kpi("quality").value(), "only the grade inside the school's quarter counts");
            assertEquals(2, kpi("quality").extra().get("quarter"));
            HomeDashboardService.Bubble b = home.classMap(school.id()).classes().get(0);
            assertEquals(100.0, b.quality(), "the class map is not blank in the vacation");
            assertEquals(1, b.grades());

            // the parents' bot: the default quarter and the dates of "2-chorak" are the school's
            var child = student(0, 0);
            assertEquals(2, parentData.currentQuarter(child));
            assertArrayEquals(new LocalDate[]{LocalDate.of(2031, 1, 12), LocalDate.of(2031, 2, 25)},
                    parentData.periodRange(child, "quarter", "2"));
            assertArrayEquals(new LocalDate[]{LocalDate.of(2030, 11, 10), LocalDate.of(2030, 12, 28)},
                    parentData.periodRange(child, "quarter", "1"));

            // the calendar's year line: the school's quarters and vacation; today is in the vacation
            var cal = calendarPage.overview(school.id());
            assertEquals(List.of("QUARTER 1-chorak 2030-11-10", "QUARTER 2-chorak 2031-01-12", "VACATION Ta'til 2031-02-26"),
                    cal.segments().stream().map(x -> x.kind() + " " + x.label() + " " + x.from()).toList());
            assertNull(cal.quarter(), "no quarter runs in the vacation");
            assertTrue(cal.onVacation());
        } finally {
            jdbc.update("delete from academic_term where academic_year_id = ?", yearId);
        }
    }

    @Test
    void parentsBot_schoolYearIsTheClasssAcademicYear() {
        at(10, 40); // 2031-03-03; the fixture's year is 2030-09-01 – 2031-06-30
        var child = student(0, 0);
        assertArrayEquals(new LocalDate[]{LocalDate.of(2030, 9, 1), LocalDate.of(2031, 6, 30)}, parentData.schoolYear(child));
        assertArrayEquals(parentData.schoolYear(child), parentData.periodRange(child, "year", null));

        // a year that is long over (or not started) falls back to the calendar school year around today
        long yearId = school.classes.get(0).getAcademicYear().getId();
        jdbc.update("update academic_year set start_date = ?, end_date = ? where id = ?",
                Date.valueOf("2029-09-01"), Date.valueOf("2030-05-31"), yearId);
        assertArrayEquals(new LocalDate[]{LocalDate.of(2030, 9, 1), LocalDate.of(2031, 8, 31)}, parentData.schoolYear(child));
    }

    @Test
    void gradeDistribution_skipsVacationWeeks_andLabelsThemWithDates() {
        at(10, 40); // Monday 2031-03-03
        String grade = "insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, ?, 'CURRENT', now())";
        jdbc.update(grade, student(0, 0).getId(), school.subject.getId(), Date.valueOf("2031-02-20"), 5);
        jdbc.update(grade, student(0, 1).getId(), school.subject.getId(), Date.valueOf("2031-02-21"), 4);
        jdbc.update(grade, student(0, 2).getId(), school.subject.getId(), Date.valueOf("2031-02-12"), 2);

        // an ordinary week: this week and last week, both empty here
        List<HomeDashboardService.GradeWeek> plain = home.gradeDistribution(school.id());
        assertEquals(List.of("Shu hafta", "O'tgan hafta"), plain.stream().map(HomeDashboardService.GradeWeek::label).toList());
        assertEquals(0, plain.get(0).total() + plain.get(1).total());

        // a vacation from Feb 24 to Mar 8: this week and last week have no lessons — the two weeks before them show
        jdbc.update("insert into calendar_event (school_id, title, type, start_date, end_date, created_date, seeded, notify_parents) "
                + "values (?, 'Ta''til', 'VACATION', ?, ?, now(), false, false)", school.id(), Date.valueOf("2031-02-24"), Date.valueOf("2031-03-08"));
        List<HomeDashboardService.GradeWeek> w = home.gradeDistribution(school.id());
        assertEquals(List.of("17-fev – 23-fev", "10-fev – 16-fev"), w.stream().map(HomeDashboardService.GradeWeek::label).toList());
        assertEquals(LocalDate.of(2031, 2, 17), w.get(0).from());
        assertEquals(LocalDate.of(2031, 2, 23), w.get(0).to());
        assertEquals(2, w.get(0).total());
        assertEquals(1, w.get(1).total());
        assertEquals(1, w.get(1).counts().get(2));
    }

    @Test
    void teacherDiscipline_countsOnlyManualAndDevice() {
        at(10, 40); // lessons 1 and 2 of today are over
        put(0, 0, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "AUTO_CARRY");
        put(1, 0, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "DEMO_SIM");
        put(0, 1, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "MANUAL");
        put(1, 1, 0, LocalDate.of(2031, 3, 3), AttendanceStatus.PRESENT, "DEVICE");
        HomeDashboardService.TeacherRow t = home.journal(school.id()).teachers().get(0);
        // the fixture has Monday lessons only: last Monday's 6 lessons + today's 4 finished ones
        assertEquals(10, t.lessons());
        assertEquals(2, t.attended(), "the AUTO_CARRY and DEMO_SIM lessons are not 'taken'");
    }

    @Test
    void classMap_showsEachClassWithItsOwnClassTeacher_andHidesClassesWithoutStudents() {
        at(10, 40);
        // an empty class of the same school (an old year's leftover): on no widget
        jdbc.update("insert into school_class (grade_number, section_letter, max_students, academic_year_id) values (11, 'A', 30, ?)",
                school.classes.get(0).getAcademicYear().getId());
        List<HomeDashboardService.Bubble> rows = home.classMap(school.id()).classes();
        assertEquals(2, rows.size());
        assertEquals("Test O'qituvchi", rows.get(0).teacher(), "class 0 is led by the fixture's teacher");
        assertNull(rows.get(1).teacher(), "class 1 has no class teacher — nobody else's name appears");
        assertEquals(2, home.todayMap(school.id()).classes().size());
    }

    @Test
    void excusedAbsence_isLeftOutOfTheRate_andAHalfMarkedClassIsPartial() {
        at(10, 40);
        manual(0, 0, AttendanceStatus.PRESENT, 0, 1, 2, 3);
        manual(0, 0, AttendanceStatus.ABSENT, 4);
        manual(0, 0, AttendanceStatus.EXCUSED, 5);
        manual(1, 0, AttendanceStatus.PRESENT, 0, 1);
        HomeDashboardService.TodayMap map = home.todayMap(school.id());
        assertEquals(80.0, map.classes().get(0).rate(), "4 / (4 + 1): the excused student is in neither side");
        assertEquals("FULL", map.classes().get(0).state());
        assertEquals("PARTIAL", map.classes().get(1).state(), "2 of 6 marked: under half");
        assertEquals(85.7, kpi("attendance").value(), "6 present of 7 counted (one excused left out)");
    }

    @Test
    void riskList_ranksAbsenceAndFallingGrades_fromForty() {
        at(10, 40);
        // student 0: absent 6 school days in the last 30 (35) and a 3 and a 2 lately (30 + 4)
        LocalDate d = LocalDate.of(2031, 3, 1);
        for (int k = 0, n = 0; n < 6; k++) {
            LocalDate day = d.minusDays(k);
            if (day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) continue;
            put(0, 0, 0, day, AttendanceStatus.ABSENT, "MANUAL");
            n++;
        }
        grade(0, 0, LocalDate.of(2031, 2, 25), 3);
        grade(0, 0, LocalDate.of(2031, 2, 27), 2);
        // student 1: 5, 5 four weeks ago, 3, 2 in the last two weeks
        grade(0, 1, LocalDate.of(2031, 2, 3), 5);
        grade(0, 1, LocalDate.of(2031, 2, 5), 5);
        grade(0, 1, LocalDate.of(2031, 2, 24), 3);
        grade(0, 1, LocalDate.of(2031, 2, 26), 2);
        // student 2: absent 6 days only — 35, under the list's 40
        for (int k = 0, n = 0; n < 6; k++) {
            LocalDate day = d.minusDays(k);
            if (day.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) continue;
            put(0, 0, 2, day, AttendanceStatus.ABSENT, "MANUAL");
            n++;
        }
        HomeDashboardService.RiskList list = home.riskStudents(school.id(), null, 6);
        assertEquals(2, list.total(), "student 2 (35) is not on the list");
        HomeDashboardService.RiskRow first = list.rows().get(0);
        assertEquals(student(0, 0).getId(), first.studentId());
        assertEquals(DashboardMetrics.risk(6, 2.5, null, 1).score(), first.score());
        assertEquals(6, first.absentDays());
        HomeDashboardService.RiskRow second = list.rows().get(1);
        assertEquals(student(0, 1).getId(), second.studentId());
        assertEquals(2.5, second.avgNow());
        assertEquals(5.0, second.avgBefore());
        assertEquals(DashboardMetrics.risk(0, 2.5, 5.0, 1).score(), second.score());
    }

    @Test
    void healthIndex_isTheWeightedSumOfItsParts() {
        at(10, 40);
        manual(0, 0, AttendanceStatus.PRESENT, 0, 1, 2, 3, 4);
        manual(0, 0, AttendanceStatus.ABSENT, 5);
        grade(0, 0, LocalDate.of(2031, 3, 3), 5);
        grade(0, 1, LocalDate.of(2031, 3, 3), 3);
        HomeDashboardService.HealthCard h = home.health(school.id());
        assertEquals(83.3, h.parts().get("attendance"));
        assertEquals(50.0, h.parts().get("quality"));
        assertEquals(0.0, h.parts().get("telegram"), "no parent joined the bot");
        DashboardMetrics.Health expected = DashboardMetrics.health(83.3, 50.0, h.parts().get("journal"), 0.0,
                DashboardMetrics.Weights.DEFAULT);
        assertEquals(expected.index(), h.index());
    }

    void grade(int cls, int studentIdx, LocalDate date, int score) {
        jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, ?, 'CURRENT', now())",
                student(cls, studentIdx).getId(), school.subject.getId(), Date.valueOf(date), score);
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
