package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import uz.azizbek.maktabboshqaruv.dto.GradeRequestDto;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.GradeType;
import uz.azizbek.maktabboshqaruv.event.GradeSavedEvent;
import uz.azizbek.maktabboshqaruv.service.GradeService;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoAttendancePlan;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The demo grade simulator: grades only after a lesson is over and only for students who came,
 * never over or next to a staff grade, never on a Sunday or ahead of today, the same grades on a
 * re-run, nothing to parents, a staff edit makes the grade MANUAL; and the few teachers the
 * simulator lets "forget" lessons show up in the journal numbers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=true"
})
@ActiveProfiles("local")
@Import(AttendanceAutomationTestBase.FakeClock.class)
@RecordApplicationEvents
class DemoGradeSimulatorIntegrationTest extends AttendanceAutomationTestBase {

    @Autowired
    GradeService grades;
    @Autowired
    HomeDashboardService home;

    private String classesSql() {
        return "select c.id from school_class c join academic_year y on y.id = c.academic_year_id where y.school_id = " + school.id();
    }

    int demoGrades() {
        return jdbc.queryForObject("select count(*) from grade where source = 'DEMO_SIM' and student_id in "
                + "(select id from students where class_id in (" + classesSql() + "))", Integer.class);
    }

    int gradesOf(int cls, int i) {
        return jdbc.queryForObject("select count(*) from grade where student_id = ?", Integer.class, student(cls, i).getId());
    }

    @Test
    void grades_comeAfterTheLesson_onlyForStudentsWhoCame() {
        at(8, 45);
        manual(0, 0, AttendanceStatus.ABSENT, 0); // student 0 is away all day
        automation.runFor(school.id());
        assertEquals(0, demoGrades(), "lesson 1 is not over yet");

        at(12, 0); // all three lessons are over
        automation.runFor(school.id());
        assertTrue(demoGrades() > 0, "30–40 % of the present students got a grade");
        assertEquals(0, gradesOf(0, 0), "an absent student gets no grade");
        // every demo grade belongs to a student marked present or late in that very lesson
        Integer orphan = jdbc.queryForObject("""
                select count(*) from grade g where g.source = 'DEMO_SIM' and g.lesson_slot_id in (?, ?, ?, ?, ?, ?)
                   and not exists (select 1 from attendance a where a.lesson_slot_id = g.lesson_slot_id
                                     and a.student_id = g.student_id and a.record_date = g.grade_date
                                     and a.status in ('PRESENT', 'LATE'))""", Integer.class,
                lesson(0, 0).getId(), lesson(0, 1).getId(), lesson(0, 2).getId(),
                lesson(1, 0).getId(), lesson(1, 1).getId(), lesson(1, 2).getId());
        assertEquals(0, orphan);
        assertEquals("O'qituvchi T.", jdbc.queryForObject("select max(created_by) from grade where source = 'DEMO_SIM' and student_id in "
                + "(select id from students where class_id in (" + classesSql() + "))", String.class));
    }

