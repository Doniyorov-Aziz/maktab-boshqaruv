package uz.azizbek.maktabboshqaruv.service.dashboard;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService.pct;

/**
 * The teacher's home page (EDITOR linked to an employee who teaches): today's lessons, lessons
 * left without attendance or grades, students of their classes that need attention, appeals to
 * them, and their own class if they lead one. Which page a user gets is decided here, not in the browser.
 */
@Service
@Transactional(readOnly = true)
public class TeacherDashboardService {

    /** Which home page: ADMIN (school admin / director), TEACHER, VIEWER. */
    public record Home(String kind, Long employeeId, Long ledClassId, String ledClassName, List<Long> classIds) {
    }

    public record MyLesson(long lessonSlotId, long classId, String className, String subject, int number,
                           LocalTime start, LocalTime end, String room, String state, int marked, int students, String phase) {
    }

    public record Missing(long lessonSlotId, long classId, LocalDate date, String className, String subject, int number,
                          String attendance, boolean graded) {
    }

    public record MyClass(long classId, String name, int students, Double todayRate, Integer todayMarked,
                          Double mastery, Double quality, Double telegramPct, List<Double> spark) {
    }

    public record AppealRow(long id, String parentName, String student, String status, LocalDateTime lastMessageAt,
                            int unread) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final SchoolAccessService access;
    private final SchoolDays schoolDays;
    private final Clock clock;

    public TeacherDashboardService(NamedParameterJdbcTemplate jdbc, SchoolAccessService access, SchoolDays schoolDays,
                                   Clock clock) {
        this.jdbc = jdbc;
        this.access = access;
        this.schoolDays = schoolDays;
        this.clock = clock;
    }

    private static int i(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private static long l(Object o) {
        return ((Number) o).longValue();
    }

    /** ADMIN → school page; VIEWER → school page without actions; EDITOR who teaches or leads a class → teacher page. */
    public Home home(long schoolId) {
        SchoolAccessService.Caller c = access.caller();
        if (c.role() == Role.VIEWER) return new Home("VIEWER", c.employeeId(), null, null, List.of());
        if (c.role() == Role.ADMIN || c.employeeId() == null) return new Home("ADMIN", c.employeeId(), null, null, List.of());
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("e", c.employeeId());
        List<Long> taught = jdbc.queryForList("""
                select distinct ls.class_id from lesson_slot ls join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id where y.school_id = :s and ls.employee_id = :e""", p, Long.class);
        List<Map<String, Object>> led = jdbc.queryForList("""
                select c.id, c.grade_number || '-' || c.section_letter as name from school_class c
                  join academic_year y on y.id = c.academic_year_id where y.school_id = :s and c.class_teacher_id = :e
                 order by c.grade_number, c.section_letter""", p);
        if (taught.isEmpty() && led.isEmpty()) return new Home("ADMIN", c.employeeId(), null, null, List.of());
        Set<Long> classes = new HashSet<>(taught);
        led.forEach(r -> classes.add(l(r.get("id"))));
        return new Home("TEACHER", c.employeeId(), led.isEmpty() ? null : l(led.get(0).get("id")),
                led.isEmpty() ? null : (String) led.get(0).get("name"), classes.stream().sorted().toList());
    }

    /** "Bugungi darslarim": the teacher's lessons today with ✓ / 🤖 / ⏳. */
    public List<MyLesson> myLessons(long schoolId, long employeeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDate today = now.toLocalDate();
        String weekday = SchoolDays.WEEKDAY_UZ.get(today.getDayOfWeek());
        if (weekday == null || !schoolDays.isSchoolDay(schoolId, today)) return List.of();
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("weekday", weekday);
        List<Map<String, Object>> lessons = jdbc.queryForList(DashboardQueries.LESSONS_OF_WEEKDAY, p);
        Map<Long, Map<String, Object>> marks = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(DashboardQueries.MARKS_BY_LESSON, p.addValue("date", today))) {
            marks.put(l(r.get("lesson_slot_id")), r);
        }
        Map<Long, List<LocalTime>> starts = new HashMap<>();
        for (Map<String, Object> r : lessons) starts.computeIfAbsent(l(r.get("class_id")), k -> new ArrayList<>()).add(((java.sql.Time) r.get("start_time")).toLocalTime());
        List<MyLesson> out = new ArrayList<>();
        for (Map<String, Object> r : lessons) {
            if (l(r.get("employee_id")) != employeeId) continue;
            LocalTime start = ((java.sql.Time) r.get("start_time")).toLocalTime();
            LocalTime end = ((java.sql.Time) r.get("end_time")).toLocalTime();
            Map<String, Object> m = marks.get(l(r.get("id")));
            int marked = m == null ? 0 : i(m.get("marked"));
            String state = marked == 0 ? "NONE" : i(m.get("real_marks")) > 0 ? "MANUAL" : "AUTO";
            String phase = now.toLocalTime().isBefore(start) ? "NEXT" : now.toLocalTime().isBefore(end) ? "NOW" : "DONE";
            int number = starts.get(l(r.get("class_id"))).stream().sorted().distinct().toList().indexOf(start) + 1;
            out.add(new MyLesson(l(r.get("id")), l(r.get("class_id")), (String) r.get("class_name"), (String) r.get("subject"),
                    number, start, end, (String) r.get("room_number"), state, marked, i(r.get("students")), phase));
        }
        return out;
    }

