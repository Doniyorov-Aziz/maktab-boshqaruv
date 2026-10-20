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
 *       the risk score of every student (0–100, the home page's rule; 40+ is the risk list), parents in Telegram;</li>
 *   <li>an employee: journal completeness over the last 7 school days (lessons a week come with
 *       what they teach, EmployeeService);</li>
 *   <li>a subject: teachers, classes and lessons a week; a position: its employees in the school;
 *       a school year: its classes and students.</li>
 * </ul>
 */
@Service
public class ListStatsService {

    public record StudentStats(Double attendance30, Double average30, Double averagePrev, Integer risk, boolean telegram) {
    }

    /** A person on a card: avatar and name (and lessons a week where it matters). */
    public record Person(long id, String name, Integer lessons) {
    }

    public record SubjectStats(int teacherCount, int classes, int weeklyLessons, List<Person> teachers,
                               Double qualityNow, Double qualityPrev, List<Double> averageSpark) {
    }

    /** The quarter going on (or the last 60 days) and the one before it. */
    public record Window(LocalDate from, LocalDate to, LocalDate prevFrom, LocalDate prevTo, String label) {
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
     * risk scores come from the school's cached scores of every student.
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
        Map<Long, HomeDashboardService.RiskRow> risk = allRisk(schoolId);
        Map<Long, StudentStats> out = new HashMap<>();
        for (Object[] r : rows) {
            long id = l(r[0]);
            out.put(id, new StudentStats(DashboardMetrics.pct(l(r[1]), l(r[2])), round2(r[3]), round2(r[4]),
                    risk.get(id) == null ? null : risk.get(id).score(), Boolean.TRUE.equals(r[5])));
        }
        return out;
    }

    /** studentId → risk row of EVERY student of the school (score 0–100 and its parts), cached 90 s. */
    public Map<Long, HomeDashboardService.RiskRow> allRisk(long schoolId) {
        return cache.get(schoolId, "risk:all", () -> {
            Map<Long, HomeDashboardService.RiskRow> out = new HashMap<>();
            for (HomeDashboardService.RiskRow r : home.riskStudents(schoolId, null, Integer.MAX_VALUE, false, 0).rows()) {
                out.put(r.studentId(), r);
            }
            return out;
        }).data();
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
        out.put("atRisk", List.of(new Facet("true", "Xavf ostida", riskScores(schoolId).size())));
        LocalDate today = LocalDate.now(clock);
        Long born = jdbc.queryForObject("""
                select count(*) from students st join school_class c on c.id = st.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and extract(month from st.birth_date) = :m and extract(day from st.birth_date) = :d""",
                p(schoolId).addValue("m", today.getMonthValue()).addValue("d", today.getDayOfMonth()), Long.class);
        out.put("birthday", List.of(new Facet("true", "Bugun tug'ilgan kun", born == null ? 0 : born)));
        return out;
    }

    // ------------------------------------------------------------------ employees

    /** employeeId → journal completeness over the last 7 school days (the shared cached table, no query). */
    public Map<Long, Double> journal7(long schoolId) {
        Map<Long, Double> out = new HashMap<>();
        for (HomeDashboardService.TeacherRow t : cache.get(schoolId, "journal", () -> home.journal(schoolId)).data().teachers()) {
            out.put(t.employeeId(), t.completeness());
        }
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
        Long leads = jdbc.queryForObject("""
                select count(distinct c.class_teacher_id) from school_class c join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and c.class_teacher_id is not null""", p(schoolId), Long.class);
        out.put("classTeacher", List.of(new Facet("true", "Sinf rahbarlari", leads == null ? 0 : leads)));
        return out;
    }

    // ------------------------------------------------------------------ subjects, positions, years

    /**
     * Every subject of the school: teachers (with their lessons a week), classes, lessons a week,
     * quality (share of 4 and 5) this quarter and the one before, and the daily average of the last
     * 30 days — four grouped queries for the whole school, cached 90 s.
     */
    public Map<Long, SubjectStats> subjects(long schoolId) {
        return cache.get(schoolId, "lists:subjects", () -> computeSubjects(schoolId)).data();
    }

    private Map<Long, SubjectStats> computeSubjects(long schoolId) {
        Map<Long, int[]> counts = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select ls.subject_id, count(distinct ls.employee_id) as teachers, count(distinct ls.class_id) as classes,
                       count(*) as lessons
                  from lesson_slot ls join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s group by ls.subject_id""", p(schoolId))) {
            counts.put(l(r.get("subject_id")), new int[]{i(r.get("teachers")), i(r.get("classes")), i(r.get("lessons"))});
        }
        Map<Long, List<Person>> teachers = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select ls.subject_id, e.id, trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as name,
                       count(*) as lessons
                  from lesson_slot ls join employee e on e.id = ls.employee_id
                  join school_class c on c.id = ls.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s group by ls.subject_id, e.id, e.first_name, e.last_name
                 order by count(*) desc, name""", p(schoolId))) {
            teachers.computeIfAbsent(l(r.get("subject_id")), k -> new ArrayList<>())
                    .add(new Person(l(r.get("id")), (String) r.get("name"), i(r.get("lessons"))));
        }
        Window w = window(schoolId);
        Map<Long, Double[]> quality = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select g.subject_id,
                       count(*) filter (where g.grade_date between :f and :t and g.score >= 4) as good_now,
                       count(*) filter (where g.grade_date between :f and :t) as all_now,
                       count(*) filter (where g.grade_date between :pf and :pt and g.score >= 4) as good_prev,
                       count(*) filter (where g.grade_date between :pf and :pt) as all_prev
                  from grade g join subject s on s.id = g.subject_id
                 where s.school_id = :s and g.grade_date between :pf and :t group by g.subject_id""",
                p(schoolId).addValue("f", Date.valueOf(w.from())).addValue("t", Date.valueOf(w.to()))
                        .addValue("pf", Date.valueOf(w.prevFrom())).addValue("pt", Date.valueOf(w.prevTo())))) {
            quality.put(l(r.get("subject_id")), new Double[]{
                    DashboardMetrics.pct(l(r.get("good_now")), l(r.get("all_now"))),
                    DashboardMetrics.pct(l(r.get("good_prev")), l(r.get("all_prev")))});
        }
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(29);
        Map<Long, Double[]> spark = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select g.subject_id, g.grade_date, avg(g.score) as a
                  from grade g join subject s on s.id = g.subject_id
                 where s.school_id = :s and g.grade_date between :f and :t group by g.subject_id, g.grade_date""",
                p(schoolId).addValue("f", Date.valueOf(from)).addValue("t", Date.valueOf(today)))) {
            Double[] days = spark.computeIfAbsent(l(r.get("subject_id")), k -> new Double[30]);
            int k = (int) java.time.temporal.ChronoUnit.DAYS.between(from, date(r.get("grade_date")));
            if (k >= 0 && k < 30) days[k] = Math.round(((Number) r.get("a")).doubleValue() * 100) / 100.0;
        }
        Set<Long> ids = new HashSet<>(counts.keySet());
        ids.addAll(quality.keySet());
        ids.addAll(spark.keySet());
        Map<Long, SubjectStats> out = new HashMap<>();
        for (Long id : ids) {
            int[] c = counts.getOrDefault(id, new int[3]);
            Double[] q = quality.getOrDefault(id, new Double[2]);
            Double[] sp = spark.get(id);
            out.put(id, new SubjectStats(c[0], c[1], c[2], teachers.getOrDefault(id, List.of()), q[0], q[1],
                    sp == null ? List.of() : java.util.Arrays.asList(sp)));
        }
        return out;
    }

    /**
     * The quarter of the school's current year that today falls in (or the last one that ended) and
     * the quarter before it; without quarters, the last 60 days and the 60 before.
     */
    public Window window(long schoolId) {
        LocalDate today = LocalDate.now(clock);
        List<Map<String, Object>> quarters = jdbc.queryForList("""
                select t.name, t.start_date, t.end_date from academic_term t
                  join academic_year y on y.id = t.academic_year_id
                 where y.school_id = :s and t.kind = 'QUARTER' and t.start_date <= :t
                 order by t.start_date desc limit 2""", p(schoolId).addValue("t", Date.valueOf(today)));
        if (quarters.isEmpty()) {
            return new Window(today.minusDays(59), today, today.minusDays(119), today.minusDays(60), "60 kun");
        }
        LocalDate f = date(quarters.get(0).get("start_date"));
        LocalDate t = date(quarters.get(0).get("end_date"));
        if (t.isAfter(today)) t = today;
        LocalDate pf = quarters.size() > 1 ? date(quarters.get(1).get("start_date")) : f.minusDays(60);
        LocalDate pt = quarters.size() > 1 ? date(quarters.get(1).get("end_date")) : f.minusDays(1);
        return new Window(f, t, pf, pt, (String) quarters.get(0).get("name"));
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

    /** positionId → its first employees (by name) for the card's avatars; one school or all. */
    public Map<Long, List<Person>> positionPeople(Long schoolId, int perPosition) {
        Map<Long, List<Person>> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select position_id, id, name from (
                  select e.position_id, e.id, trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as name,
                         row_number() over (partition by e.position_id order by e.last_name, e.first_name) as n
                    from employee e where cast(:s as bigint) is null or e.school_id = :s) x
                 where n <= :k""", new MapSqlParameterSource("s", schoolId).addValue("k", perPosition))) {
            out.computeIfAbsent(l(r.get("position_id")), k -> new ArrayList<>())
                    .add(new Person(l(r.get("id")), (String) r.get("name"), null));
        }
        return out;
    }
}
