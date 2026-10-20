package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import uz.azizbek.maktabboshqaruv.controller.AttendanceController;
import uz.azizbek.maktabboshqaruv.controller.GradeController;
import uz.azizbek.maktabboshqaruv.controller.JournalController;
import uz.azizbek.maktabboshqaruv.controller.LessonSlotController;
import uz.azizbek.maktabboshqaruv.controller.SchoolClassController;
import uz.azizbek.maktabboshqaruv.controller.SubjectController;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Not a check: writes what the attendance and journal pages' endpoints answer for school 1 of the
 * TEST database into one JSON file ("path" or "path?query" → body), so the UI tests can show the
 * pages with real numbers and no sign-in (tests/ui/aj-mock.js serves it). Refuses any database but
 * maktab_tg_test. Runs only when AJ_FIXTURES names the file:
 *   DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test AJ_FIXTURES=/path/aj.json \
 *     ./gradlew test --tests '*AttendanceJournalFixtureDump'
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "AJ_FIXTURES", matches = ".+")
class AttendanceJournalFixtureDump {

    static final long SCHOOL = 1;

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    SchoolClassController classes;
    @Autowired
    AttendanceController attendance;
    @Autowired
    LessonSlotController lessons;
    @Autowired
    JournalController journal;
    @Autowired
    GradeController grades;
    @Autowired
    SubjectController subjects;
    @Autowired
    uz.azizbek.maktabboshqaruv.controller.AttendanceJournalController pages;

    static Map<String, Object> page(Page<?> p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content", p.getContent());
        m.put("totalElements", p.getTotalElements());
        m.put("totalPages", p.getTotalPages());
        return m;
    }

    @Test
    void dump() throws Exception {
        String db = jdbc.queryForObject("select current_database()", String.class);
        if (!"maktab_tg_test".equals(db)) {
            throw new IllegalStateException("Faqat maktab_tg_test da ishlaydi (DB_URL), hozir: " + db);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        Map<String, Object> out = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        out.put("today", today.toString());

        long classA = jdbc.queryForObject("""
                select c.id from school_class c join academic_year y on y.id = c.academic_year_id
                 where y.school_id = 1 and c.grade_number = 1 and c.section_letter = 'A'""", Long.class);
        long classB = jdbc.queryForObject("""
                select c.id from school_class c join academic_year y on y.id = c.academic_year_id
                 where y.school_id = 1 and c.grade_number = 1 and c.section_letter = 'B'""", Long.class);
        out.put("classA", classA);
        out.put("classB", classB);

        out.put("/api/school-classes", page(classes.getAllSchoolClasses(SCHOOL,
                PageRequest.of(0, 300, Sort.by("gradeNumber")))));
        out.put("/api/subjects", page(subjects.getAllSubjects(SCHOOL, false, null, null, PageRequest.of(0, 500, Sort.by("name")))));
        out.put("/api/attendance/today-lessons", attendance.getTodayLessons(SCHOOL, null));

        // the class's whole week (the old page lists every weekday) and a roster per lesson of the class
        var week = lessons.getTimetable(SCHOOL, classB, null, null);
        out.put("/api/lesson-slots/timetable", week);
        for (var l : week) {
            out.put("/api/attendance/roster?lessonSlotId=" + l.getLessonSlotId(),
                    attendance.getRoster(l.getLessonSlotId(), today));
        }

        // journal: class 1-A, its first subject this month
        var ov = journal.overview(classA);
        out.put("/api/classes/" + classA + "/overview", ov);
        out.put("/api/classes/" + classB + "/overview", journal.overview(classB));
        long subject = ov.subjects().get(0).id();
        out.put("subjectA", subject);
        out.put("/api/grades/journal", grades.journal(classA, subject, today.toString().substring(0, 7)));

        // the new pages: the class's day (today and the school day before), rosters of today's lessons
        String m = today.toString().substring(0, 7);
        LocalDate before = today.minusDays(today.getDayOfWeek() == java.time.DayOfWeek.MONDAY ? 2 : 1);
        for (long cid : List.of(classA, classB)) {
            for (LocalDate d : List.of(today, before)) {
                var day = pages.day(SCHOOL, cid, d);
                out.put("/api/pages/attendance/day?classId=" + cid + "&date=" + d, day);
                for (var l : day.lessons()) {
                    out.put("/api/pages/attendance/roster?lessonSlotId=" + l.lessonSlotId() + "&date=" + d,
                            pages.roster(l.lessonSlotId(), d));
                }
            }
        }
        for (var sub : ov.subjects()) {
            out.put("/api/grades/journal?subjectId=" + sub.id(), grades.journal(classA, sub.id(), m));
            out.put("/api/pages/journal/summary?subjectId=" + sub.id(), pages.summary(classA, sub.id(), m));
            out.put("/api/pages/journal/day-attendance?subjectId=" + sub.id(), pages.dayAttendance(classA, sub.id(), today));
        }
        var firstStudent = grades.journal(classA, subject, m).students().get(0).id();
        out.put("/api/pages/journal/student", pages.student(firstStudent, subject));

        Path file = Path.of(System.getenv("AJ_FIXTURES"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
    }
}