    /** "Davomat yoki baho kiritilmagan darslarim": the last 7 school days, lessons already over. */
    public List<Missing> myMissing(long schoolId, long employeeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, now.toLocalDate(), 7);
        if (days.isEmpty()) return List.of();
        String dayArray = "{" + String.join(",", days.stream().map(LocalDate::toString).toList()) + "}";
        List<Missing> out = new ArrayList<>();
        Map<String, List<LocalTime>> startsByClassDay = new HashMap<>();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                with days(d) as (select unnest(cast(:days as date[])))
                select ls.id, ls.class_id, days.d, c.grade_number || '-' || c.section_letter as class_name, s.name as subject,
                       ls.start_time, ls.employee_id,
                       (select count(*) from attendance a where a.lesson_slot_id = ls.id and a.record_date = days.d
                          and a.source in ('MANUAL', 'DEVICE')) as real_marks,
                       (select count(*) from attendance a where a.lesson_slot_id = ls.id and a.record_date = days.d) as marks,
                       exists (select 1 from grade g join students st on st.id = g.student_id
                                where st.class_id = ls.class_id and g.subject_id = ls.subject_id and g.grade_date = days.d) as graded
                  from lesson_slot ls
                  join school_class c on c.id = ls.class_id join academic_year y on y.id = c.academic_year_id
                  join subject s on s.id = ls.subject_id
                  join days on ls.weekday = """ + DashboardQueries.weekdayOf("days.d") + """
                 where y.school_id = :s and ls.class_id in (select class_id from lesson_slot where employee_id = :e)
                   and (days.d < :today or ls.end_time <= :nowTime)
                 order by days.d desc, ls.start_time""",
                new MapSqlParameterSource("s", schoolId).addValue("e", employeeId).addValue("days", dayArray)
                        .addValue("today", now.toLocalDate()).addValue("nowTime", now.toLocalTime()));
        for (Map<String, Object> r : rows) {
            startsByClassDay.computeIfAbsent(r.get("class_id") + "|" + r.get("d"), k -> new ArrayList<>())
                    .add(((java.sql.Time) r.get("start_time")).toLocalTime());
        }
        for (Map<String, Object> r : rows) {
            if (l(r.get("employee_id")) != employeeId) continue;
            boolean graded = Boolean.TRUE.equals(r.get("graded"));
            int real = i(r.get("real_marks"));
            int marks = i(r.get("marks"));
            if (real > 0 && graded) continue;
            String attendance = real > 0 ? "MANUAL" : marks > 0 ? "AUTO" : "NONE";
            LocalTime start = ((java.sql.Time) r.get("start_time")).toLocalTime();
            int number = startsByClassDay.get(r.get("class_id") + "|" + r.get("d")).stream().sorted().distinct().toList().indexOf(start) + 1;
            out.add(new Missing(l(r.get("id")), l(r.get("class_id")), ((Date) r.get("d")).toLocalDate(), (String) r.get("class_name"),
                    (String) r.get("subject"), number, attendance, graded));
        }
        return out;
    }

    /** Appeals to the teacher's own class (class-teacher appeals), waiting for an answer. */
    public List<AppealRow> myAppeals(long schoolId, Long ledClassId) {
        if (ledClassId == null) return List.of();
        List<AppealRow> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select a.id, a.parent_name, st.first_name || ' ' || st.last_name as student, a.status, a.last_message_at, a.unread_count
                  from appeal a join students st on st.id = a.student_id
                 where a.school_id = :s and st.class_id = :c and a.target = 'CLASS_TEACHER' and a.status in ('NEW', 'SEEN')
                 order by a.last_message_at limit 7""",
                new MapSqlParameterSource("s", schoolId).addValue("c", ledClassId))) {
            out.add(new AppealRow(l(r.get("id")), (String) r.get("parent_name"), (String) r.get("student"), (String) r.get("status"),
                    ((java.sql.Timestamp) r.get("last_message_at")).toLocalDateTime(), i(r.get("unread_count"))));
        }
        return out;
    }

    /** "Mening sinfim": today, this quarter's grades, parents in the bot, 14-day attendance. */
    public MyClass myClass(long schoolId, long classId) {
        LocalDate today = LocalDate.now(clock);
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, 14);
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("c", classId)
                .addValue("from", days.get(0)).addValue("to", today);
        Map<String, Object> cls = jdbc.queryForMap("""
                select c.grade_number || '-' || c.section_letter as name, count(st.id) as students,
                       count(st.id) filter (where exists (select 1 from parent_telegram_link l where l.student_id = st.id and l.active)) as linked
                  from school_class c left join students st on st.class_id = c.id where c.id = :c group by c.grade_number, c.section_letter""", p);
        Map<LocalDate, long[]> byDay = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("with " + DashboardQueries.DAY_CTE + """
                select d, count(*) as marked, count(*) filter (where status in ('PRESENT', 'LATE')) as present
                  from day where class_id = :c group by d""", p)) {
            byDay.put(((Date) r.get("d")).toLocalDate(), new long[]{l(r.get("present")), l(r.get("marked"))});
        }
        LocalDate[] q = ParentStats.quarterRange(today, ParentStats.quarterOf(today));
        Map<String, Object> g = jdbc.queryForMap("""
                select count(*) as total, count(*) filter (where g.score >= 3) as passed, count(*) filter (where g.score >= 4) as good
                  from grade g join students st on st.id = g.student_id where st.class_id = :c and g.grade_date between :qf and :to""",
                p.addValue("qf", q[0]));
        long[] t = byDay.get(today);
        long total = l(g.get("total"));
        return new MyClass(classId, (String) cls.get("name"), i(cls.get("students")), t == null ? null : pct(t[0], t[1]),
                t == null ? null : (int) t[1], pct(l(g.get("passed")), total), pct(l(g.get("good")), total),
                pct(i(cls.get("linked")), i(cls.get("students"))),
                days.stream().map(d -> { long[] v = byDay.get(d); return v == null ? null : pct(v[0], v[1]); }).toList());
    }
}
