package uz.azizbek.maktabboshqaruv.service.pages;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardMetrics;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * The numbers the list pages show next to each row, for one page of rows at a time (one SQL per
 * page, never one per row), and the counts beside the filter chips. What is costly and shared by
 * the whole school (the risk list, journal completeness) comes from the 90-second
 * {@link DashboardCache}.
 * <ul>
 *   <li>a student: attendance over 30 days (present + late of present + late + absent, excused
 *       left out — the home page's rule), average grade of the last 30 days and of the 30 before,
 *       the risk score (only from 40, the risk list's floor; below it null), parents in Telegram;</li>
 *   <li>an employee: lessons a week and journal completeness over the last 7 school days;</li>
 *   <li>a subject: teachers, classes and lessons a week; a position: its employees in the school;
 *       a school year: its classes and students.</li>
 * </ul>
 */
@Service
public class ListStatsService {

    public record StudentStats(Double attendance30, Double average30, Double averagePrev, Integer risk, boolean telegram) {
    }

    public record EmployeeStats(int weeklyLessons, Double journal7) {
    }

    public record SubjectStats(int teachers, int classes, int weeklyLessons) {
    }

    public record YearStats(int classes, int students) {
    }

    /** One chip: its value, label and how many rows it keeps. */
    public record Facet(String value, String label, long count) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final HomeDashboardService home;
    private final DashboardCache cache;
    private final Clock clock;

    @PersistenceContext
    private EntityManager em;

    public ListStatsService(NamedParameterJdbcTemplate jdbc, HomeDashboardService home, DashboardCache cache, Clock clock) {
        this.jdbc = jdbc;
        this.home = home;
        this.cache = cache;
        this.clock = clock;
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    private static String ids(Collection<Long> ids) {
        return "{" + String.join(",", ids.stream().map(String::valueOf).toList()) + "}";
    }

    // ------------------------------------------------------------------ students

    /**
     * One page of students in one statement (through Hibernate, so its statistics see it). The
     * risk scores come from the school's cached risk list.
     */
    @SuppressWarnings("unchecked")
    public Map<Long, StudentStats> students(long schoolId, Collection<Long> studentIds) {
        if (studentIds.isEmpty()) return Map.of();
        LocalDate today = LocalDate.now(clock);
        List<Object[]> rows = em.createNativeQuery("""
                        with ids(id) as (select unnest(cast(:ids as bigint[])))
                        select ids.id,
                               (select count(*) from attendance a where a.student_id = ids.id
                                   and a.record_date between :from30 and :today and a.status in ('PRESENT', 'LATE')) as came,
                               (select count(*) from attendance a where a.student_id = ids.id
                                   and a.record_date between :from30 and :today and a.status in ('PRESENT', 'LATE', 'ABSENT')) as counted,
                               (select avg(g.score) from grade g where g.student_id = ids.id
                                   and g.grade_date between :from30 and :today) as avg30,
                               (select avg(g.score) from grade g where g.student_id = ids.id
                                   and g.grade_date >= :from60 and g.grade_date < :from30) as avg_prev,
                               exists (select 1 from parent_telegram_link l where l.student_id = ids.id and l.active) as telegram
                          from ids""")
                .setParameter("ids", ids(studentIds))
                .setParameter("today", Date.valueOf(today))
                .setParameter("from30", Date.valueOf(today.minusDays(29)))
                .setParameter("from60", Date.valueOf(today.minusDays(59)))
                .getResultList();
        Map<Long, Integer> risk = riskScores(schoolId);
        Map<Long, StudentStats> out = new HashMap<>();
        for (Object[] r : rows) {
            long id = l(r[0]);
            out.put(id, new StudentStats(DashboardMetrics.pct(l(r[1]), l(r[2])), round2(r[3]), round2(r[4]),
                    risk.get(id), Boolean.TRUE.equals(r[5])));
        }
        return out;
    }

    /** studentId → risk score (40+) of the school, from the shared 90-second cache. */
    public Map<Long, Integer> riskScores(long schoolId) {
        Map<Long, Integer> out = new HashMap<>();
        for (HomeDashboardService.RiskRow row : cache.get(schoolId, "risk:2000:false",
                () -> home.riskStudents(schoolId, null, 2000, false)).data().rows()) {
            out.put(row.studentId(), row.score());
        }
        return out;
    }

    private static Double round2(Object v) {
        return v == null ? null : Math.round(((Number) v).doubleValue() * 100) / 100.0;
    }

    /** Students of the school per class (the class picker's numbers) and per Telegram link. */
    public Map<String, List<Facet>> studentFacets(long schoolId) {
        List<Facet> classes = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select c.id, c.grade_number, c.section_letter, count(st.id) as n
                  from school_class c join academic_year y on y.id = c.academic_year_id
                  left join students st on st.class_id = c.id
                 where y.school_id = :s
                 group by c.id, c.grade_number, c.section_letter
                 order by c.grade_number, c.section_letter""", p(schoolId))) {
            classes.add(new Facet(String.valueOf(l(r.get("id"))), r.get("grade_number") + "-" + r.get("section_letter"),
                    l(r.get("n"))));
        }
        Map<String, Object> t = jdbc.queryForMap("""
                select count(*) filter (where linked) as yes, count(*) filter (where not linked) as no
                  from (select exists (select 1 from parent_telegram_link l where l.student_id = st.id and l.active) as linked
                          from students st join school_class c on c.id = st.class_id
                          join academic_year y on y.id = c.academic_year_id where y.school_id = :s) x""", p(schoolId));
        Map<String, List<Facet>> out = new LinkedHashMap<>();
        out.put("schoolClassId", classes);
        out.put("telegram", List.of(new Facet("true", "Telegramda", l(t.get("yes"))),
                new Facet("false", "Telegramda emas", l(t.get("no")))));
        return out;
    }

    // ------------------------------------------------------------------ employees

    /** Lessons a week per employee of the page, and journal completeness from the cached 7-day table. */
    public Map<Long, EmployeeStats> employees(long schoolId, Collection<Long> employeeIds) {
        if (employeeIds.isEmpty()) return Map.of();
        Map<Long, Integer> lessons = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select ls.employee_id, count(*) as n from lesson_slot ls
                 where ls.employee_id = any(cast(:ids as bigint[])) group by ls.employee_id""",
                new MapSqlParameterSource("ids", ids(employeeIds)))) {
            lessons.put(l(r.get("employee_id")), i(r.get("n")));
        }
        Map<Long, Double> journal = new HashMap<>();
        for (HomeDashboardService.TeacherRow t : cache.get(schoolId, "journal", () -> home.journal(schoolId)).data().teachers()) {
            journal.put(t.employeeId(), t.completeness());
        }
        Map<Long, EmployeeStats> out = new HashMap<>();
        for (Long id : employeeIds) out.put(id, new EmployeeStats(lessons.getOrDefault(id, 0), journal.get(id)));
        return out;
    }

    /** Employees of the school per status and per position (the filter chips' numbers). */
    public Map<String, List<Facet>> employeeFacets(long schoolId) {
        Map<String, String> statusLabel = Map.of("ACTIVE", "Ishda", "ON_LEAVE", "Ta'tilda", "DISMISSED", "Ishdan ketgan");
        List<Facet> statuses = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select coalesce(e.status, 'ACTIVE') as status, count(*) as n from employee e
                 where e.school_id = :s group by coalesce(e.status, 'ACTIVE')""", p(schoolId))) {
            String st = (String) r.get("status");
            statuses.add(new Facet(st, statusLabel.getOrDefault(st, st), l(r.get("n"))));
        }
        statuses.sort(Comparator.comparing(f -> List.of("ACTIVE", "ON_LEAVE", "DISMISSED").indexOf(f.value())));
        List<Facet> positions = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select p.id, p.title, count(*) as n from employee e join position p on p.id = e.position_id
                 where e.school_id = :s group by p.id, p.title order by count(*) desc, p.title""", p(schoolId))) {
            positions.add(new Facet(String.valueOf(l(r.get("id"))), (String) r.get("title"), l(r.get("n"))));
        }
        Map<String, List<Facet>> out = new LinkedHashMap<>();
        out.put("status", statuses);
        out.put("positionId", positions);
        return out;
    }

    // ------------------------------------------------------------------ subjects, positions, years

    /** Teachers, classes and lessons a week of every subject of the school that has lessons. */
    public Map<Long, SubjectStats> subjects(long schoolId) {
        Map<Long, SubjectStats> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select ls.subject_id, count(distinct ls.employee_id) as teachers, count(distinct ls.class_id) as classes,
                       count(*) as lessons
                  from lesson_slot ls join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s group by ls.subject_id""", p(schoolId))) {
            out.put(l(r.get("subject_id")), new SubjectStats(i(r.get("teachers")), i(r.get("classes")), i(r.get("lessons"))));
        }
        return out;
    }

    /** Active and retired subjects of the school (the tab counts). */
    public Map<String, List<Facet>> subjectFacets(long schoolId) {
        Map<String, Object> r = jdbc.queryForMap("""
                select count(*) filter (where coalesce(active, true)) as on, count(*) filter (where not coalesce(active, true)) as off
                  from subject where school_id = :s""", p(schoolId));
        Map<String, List<Facet>> out = new LinkedHashMap<>();
        out.put("active", List.of(new Facet("true", "Faol", l(r.get("on"))), new Facet("false", "Nofaol", l(r.get("off")))));
        return out;
    }

    /** positionId → employees; of one school, or of all schools when schoolId is null. */
    public Map<Long, Integer> positionEmployees(Long schoolId) {
        Map<Long, Integer> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select e.position_id, count(*) as n from employee e
                 where cast(:s as bigint) is null or e.school_id = :s group by e.position_id""",
                new MapSqlParameterSource("s", schoolId))) {
            out.put(l(r.get("position_id")), i(r.get("n")));
        }
        return out;
    }

    /** yearId → its classes and students, for the years of one school. */
    public Map<Long, YearStats> years(long schoolId) {
        Map<Long, YearStats> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select y.id, count(distinct c.id) as classes, count(st.id) as students
                  from academic_year y left join school_class c on c.academic_year_id = y.id
                  left join students st on st.class_id = c.id
                 where y.school_id = :s group by y.id""", p(schoolId))) {
            out.put(l(r.get("id")), new YearStats(i(r.get("classes")), i(r.get("students"))));
        }
        return out;
    }
}
