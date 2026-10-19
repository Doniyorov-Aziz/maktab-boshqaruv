package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardMetrics;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * "Sinflar" page and one class's page: every class as a card (teacher, size, today's attendance,
 * quality and mastery with their change, parents on Telegram, 20 school days of attendance, students
 * at risk), two or three classes side by side, and one class in full (hero, students, parents,
 * behaviour, attendance month). Formulas are the home page's ({@link DashboardMetrics}).
 */
@Service
public class ClassesPageService {

    /** How many school days the sparklines and the comparison show. */
    static final int SPARK_DAYS = 20;

    public record ClassCard(long id, String name, int grade, String letter, Long teacherId, String teacher, int students,
                            String room, Double today, int todayMarked, Double attendance, Double quality, Double qualityDelta,
                            Double mastery, Double masteryDelta, int grades, Double telegram, List<Double> spark, int risk) {
    }

    public record Best(long id, String name, Double quality) {
    }

    public record Overview(List<ClassCard> classes, int students, Double avgSize, Double today, List<Double> todaySpark,
                           Best best, List<LocalDate> sparkDays) {
    }

    public record SubjectQuality(long subjectId, String subject, String color, Double quality, Double mastery, int grades) {
    }

    public record Point(LocalDate date, Double rate) {
    }

    public record Compared(long id, String name, Double attendance, Double mastery, Double quality,
                           List<SubjectQuality> subjects, List<Point> daily) {
    }

    public record StudentRow(long id, String name, String guardianName, String guardianPhone, String relation,
                             Double attendance, Double average, Integer risk, boolean telegram, int absentDays) {
    }

    public record Behavior(long id, long studentId, String student, LocalDate date, String type, String description,
                           String by) {
    }

    public record Detail(ClassCard card, String teacherPhone, List<Point> dynamics, Map<Integer, Integer> gradeCounts,
                         List<SubjectQuality> subjects, List<HomeDashboardService.RiskRow> risk, List<StudentRow> students,
                         List<Behavior> behavior) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final HomeDashboardService home;
    private final SchoolDays schoolDays;
    private final DashboardCache cache;
    private final Clock clock;

    public ClassesPageService(NamedParameterJdbcTemplate jdbc, HomeDashboardService home, SchoolDays schoolDays,
                              DashboardCache cache, Clock clock) {
        this.jdbc = jdbc;
        this.home = home;
        this.schoolDays = schoolDays;
        this.cache = cache;
        this.clock = clock;
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Grades per class (and subject) between dates: total, passed (3–5), good (4–5), sum of scores. */
    private Map<String, long[]> grades(long schoolId, LocalDate from, LocalDate to, boolean bySubject, Long classId) {
        Map<String, long[]> out = new HashMap<>();
        String sql = """
                select st.class_id, %s count(*) as total, count(*) filter (where g.score >= 3) as passed,
                       count(*) filter (where g.score >= 4) as good, sum(g.score) as points
                  from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to %s
                 group by st.class_id %s""".formatted(bySubject ? "g.subject_id," : "",
                classId == null ? "" : "and st.class_id = :c", bySubject ? ", g.subject_id" : "");
        MapSqlParameterSource params = p(schoolId).addValue("from", from).addValue("to", to).addValue("c", classId);
        for (Map<String, Object> r : jdbc.queryForList(sql, params)) {
            String key = l(r.get("class_id")) + (bySubject ? ":" + l(r.get("subject_id")) : "");
            out.put(key, new long[]{l(r.get("total")), l(r.get("passed")), l(r.get("good")), l(r.get("points"))});
        }
        return out;
    }

    private List<Map<String, Object>> classRows(long schoolId, Long classId) {
        return jdbc.queryForList("""
                select c.id, c.grade_number, c.section_letter, e.id as teacher_id,
                       trim(coalesce(e.last_name, '') || ' ' || coalesce(e.first_name, '')) as teacher, e.phone as teacher_phone,
                       (select count(*) from students st where st.class_id = c.id) as students,
                       (select count(distinct l.student_id) from parent_telegram_link l join students st on st.id = l.student_id
                         where st.class_id = c.id and l.active) as linked,
                       (select r.room_number from lesson_slot ls join room r on r.id = ls.room_id where ls.class_id = c.id
                         group by r.room_number order by count(*) desc, r.room_number limit 1) as room
                  from school_class c
                  join academic_year y on y.id = c.academic_year_id
                  left join employee e on e.id = c.class_teacher_id
                 where y.school_id = :s and exists (select 1 from students st where st.class_id = c.id)
                   and (cast(:c as bigint) is null or c.id = :c)
                 order by c.grade_number, c.section_letter""", p(schoolId).addValue("c", classId));
    }

    private HomeDashboardService.RiskList riskList(long schoolId) {
        return cache.get(schoolId, "risk:2000:false", () -> home.riskStudents(schoolId, null, 2000, false)).data();
    }

    // ------------------------------------------------------------------ the cards

    @Transactional(readOnly = true)
    public Overview overview(long schoolId) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, SPARK_DAYS);
        LocalDate from30 = today.minusDays(29);
        LocalDate first = days.isEmpty() || days.get(0).isAfter(from30) ? from30 : days.get(0);
        Map<Long, Map<LocalDate, long[]>> daily = home.classDaily(schoolId, first, today);
        LocalDate qFrom = ParentStats.quarterRange(today, ParentStats.quarterOf(today))[0];
        Map<String, long[]> quarter = grades(schoolId, qFrom, today, false, null);
        Map<String, long[]> last30 = grades(schoolId, today.minusDays(29), today, false, null);
        Map<String, long[]> prev30 = grades(schoolId, today.minusDays(59), today.minusDays(30), false, null);
        Map<Long, Integer> risk = new HashMap<>();
        for (HomeDashboardService.RiskRow r : riskList(schoolId).rows()) risk.merge(r.classId(), 1, Integer::sum);