    @Test
    void staffGrade_isNeverChangedNorDoubled_andAnEditMakesADemoGradeManual() {
        at(12, 0);
        // a teacher already graded every student of class 0 in this subject today
        for (int i = 0; i < 6; i++) {
            jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date, created_by) "
                            + "values (?, ?, ?, 4, 'CURRENT', now(), 'Ustoz')",
                    student(0, i).getId(), school.subject.getId(), Date.valueOf(MONDAY));
        }
        automation.runFor(school.id());
        for (int i = 0; i < 6; i++) {
            assertEquals(1, gradesOf(0, i), "the staff grade stays alone");
        }
        assertEquals(6, jdbc.queryForObject("select count(*) from grade where source = 'MANUAL' and score = 4 and student_id in "
                + "(select id from students where class_id = ?)", Integer.class, school.classes.get(0).getId()));

        // a staff member corrects a demo grade: from now on it is MANUAL and the simulator leaves it
        Map<String, Object> demo = jdbc.queryForMap("select id, student_id, score from grade where source = 'DEMO_SIM' and student_id in "
                + "(select id from students where class_id = ?) order by id limit 1", school.classes.get(1).getId());
        GradeRequestDto edit = new GradeRequestDto();
        edit.setStudentId(((Number) demo.get("student_id")).longValue());
        edit.setSubjectId(school.subject.getId());
        edit.setGradeDate(MONDAY);
        edit.setScore(((Number) demo.get("score")).intValue() == 5 ? 4 : 5);
        edit.setType(GradeType.CURRENT);
        grades.updateGrade(((Number) demo.get("id")).longValue(), edit);
        automation.runFor(school.id());
        assertEquals("MANUAL", jdbc.queryForObject("select source from grade where id = ?", String.class, demo.get("id")));
        assertEquals(edit.getScore(), jdbc.queryForObject("select score from grade where id = ?", Integer.class, demo.get("id")));
    }

    @Test
    void grades_areIdempotentAndDeterministic() {
        at(12, 0);
        automation.runFor(school.id());
        int first = demoGrades();
        String snapshot = jdbc.queryForObject("select string_agg(student_id || ':' || lesson_slot_id || ':' || score || ':' || type, ',' "
                + "order by student_id, lesson_slot_id) from grade where source = 'DEMO_SIM' and lesson_slot_id = ?", String.class,
                lesson(1, 2).getId());
        assertEquals(0, automation.runFor(school.id()).grades(), "a second run adds nothing");
        assertEquals(first, demoGrades());
        automation.deleteDemoGrades(school.id());
        assertEquals(0, demoGrades());
        automation.runFor(school.id());
        assertEquals(first, demoGrades());
        assertEquals(snapshot, jdbc.queryForObject("select string_agg(student_id || ':' || lesson_slot_id || ':' || score || ':' || type, ',' "
                + "order by student_id, lesson_slot_id) from grade where source = 'DEMO_SIM' and lesson_slot_id = ?", String.class,
                lesson(1, 2).getId()), "the same lesson gives the same grades again");
    }

    @Test
    void noGrades_onSunday_orAheadOfToday() {
        clock.set(LocalDate.of(2031, 3, 2).atTime(12, 0)); // Sunday
        automation.runFor(school.id());
        assertEquals(0, demoGrades());

        at(12, 0);
        int[] n = automation.backfillDemo(school.id(), 20, false);
        assertNotNull(n);
        assertTrue(n[1] > 0, "past Mondays got grades");
        automation.runFor(school.id());
        assertEquals(0, jdbc.queryForObject("select count(*) from grade where source = 'DEMO_SIM' and (grade_date > ? "
                        + "or extract(isodow from grade_date) = 7) and student_id in (select id from students where class_id in (" + classesSql() + "))",
                Integer.class, Date.valueOf(MONDAY)));
    }

    @Test
    void demoGrades_neverReachParents() {
        at(12, 0);
        automation.runFor(school.id());
        Long id = jdbc.queryForObject("select id from grade where source = 'DEMO_SIM' and lesson_slot_id = ? order by id limit 1",
                Long.class, lesson(0, 0).getId());
        assertEquals(0, notifications.enqueueGrade(new GradeSavedEvent(id, true)));
        assertEquals(0, notifications.enqueueAttendance(new uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent(
                jdbc.queryForObject("select id from attendance where source = 'DEMO_SIM' and lesson_slot_id = ? limit 1", Long.class,
                        lesson(0, 0).getId()), null, AttendanceStatus.ABSENT)));
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_log where school_id = ?", Integer.class, school.id()),
                "the Telegram outbox stays empty");
    }

    @Test
    void forgottenLessons_showInTheJournal_andOnlyForTheFewSlackers() {
        // a bigger school: 2 classes × 25 students, lessons Monday–Saturday, 6 teachers (one per lesson)
        school.delete();
        school = TestSchool.create(new TestSchool.Repos(schools, years, positions, employees, subjects, buildings, rooms,
                classes, lessons, students), jdbc, 2, 25);
        List<Long> teachers = new ArrayList<>();
        for (int c = 0; c < 2; c++) {
            for (int k = 0; k < 3; k++) {
                Employee e = new Employee();
                e.setSchool(school.school);
                e.setPosition(school.teacher.getPosition());
                e.setFirstName("T" + c + k);
                e.setLastName("Ustoz");
                e.setPhone("+99890" + (1_000_000 + school.id() % 100_000 * 10 + c * 3 + k));
                long id = employees.save(e).getId();
                teachers.add(id);
                jdbc.update("update lesson_slot set employee_id = ? where id = ?", id, lesson(c, k).getId());
            }
        }
        for (String wd : List.of("Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba")) {
            jdbc.update("insert into lesson_slot (class_id, subject_id, employee_id, room_id, weekday, start_time, end_time) "
                    + "select class_id, subject_id, employee_id, room_id, ?, start_time, end_time from lesson_slot "
                    + "where class_id in (" + classesSql() + ") and weekday = 'Dushanba'", wd);
        }
        at(12, 0);
        int[] n = automation.backfillDemo(school.id(), 30, false);
        assertNotNull(n);

        List<LocalDate> days = new ArrayList<>();
        for (LocalDate d = MONDAY.minusDays(1); days.size() < 30; d = d.minusDays(1)) {
            if (d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY) days.add(0, d);
        }
        Set<Long> slackers = DemoAttendancePlan.slackers(school.id(), teachers, 2030);
        assertTrue(slackers.size() == 2 || slackers.size() == 3);

        // what the plan says each teacher skipped, lesson by lesson
        Map<Long, Integer> skipped = new HashMap<>();
        Map<Long, Integer> held = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("select id, employee_id from lesson_slot where class_id in (" + classesSql() + ")")) {
            long slot = ((Number) r.get("id")).longValue();
            long emp = ((Number) r.get("employee_id")).longValue();
            String wd = jdbc.queryForObject("select weekday from lesson_slot where id = ?", String.class, slot);
            for (LocalDate d : days) {
                if (!wd.equals(uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays.WEEKDAY_UZ.get(d.getDayOfWeek()))) continue;
                held.merge(emp, 1, Integer::sum);
                if (DemoAttendancePlan.skipped(emp, slot, d, slackers.contains(emp))) skipped.merge(emp, 1, Integer::sum);
            }
        }
        HomeDashboardService.Journal journal = home.journal(school.id(), null, days);
        int slackerSkips = 0;
        for (HomeDashboardService.TeacherRow t : journal.teachers()) {
            int s = skipped.getOrDefault(t.employeeId(), 0);
            assertEquals(held.get(t.employeeId()), t.lessons());
            assertEquals(t.lessons() - s, t.attended(), "attendance is in every lesson the teacher did not forget");
            if (slackers.contains(t.employeeId())) slackerSkips += s;
        }
        assertTrue(slackerSkips > 0, "the slackers' forgotten lessons are visible");
    }
}