        List<ClassCard> cards = new ArrayList<>();
        int students = 0;
        for (Map<String, Object> r : classRows(schoolId, null)) {
            ClassCard c = card(r, daily, quarter, last30, prev30, risk, days, today);
            cards.add(c);
            students += c.students();
        }
        Map<LocalDate, HomeDashboardService.DayRate> rates = home.dailyRates(schoolId, first, today);
        List<Double> todaySpark = new ArrayList<>();
        for (LocalDate d : days) todaySpark.add(rates.containsKey(d) ? rates.get(d).rate() : null);
        Double todayRate = rates.containsKey(today) ? rates.get(today).rate() : null;
        Best best = cards.stream().filter(c -> c.quality() != null && c.grades() >= 10)
                .max(Comparator.comparing(ClassCard::quality)).map(c -> new Best(c.id(), c.name(), c.quality())).orElse(null);
        return new Overview(cards, students, cards.isEmpty() ? null : round1(students * 1.0 / cards.size()), todayRate,
                todaySpark, best, days);
    }

    private ClassCard card(Map<String, Object> r, Map<Long, Map<LocalDate, long[]>> daily, Map<String, long[]> quarter,
                           Map<String, long[]> last30, Map<String, long[]> prev30, Map<Long, Integer> risk,
                           List<LocalDate> days, LocalDate today) {
        long id = l(r.get("id"));
        Map<LocalDate, long[]> byDay = daily.getOrDefault(id, Map.of());
        long[] t = byDay.get(today);
        long[] sum = new long[3];
        byDay.forEach((d, v) -> {
            if (!d.isBefore(today.minusDays(29))) for (int k = 0; k < 3; k++) sum[k] += v[k];
        });
        List<Double> spark = new ArrayList<>();
        for (LocalDate d : days) {
            long[] v = byDay.get(d);
            spark.add(v == null ? null : DashboardMetrics.attendance(v[0], v[1], v[2]));
        }
        String key = String.valueOf(id);
        long[] q = quarter.getOrDefault(key, new long[4]);
        long[] a = last30.getOrDefault(key, new long[4]);
        long[] b = prev30.getOrDefault(key, new long[4]);
        int students = i(r.get("students"));
        String teacher = blankToNull((String) r.get("teacher"));
        return new ClassCard(id, i(r.get("grade_number")) + "-" + r.get("section_letter"), i(r.get("grade_number")),
                (String) r.get("section_letter"), lOrNull(r.get("teacher_id")), teacher, students, (String) r.get("room"),
                t == null ? null : DashboardMetrics.attendance(t[0], t[1], t[2]), t == null ? 0 : (int) (t[0] + t[1] + t[2]),
                DashboardMetrics.attendance(sum[0], sum[1], sum[2]),
                DashboardMetrics.pct(q[2], q[0]), delta(DashboardMetrics.pct(a[2], a[0]), DashboardMetrics.pct(b[2], b[0])),
                DashboardMetrics.pct(q[1], q[0]), delta(DashboardMetrics.pct(a[1], a[0]), DashboardMetrics.pct(b[1], b[0])),
                (int) q[0], DashboardMetrics.pct(l(r.get("linked")), students), spark, risk.getOrDefault(id, 0));
    }

    private static Double delta(Double now, Double before) {
        return now == null || before == null ? null : round1(now - before);
    }

    // ------------------------------------------------------------------ compare

    @Transactional(readOnly = true)
    public List<Compared> compare(long schoolId, List<Long> ids) {
        if (ids == null || ids.size() < 2 || ids.size() > 3) throw new IllegalStateException("Solishtirish uchun 2 yoki 3 ta sinf tanlang");
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, SPARK_DAYS);
        LocalDate first = days.isEmpty() ? today.minusDays(29) : days.get(0);
        Map<Long, Map<LocalDate, long[]>> daily = home.classDaily(schoolId, first.isBefore(today.minusDays(29)) ? first : today.minusDays(29), today);
        LocalDate qFrom = ParentStats.quarterRange(today, ParentStats.quarterOf(today))[0];
        Map<String, long[]> quarter = grades(schoolId, qFrom, today, false, null);
        Map<String, long[]> bySubject = grades(schoolId, qFrom, today, true, null);
        Map<Long, String[]> subjects = subjects(schoolId);
        List<Compared> out = new ArrayList<>();
        for (Long id : ids) {
            List<Map<String, Object>> rows = classRows(schoolId, id);
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf topilmadi: " + id);
            Map<LocalDate, long[]> byDay = daily.getOrDefault(id, Map.of());
            long[] sum = new long[3];
            byDay.forEach((d, v) -> {
                if (!d.isBefore(today.minusDays(29))) for (int k = 0; k < 3; k++) sum[k] += v[k];
            });
            List<Point> points = new ArrayList<>();
            for (LocalDate d : days) {
                long[] v = byDay.get(d);
                points.add(new Point(d, v == null ? null : DashboardMetrics.attendance(v[0], v[1], v[2])));
            }
            long[] q = quarter.getOrDefault(String.valueOf(id), new long[4]);
            out.add(new Compared(id, i(rows.get(0).get("grade_number")) + "-" + rows.get(0).get("section_letter"),
                    DashboardMetrics.attendance(sum[0], sum[1], sum[2]), DashboardMetrics.pct(q[1], q[0]),
                    DashboardMetrics.pct(q[2], q[0]), subjectRows(id, bySubject, subjects), points));
        }
        return out;
    }

    /** subject id → {name, colour} */
    private Map<Long, String[]> subjects(long schoolId) {
        Map<Long, String[]> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("select id, name, color from subject where school_id = :s", p(schoolId))) {
            out.put(l(r.get("id")), new String[]{(String) r.get("name"), (String) r.get("color")});
        }
        return out;
    }

    private static List<SubjectQuality> subjectRows(long classId, Map<String, long[]> bySubject, Map<Long, String[]> subjects) {
        List<SubjectQuality> rows = new ArrayList<>();
        String prefix = classId + ":";
        bySubject.forEach((key, g) -> {
            if (!key.startsWith(prefix)) return;
            long sid = Long.parseLong(key.substring(prefix.length()));
            String[] s = subjects.getOrDefault(sid, new String[]{"—", null});
            rows.add(new SubjectQuality(sid, s[0], s[1], DashboardMetrics.pct(g[2], g[0]), DashboardMetrics.pct(g[1], g[0]), (int) g[0]));
        });
        rows.sort(Comparator.comparing(SubjectQuality::subject));
        return rows;
    }

    // ------------------------------------------------------------------ one class

    @Transactional(readOnly = true)
    public Detail detail(long schoolId, long classId, boolean withPhones) {
        List<Map<String, Object>> rows = classRows(schoolId, classId);
        if (rows.isEmpty()) {
            // a class with no students yet still opens (empty lists)
            rows = jdbc.queryForList("""
                    select c.id, c.grade_number, c.section_letter, e.id as teacher_id,
                           trim(coalesce(e.last_name, '') || ' ' || coalesce(e.first_name, '')) as teacher, e.phone as teacher_phone,
                           0 as students, 0 as linked, null as room
                      from school_class c join academic_year y on y.id = c.academic_year_id
                      left join employee e on e.id = c.class_teacher_id
                     where y.school_id = :s and c.id = :c""", p(schoolId).addValue("c", classId));
            if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf topilmadi");
        }
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, SPARK_DAYS);
        LocalDate first = days.isEmpty() || days.get(0).isAfter(today.minusDays(29)) ? today.minusDays(29) : days.get(0);
        Map<Long, Map<LocalDate, long[]>> daily = home.classDaily(schoolId, first, today);
        LocalDate qFrom = ParentStats.quarterRange(today, ParentStats.quarterOf(today))[0];
        Map<String, long[]> quarter = grades(schoolId, qFrom, today, false, classId);
        Map<String, long[]> last30 = grades(schoolId, today.minusDays(29), today, false, classId);
        Map<String, long[]> prev30 = grades(schoolId, today.minusDays(59), today.minusDays(30), false, classId);
        List<HomeDashboardService.RiskRow> classRisk = riskList(schoolId).rows().stream().filter(x -> x.classId() == classId).toList();
        ClassCard card = card(rows.get(0), daily, quarter, last30, prev30, Map.of(classId, classRisk.size()), days, today);

        List<Point> dynamics = new ArrayList<>();
        for (int k = 0; k < days.size(); k++) dynamics.add(new Point(days.get(k), card.spark().get(k)));

        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (int s = 5; s >= 2; s--) counts.put(s, 0);
        for (Map<String, Object> r : jdbc.queryForList("""
                select least(greatest(g.score, 2), 5) as score, count(*) as n from grade g join students st on st.id = g.student_id
                 where st.class_id = :c and g.grade_date between :from and :to group by 1""",
                p(schoolId).addValue("c", classId).addValue("from", qFrom).addValue("to", today))) {
            counts.put(i(r.get("score")), i(r.get("n")));
        }
        List<SubjectQuality> subjects = subjectRows(classId, grades(schoolId, qFrom, today, true, classId), subjects(schoolId));

        Map<Long, long[]> att = home.studentDays(schoolId, classId, today.minusDays(29), today);
        Map<Long, Integer> riskBy = new HashMap<>();
        for (HomeDashboardService.RiskRow r : classRisk) riskBy.put(r.studentId(), r.score());
        List<StudentRow> students = jdbc.query("""
                select st.id, st.first_name, st.last_name, st.guardian_name, st.guardian_phone, st.guardian_relation,
                       (select avg(g.score) from grade g where g.student_id = st.id and g.grade_date between :from and :to) as avg,
                       exists (select 1 from parent_telegram_link l where l.student_id = st.id and l.active) as telegram
                  from students st where st.class_id = :c order by st.last_name, st.first_name""",
                p(schoolId).addValue("c", classId).addValue("from", qFrom).addValue("to", today), (rs, n) -> {
                    long id = rs.getLong("id");
                    long[] a = att.getOrDefault(id, new long[2]);
                    Object avg = rs.getObject("avg");
                    return new StudentRow(id, (rs.getString("last_name") + " " + rs.getString("first_name")).trim(),
                            rs.getString("guardian_name"), withPhones ? rs.getString("guardian_phone") : null,
                            rs.getString("guardian_relation"), DashboardMetrics.pct(a[0], a[0] + a[1]),
                            avg == null ? null : Math.round(((Number) avg).doubleValue() * 100) / 100.0,
                            riskBy.get(id), rs.getBoolean("telegram"), (int) a[1]);
                });
        List<Behavior> behavior = jdbc.query("""
                select b.id, b.student_id, st.last_name || ' ' || st.first_name as student, b.record_date, b.type, b.description,
                       b.created_by
                  from behavior_record b join students st on st.id = b.student_id
                 where st.class_id = :c order by b.record_date desc, b.id desc limit 100""", p(schoolId).addValue("c", classId),
                (rs, n) -> new Behavior(rs.getLong("id"), rs.getLong("student_id"), rs.getString("student"),
                        rs.getDate("record_date").toLocalDate(), rs.getString("type"), rs.getString("description"),
                        rs.getString("created_by")));
        String phone = withPhones ? (String) rows.get(0).get("teacher_phone") : null;
        return new Detail(card, phone, dynamics, counts, subjects, classRisk, students, behavior);
    }

    /** The class's attendance per date of a month (for the month calendar tab). */
    @Transactional(readOnly = true)
    public List<Point> month(long schoolId, long classId, LocalDate anyDay) {
        LocalDate from = anyDay.withDayOfMonth(1);
        LocalDate to = anyDay.withDayOfMonth(anyDay.lengthOfMonth());
        Map<LocalDate, long[]> byDay = home.classDaily(schoolId, from, to).getOrDefault(classId, Map.of());
        List<Point> out = new ArrayList<>();
        byDay.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> out.add(new Point(e.getKey(), DashboardMetrics.attendance(e.getValue()[0], e.getValue()[1], e.getValue()[2]))));
        return out;
    }
}
