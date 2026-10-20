package uz.azizbek.maktabboshqaruv.service.dashboard;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceSettingsService;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoMode;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import static uz.azizbek.maktabboshqaruv.service.dashboard.DashboardQueries.*;

/**
 * The home page of a school ("qaror qabul qilish markazi"): every widget answers one question
 * with the school's real data; the formulas live in {@link DashboardMetrics}. Percentages have one
 * decimal; null means "no data yet" (never 0).
 */
@Service
@Transactional(readOnly = true)
public class HomeDashboardService {

    // ------------------------------------------------------------------ result shapes

    /** One KPI card: value, change against the previous period, 14 school-day sparkline. */
    public record Kpi(String key, Double value, Double delta, boolean higherIsBetter, List<Double> spark,
                      Map<String, Object> extra) {
    }

    /** One class on today's attendance strip. state: FULL, PARTIAL (under half marked), NONE (not taken), NO_LESSONS. */
    public record MapTile(long classId, String name, int grade, String letter, String teacher, int students, int present,
                          int late, int absent, int excused, int marked, Double rate, String state, String source) {
    }

    public record TodayMap(List<MapTile> classes, Map<String, Integer> ring, Double rate, Map<String, Integer> sources,
                           Map<String, Integer> thresholds, boolean schoolDay) {
    }

    public record AttentionItem(String kind, String level, String title, String subtitle, String detail,
                                Map<String, Object> ref) {
    }

    public record AttentionGroup(String kind, String level, String title, int total, List<AttentionItem> items) {
    }

    public record LiveLesson(long lessonSlotId, long classId, String className, String subject, String teacher,
                             String room, String state, int marked, int students) {
    }

    public record Live(String phase, Integer period, LocalTime start, LocalTime end, Long minutesLeft,
                       Integer nextPeriod, LocalTime nextStart, List<LiveLesson> lessons, Map<String, Object> summary) {
    }

    public record HealthCard(Integer index, String label, Integer delta, Map<String, Double> parts,
                             Map<String, Double> previous, DashboardMetrics.Weights weights, int days) {
    }

    public record Bubble(long classId, String name, String teacher, int students, Double attendance, Double quality,
                         Double mastery, int grades) {
    }

    public record ClassMap(List<Bubble> classes, Double avgAttendance, Double avgQuality, int days) {
    }

    /** phone: the guardian's, only for staff who may call (null for VIEWER); telegram: a parent joined the bot. */
    public record RiskRow(long studentId, String name, long classId, String className, int score, int absentDays,
                          Double avgBefore, Double avgNow, int twos, DashboardMetrics.Risk parts, String phone,
                          boolean telegram) {
    }

    public record RiskList(int total, List<RiskRow> rows) {
    }

    public record DynPoint(LocalDate date, Double attendance, Double quality, Integer absent, Integer grades) {
    }

    public record Dynamics(List<DynPoint> points, int target) {
    }

    public record Signal(String kind, String level, String title, int count, List<AttentionItem> items) {
    }

    public record SubjectRow(long subjectId, String subject, int total, Double mastery, Double quality) {
    }

    public record GradeWeek(String label, LocalDate from, LocalDate to, Map<Integer, Integer> counts, int total,
                            Double average, Double share5, Double share2) {
    }

    public record TeacherRow(long employeeId, String name, int lessons, int attended, int graded,
                             Double attendedPct, Double gradedPct, Double completeness) {
    }

    public record Journal(List<TeacherRow> teachers, Double school, int days) {
    }

    public record ParentsTelegram(int students, int linked, Double linkedPct, int sentWeek, int deliveredWeek,
                                  Double deliveredPct, Integer bookDownloadsWeek) {
    }

    public record EventRow(long id, String title, String type, LocalDate startDate, LocalDate endDate, long daysLeft) {
    }

    // ------------------------------------------------------------------

    private final NamedParameterJdbcTemplate jdbc;
    private final SchoolDays schoolDays;
    private final AttendanceSettingsService settings;
    private final Clock clock;
    private final DemoMode demo;
    private final uz.azizbek.maktabboshqaruv.service.library.LibraryPermissions libraryPermissions;

    public HomeDashboardService(NamedParameterJdbcTemplate jdbc, SchoolDays schoolDays,
                                AttendanceSettingsService settings, Clock clock, DemoMode demo,
                                uz.azizbek.maktabboshqaruv.service.library.LibraryPermissions libraryPermissions) {
        this.libraryPermissions = libraryPermissions;
        this.jdbc = jdbc;
        this.schoolDays = schoolDays;
        this.settings = settings;
        this.clock = clock;
        this.demo = demo;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    static Double pct(long part, long whole) {
        return DashboardMetrics.pct(part, whole);
    }

    static Double minus(Double a, Double b) {
        return a == null || b == null ? null : Math.round((a - b) * 10) / 10.0;
    }

    private static int i(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    private static Long l(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static Double d(Object o) {
        return o == null ? null : Math.round(((Number) o).doubleValue() * 100) / 100.0;
    }

    private static LocalDate date(Object o) {
        return o instanceof Date dt ? dt.toLocalDate() : (LocalDate) o;
    }

    private static LocalTime time(Object o) {
        return o instanceof java.sql.Time t ? t.toLocalTime() : (LocalTime) o;
    }

    /** Attendance % of one DAILY row (excused out of the base). */
    private static Double rate(Map<String, Object> r) {
        return r == null ? null : pct(i(r.get("present")), i(r.get("counted")));
    }

    private Map<LocalDate, Map<String, Object>> dailyAttendance(long schoolId, LocalDate from, LocalDate to) {
        Map<LocalDate, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(DAILY, p(schoolId).addValue("from", from).addValue("to", to))) {
            byDate.put(date(r.get("d")), r);
        }
        return byDate;
    }

    /** Grades per date: total, passed (3–5), good (4–5). */
    private Map<LocalDate, long[]> dailyGrades(long schoolId, LocalDate from, LocalDate to) {
        Map<LocalDate, long[]> grades = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(GRADES_DAILY, p(schoolId).addValue("from", from).addValue("to", to))) {
            grades.put(date(r.get("d")), new long[]{l(r.get("total")), l(r.get("passed")), l(r.get("good"))});
        }
        return grades;
    }

    private static long[] sum(Map<LocalDate, long[]> grades, Predicate<LocalDate> in) {
        long[] s = new long[3];
        grades.forEach((dt, v) -> {
            if (in.test(dt)) for (int k = 0; k < 3; k++) s[k] += v[k];
        });
        return s;
    }

    /** The start of a period that ends today: quarter (default), month or week (from Monday). */
    LocalDate periodStart(String period) {
        LocalDate today = today();
        return switch (period == null ? "quarter" : period) {
            case "month" -> today.withDayOfMonth(1);
            case "week" -> today.with(DayOfWeek.MONDAY);
            default -> ParentStats.quarterRange(today, ParentStats.quarterOf(today))[0];
        };
    }

    /** Classes with lessons today (none on a Sunday or a holiday). */
    private Set<Long> classesWithLessonsToday(long schoolId) {
        LocalDate today = today();
        String weekday = SchoolDays.WEEKDAY_UZ.get(today.getDayOfWeek());
        if (weekday == null || !schoolDays.isSchoolDay(schoolId, today)) return Set.of();
        return new HashSet<>(jdbc.queryForList("select distinct ls.class_id from lesson_slot ls join school_class c on c.id = ls.class_id "
                + "join academic_year y on y.id = c.academic_year_id where y.school_id = :s and ls.weekday = :w "
                + "and exists (select 1 from students st where st.class_id = c.id)",
                p(schoolId).addValue("w", weekday), Long.class));
    }

    // ------------------------------------------------------------------ 6.1 KPI

    /** The four hero cards: attendance today, absent today, mastery and quality over the period. */
    public List<Kpi> kpis(long schoolId, String period) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, 15);
        LocalDate from = days.get(0);
        Map<LocalDate, Map<String, Object>> att = dailyAttendance(schoolId, from, today);
        List<LocalDate> spark = days.subList(Math.max(0, days.size() - 14), days.size());
        boolean todayIsSchoolDay = days.get(days.size() - 1).equals(today);
        LocalDate prevDay = todayIsSchoolDay ? (days.size() >= 2 ? days.get(days.size() - 2) : null) : days.get(days.size() - 1);

        Map<String, Object> t = todayIsSchoolDay ? att.getOrDefault(today, Map.of()) : Map.of();
        Map<String, Object> y = prevDay == null ? Map.of() : att.getOrDefault(prevDay, Map.of());
        Set<Long> withLessons = classesWithLessonsToday(schoolId);

        List<Kpi> out = new ArrayList<>();
        Double rateToday = t.isEmpty() ? null : rate(t);
        Double ratePrev = y.isEmpty() ? null : rate(y);
        Map<String, Object> e1 = new LinkedHashMap<>();
        e1.put("classesTaken", i(t.get("classes")));
        e1.put("classesTotal", withLessons.size());
        e1.put("autoClasses", i(t.get("auto_classes")));
        e1.put("schoolDay", todayIsSchoolDay);
        out.add(new Kpi("attendance", rateToday, minus(rateToday, ratePrev), true,
                spark.stream().map(dt -> rate(att.get(dt))).toList(), e1));

        Integer absentToday = t.isEmpty() ? null : i(t.get("absent"));
        Map<String, Object> e2 = new LinkedHashMap<>();
        e2.put("excused", t.isEmpty() ? null : i(t.get("excused")));
        e2.put("late", t.isEmpty() ? null : i(t.get("late")));
        out.add(new Kpi("absent", absentToday == null ? null : absentToday.doubleValue(),
                absentToday == null || y.isEmpty() ? null : (double) (absentToday - i(y.get("absent"))), false,
                spark.stream().map(dt -> { var r = att.get(dt); return r == null ? null : (double) i(r.get("absent")); }).toList(), e2));

        // grades: the period for the value, last 7 vs previous 7 school days for the change
        LocalDate start = periodStart(period);
        Map<LocalDate, long[]> grades = dailyGrades(schoolId, start.isBefore(days.get(0)) ? start : days.get(0), today);
        long[] q = sum(grades, dt -> !dt.isBefore(start) && !dt.isAfter(today));
        List<LocalDate> last7 = days.subList(Math.max(0, days.size() - 7), days.size());
        List<LocalDate> prev7 = days.subList(Math.max(0, days.size() - 14), Math.max(0, days.size() - 7));
        long[] g7 = sum(grades, last7::contains);
        long[] gp7 = sum(grades, prev7::contains);
        Map<String, Object> eq = new LinkedHashMap<>();
        eq.put("period", period == null ? "quarter" : period);
        eq.put("quarter", ParentStats.quarterOf(today));
        eq.put("from", start.toString());
        eq.put("grades", q[0]);
        out.add(new Kpi("mastery", pct(q[1], q[0]), minus(pct(g7[1], g7[0]), pct(gp7[1], gp7[0])), true,
                spark.stream().map(dt -> { long[] v = grades.get(dt); return v == null ? null : pct(v[1], v[0]); }).toList(), eq));
        out.add(new Kpi("quality", pct(q[2], q[0]), minus(pct(g7[2], g7[0]), pct(gp7[2], gp7[0])), true,
                spark.stream().map(dt -> { long[] v = grades.get(dt); return v == null ? null : pct(v[2], v[0]); }).toList(), eq));
        return out;
    }

    // ------------------------------------------------------------------ 6.2 today, compact

    public TodayMap todayMap(long schoolId) {
        LocalDate today = today();
        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        Map<Long, Map<String, Object>> classes = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(CLASSES, p(schoolId))) classes.put(l(r.get("id")), r);

        Map<Long, int[]> counts = new HashMap<>(); // present, late, absent, excused, auto, demo, real
        Map<String, Integer> sources = new LinkedHashMap<>();
        sources.put("manual", 0);
        sources.put("auto", 0);
        for (Map<String, Object> r : jdbc.queryForList(TODAY_BY_CLASS, p(schoolId).addValue("from", today).addValue("to", today))) {
            int[] c = counts.computeIfAbsent(l(r.get("class_id")), k -> new int[7]);
            int n = i(r.get("n"));
            switch ((String) r.get("status")) {
                case "PRESENT" -> c[0] += n;
                case "LATE" -> c[1] += n;
                case "ABSENT" -> c[2] += n;
                default -> c[3] += n;
            }
            String src = (String) r.get("source");
            if (src.equals("AUTO_CARRY")) c[4] += n;
            else if (src.equals("DEMO_SIM")) c[5] += n;
            else c[6] += n;
            sources.merge(src.equals("MANUAL") || src.equals("DEVICE") ? "manual" : "auto", n, Integer::sum);
        }
        Set<Long> withLessons = classesWithLessonsToday(schoolId);

        List<MapTile> tiles = new ArrayList<>();
        int[] ring = new int[5];
        for (Map.Entry<Long, Map<String, Object>> e : classes.entrySet()) {
            Map<String, Object> r = e.getValue();
            int[] c = counts.getOrDefault(e.getKey(), new int[7]);
            int marked = c[0] + c[1] + c[2] + c[3];
            int students = i(r.get("students"));
            boolean lessons = withLessons.contains(e.getKey());
            String state = !lessons && marked == 0 ? "NO_LESSONS" : marked == 0 ? "NONE"
                    : DashboardMetrics.partial(marked, students) ? "PARTIAL" : "FULL";
            String source = marked == 0 ? null : c[6] > 0 ? (c[4] + c[5] > 0 ? "MIXED" : "MANUAL") : (c[5] > 0 ? "DEMO_SIM" : "AUTO_CARRY");
            tiles.add(new MapTile(e.getKey(), i(r.get("grade_number")) + "-" + r.get("section_letter"), i(r.get("grade_number")),
                    (String) r.get("section_letter"), blankToNull((String) r.get("teacher")), students, c[0], c[1], c[2], c[3], marked,
                    DashboardMetrics.attendance(c[0], c[1], c[2]), state, source));
            ring[0] += c[0];
            ring[1] += c[1];
            ring[2] += c[2];
            ring[3] += c[3];
            if (lessons) ring[4] += Math.max(0, students - marked);
        }
        Map<String, Integer> ringMap = new LinkedHashMap<>();
        ringMap.put("present", ring[0]);
        ringMap.put("late", ring[1]);
        ringMap.put("absent", ring[2]);
        ringMap.put("excused", ring[3]);
        ringMap.put("unmarked", ring[4]);
        return new TodayMap(tiles, ringMap, DashboardMetrics.attendance(ring[0], ring[1], ring[2]), sources,
                Map.of("good", s.heatGood(), "ok", s.heatOk(), "warn", s.heatWarn()), !withLessons.isEmpty());
    }

    // ------------------------------------------------------------------ 6.9 live lessons

    public Live liveLessons(long schoolId) {
        LocalDateTime nowDt = LocalDateTime.now(clock);
        LocalDate today = nowDt.toLocalDate();
        LocalTime now = nowDt.toLocalTime();
        String weekday = SchoolDays.WEEKDAY_UZ.get(today.getDayOfWeek());
        if (weekday == null || !schoolDays.isSchoolDay(schoolId, today)) {
            return new Live("DAY_OFF", null, null, null, null, null, null, List.of(), Map.of());
        }
        List<Map<String, Object>> lessons = jdbc.queryForList(LESSONS_OF_WEEKDAY, p(schoolId).addValue("weekday", weekday));
        if (lessons.isEmpty()) return new Live("NO_LESSONS", null, null, null, null, null, null, List.of(), Map.of());
        // the bell periods of the day: distinct (start, end), in order
        List<LocalTime[]> periods = lessons.stream().map(r -> new LocalTime[]{time(r.get("start_time")), time(r.get("end_time"))})
                .filter(distinctBy()).sorted(Comparator.comparing(t -> t[0])).toList();
        Map<Long, Map<String, Object>> marks = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(MARKS_BY_LESSON, p(schoolId).addValue("date", today))) {
            marks.put(l(r.get("lesson_slot_id")), r);
        }

        for (int k = 0; k < periods.size(); k++) {
            LocalTime[] pr = periods.get(k);
            if (!now.isBefore(pr[0]) && now.isBefore(pr[1])) {
                List<LiveLesson> current = new ArrayList<>();
                for (Map<String, Object> r : lessons) {
                    if (!time(r.get("start_time")).equals(pr[0])) continue;
                    current.add(live(r, marks.get(l(r.get("id")))));
                }
                current.sort(Comparator.comparing((LiveLesson x) -> stateOrder(x.state())).thenComparing(LiveLesson::className));
                return new Live("LESSON", k + 1, pr[0], pr[1], Duration.between(now, pr[1]).toMinutes(), null, null, current, Map.of());
            }
        }
        if (now.isBefore(periods.get(0)[0])) {
            return new Live("BEFORE", null, null, null, Duration.between(now, periods.get(0)[0]).toMinutes(), 1,
                    periods.get(0)[0], List.of(), Map.of());
        }
        for (int k = 0; k + 1 < periods.size(); k++) {
            if (!now.isBefore(periods.get(k)[1]) && now.isBefore(periods.get(k + 1)[0])) {
                return new Live("BREAK", null, null, null, Duration.between(now, periods.get(k + 1)[0]).toMinutes(),
                        k + 2, periods.get(k + 1)[0], List.of(), Map.of());
            }
        }
        // the day is over: a short summary
        int total = lessons.size();
        int real = 0, auto = 0;
        for (Map<String, Object> r : lessons) {
            Map<String, Object> m = marks.get(l(r.get("id")));
            if (m == null) continue;
            if (i(m.get("real_marks")) > 0) real++;
            else if (i(m.get("marked")) > 0) auto++;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("lessons", total);
        summary.put("taken", real);
        summary.put("auto", auto);
        summary.put("missing", total - real - auto);
        return new Live("AFTER", null, null, null, null, null, null, List.of(), summary);
    }

    private static Predicate<LocalTime[]> distinctBy() {
        Set<String> seen = new HashSet<>();
        return t -> seen.add(t[0] + "-" + t[1]);
    }

    private static int stateOrder(String s) {
        return switch (s) {
            case "NONE" -> 0;
            case "AUTO" -> 1;
            default -> 2;
        };
    }

    private LiveLesson live(Map<String, Object> r, Map<String, Object> m) {
        int marked = m == null ? 0 : i(m.get("marked"));
        String state = m == null || marked == 0 ? "NONE" : i(m.get("real_marks")) > 0 ? "MANUAL" : "AUTO";
        String teacher = (String) r.get("teacher");
        return new LiveLesson(l(r.get("id")), l(r.get("class_id")), (String) r.get("class_name"), (String) r.get("subject"),
                teacher, (String) r.get("room_number"), state, marked, i(r.get("students")));
    }

    // ------------------------------------------------------------------ 6.1 health index

    /**
     * The school's health index over the last 7 school days, and the same for the 7 before (delta).
     * Telegram has no history, so it is the same in both.
     */
    public HealthCard health(long schoolId) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, 14);
        List<LocalDate> last = days.subList(Math.max(0, days.size() - 7), days.size());
        List<LocalDate> prev = days.subList(0, Math.max(0, days.size() - 7));
        Map<LocalDate, Map<String, Object>> att = dailyAttendance(schoolId, days.get(0), today);
        Map<LocalDate, long[]> grades = dailyGrades(schoolId, days.get(0), today);
        Double telegram = parentsTelegram(schoolId).linkedPct();
        DashboardMetrics.Weights w = weights(schoolId);

        Map<String, Double> partsNow = parts(att, grades, last, journal(schoolId, null, last).school(), telegram);
        Map<String, Double> partsPrev = prev.isEmpty() ? null : parts(att, grades, prev, journal(schoolId, null, prev).school(), telegram);
        DashboardMetrics.Health now = DashboardMetrics.health(partsNow.get("attendance"), partsNow.get("quality"),
                partsNow.get("journal"), partsNow.get("telegram"), w);
        Integer delta = null;
        if (partsPrev != null) {
            DashboardMetrics.Health before = DashboardMetrics.health(partsPrev.get("attendance"), partsPrev.get("quality"),
                    partsPrev.get("journal"), partsPrev.get("telegram"), w);
            if (now.index() != null && before.index() != null) delta = now.index() - before.index();
        }
        return new HealthCard(now.index(), now.label(), delta, partsNow, partsPrev, w, last.size());
    }

    DashboardMetrics.Weights weights(long schoolId) {
        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        return new DashboardMetrics.Weights(s.healthWeightAttendance(), s.healthWeightQuality(), s.healthWeightJournal(),
                s.healthWeightTelegram());
    }

    private static Map<String, Double> parts(Map<LocalDate, Map<String, Object>> att, Map<LocalDate, long[]> grades,
                                             List<LocalDate> days, Double journal, Double telegram) {
        long present = 0, counted = 0;
        for (LocalDate dt : days) {
            Map<String, Object> r = att.get(dt);
            if (r == null) continue;
            present += i(r.get("present"));
            counted += i(r.get("counted"));
        }
        long[] g = sum(grades, days::contains);
        Map<String, Double> m = new LinkedHashMap<>();
        m.put("attendance", pct(present, counted));
        m.put("quality", pct(g[2], g[0]));
        m.put("journal", journal);
        m.put("telegram", telegram);
        return m;
    }

    // ------------------------------------------------------------------ 6.3 classes: attendance × quality

    /** Each class: 30-day attendance (x), this quarter's quality (y), students (size). */
    public ClassMap classMap(long schoolId) {
        LocalDate today = today();
        LocalDate from = today.minusDays(29);
        LocalDate qFrom = periodStart("quarter");
        Map<Long, long[]> att = new HashMap<>(); // present, late, absent
        for (Map<String, Object> r : jdbc.queryForList("with " + DAY_CTE + """
                select class_id,
                       count(*) filter (where status = 'PRESENT') as present,
                       count(*) filter (where status = 'LATE') as late,
                       count(*) filter (where status = 'ABSENT') as absent
                  from day group by class_id""", p(schoolId).addValue("from", from).addValue("to", today))) {
            att.put(l(r.get("class_id")), new long[]{l(r.get("present")), l(r.get("late")), l(r.get("absent"))});
        }
        Map<Long, long[]> gr = new HashMap<>(); // total, passed, good
        for (Map<String, Object> r : jdbc.queryForList("""
                select st.class_id, count(*) as total,
                       count(*) filter (where g.score >= 3) as passed, count(*) filter (where g.score >= 4) as good
                  from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by st.class_id""", p(schoolId).addValue("from", qFrom).addValue("to", today))) {
            gr.put(l(r.get("class_id")), new long[]{l(r.get("total")), l(r.get("passed")), l(r.get("good"))});
        }
        List<Bubble> out = new ArrayList<>();
        long[] attSum = new long[3];
        long[] grSum = new long[3];
        for (Map<String, Object> r : jdbc.queryForList(CLASSES, p(schoolId))) {
            long id = l(r.get("id"));
            long[] a = att.getOrDefault(id, new long[3]);
            long[] g = gr.getOrDefault(id, new long[3]);
            for (int k = 0; k < 3; k++) {
                attSum[k] += a[k];
                grSum[k] += g[k];
            }
            out.add(new Bubble(id, i(r.get("grade_number")) + "-" + r.get("section_letter"), blankToNull((String) r.get("teacher")),
                    i(r.get("students")), DashboardMetrics.attendance(a[0], a[1], a[2]), pct(g[2], g[0]), pct(g[1], g[0]), (int) g[0]));
        }
        return new ClassMap(out, DashboardMetrics.attendance(attSum[0], attSum[1], attSum[2]), pct(grSum[2], grSum[0]), 30);
    }

    // ------------------------------------------------------------------ 6.4 students at risk

    /** Students with a risk score of {@link DashboardMetrics#RISK_LIST_FROM}+, highest first. classIds limits it (null = school). */
    public RiskList riskStudents(long schoolId, Collection<Long> classIds, int limit) {
        return riskStudents(schoolId, classIds, limit, false);
    }

    public RiskList riskStudents(long schoolId, Collection<Long> classIds, int limit, boolean withPhones) {
        return riskStudents(schoolId, classIds, limit, withPhones, DashboardMetrics.RISK_LIST_FROM);
    }

    /** minScore 0: every student with their score and its parts (the O'quvchilar list shows each one's ring). */
    public RiskList riskStudents(long schoolId, Collection<Long> classIds, int limit, boolean withPhones, int minScore) {
        LocalDate today = today();
        Map<Long, Integer> absent = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("with " + DAY_CTE + """
                select student_id, count(*) filter (where status = 'ABSENT') as absent from day group by student_id""",
                p(schoolId).addValue("from", today.minusDays(29)).addValue("to", today))) {
            absent.put(l(r.get("student_id")), i(r.get("absent")));
        }
        Map<Long, Map<String, Object>> grades = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select g.student_id,
                       avg(g.score) filter (where g.grade_date > :mid) as now_avg,
                       count(*) filter (where g.grade_date > :mid) as now_n,
                       avg(g.score) filter (where g.grade_date <= :mid) as before_avg,
                       count(*) filter (where g.grade_date <= :mid) as before_n,
                       count(*) filter (where g.score = 2 and g.grade_date > :d30) as twos
                  from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by g.student_id""",
                p(schoolId).addValue("mid", today.minusDays(14)).addValue("d30", today.minusDays(30))
                        .addValue("from", today.minusDays(42)).addValue("to", today))) {
            grades.put(l(r.get("student_id")), r);
        }
        List<RiskRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select st.id, st.first_name || ' ' || st.last_name as name, st.class_id, st.guardian_phone,
                       exists (select 1 from parent_telegram_link l where l.student_id = st.id and l.active) as telegram,
                       c.grade_number || '-' || c.section_letter as class_name
                  from students st join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s""", p(schoolId))) {
            long id = l(r.get("id"));
            long classId = l(r.get("class_id"));
            if (classIds != null && !classIds.contains(classId)) continue;
            Map<String, Object> g = grades.get(id);
            // an average needs at least two grades to mean anything
            Double now = g == null || i(g.get("now_n")) < 2 ? null : d(g.get("now_avg"));
            Double before = g == null || i(g.get("before_n")) < 2 ? null : d(g.get("before_avg"));
            int twos = g == null ? 0 : i(g.get("twos"));
            int abs = absent.getOrDefault(id, 0);
            DashboardMetrics.Risk risk = DashboardMetrics.risk(abs, now, before, twos);
            if (risk.score() < minScore) continue;
            rows.add(new RiskRow(id, (String) r.get("name"), classId, (String) r.get("class_name"), risk.score(), abs,
                    before, now, twos, risk, withPhones ? blankToNull((String) r.get("guardian_phone")) : null,
                    Boolean.TRUE.equals(r.get("telegram"))));
        }
        rows.sort(Comparator.comparing((RiskRow x) -> -x.score()).thenComparing(RiskRow::name));
        return new RiskList(rows.size(), rows.subList(0, Math.min(rows.size(), Math.max(1, limit))));
    }

    // ------------------------------------------------------------------ shared with the other pages

    /** One date's attendance as the home page counts it (a student's latest lesson of the day). */
    public record DayRate(LocalDate date, Double rate, int present, int absent, int marked) {
    }

    /** Attendance per date between the two dates (only dates with marks). */
    public Map<LocalDate, DayRate> dailyRates(long schoolId, LocalDate from, LocalDate to) {
        Map<LocalDate, DayRate> out = new HashMap<>();
        dailyAttendance(schoolId, from, to).forEach((dt, r) -> out.put(dt,
                new DayRate(dt, rate(r), i(r.get("present")), i(r.get("absent")), i(r.get("marked")))));
        return out;
    }

    /** Per class and date: {present, late, absent} with the same definition. */
    public Map<Long, Map<LocalDate, long[]>> classDaily(long schoolId, LocalDate from, LocalDate to) {
        Map<Long, Map<LocalDate, long[]>> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("with " + DAY_CTE + """
                select class_id, d,
                       count(*) filter (where status = 'PRESENT') as present,
                       count(*) filter (where status = 'LATE') as late,
                       count(*) filter (where status = 'ABSENT') as absent
                  from day group by class_id, d""", p(schoolId).addValue("from", from).addValue("to", to))) {
            out.computeIfAbsent(l(r.get("class_id")), k -> new HashMap<>())
                    .put(date(r.get("d")), new long[]{l(r.get("present")), l(r.get("late")), l(r.get("absent"))});
        }
        return out;
    }

    /** Per student between the dates: {present + late, absent} days (latest lesson of each day). */
    public Map<Long, long[]> studentDays(long schoolId, long classId, LocalDate from, LocalDate to) {
        Map<Long, long[]> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("with " + DAY_CTE + """
                select student_id,
                       count(*) filter (where status in ('PRESENT', 'LATE')) as present,
                       count(*) filter (where status = 'ABSENT') as absent
                  from day where class_id = :c group by student_id""",
                p(schoolId).addValue("from", from).addValue("to", to).addValue("c", classId))) {
            out.put(l(r.get("student_id")), new long[]{l(r.get("present")), l(r.get("absent"))});
        }
        return out;
    }

    // ------------------------------------------------------------------ 6.5 dynamics (30 school days)

    public Dynamics dynamics(long schoolId, int count) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, Math.max(5, Math.min(count, 90)));
        Map<LocalDate, Map<String, Object>> att = dailyAttendance(schoolId, days.get(0), today);
        Map<LocalDate, long[]> grades = dailyGrades(schoolId, days.get(0), today);
        List<DynPoint> points = new ArrayList<>();
        for (LocalDate dt : days) {
            Map<String, Object> r = att.get(dt);
            long[] g = grades.get(dt);
            points.add(new DynPoint(dt, rate(r), g == null ? null : pct(g[2], g[0]),
                    r == null ? null : i(r.get("absent")), g == null ? null : (int) g[0]));
        }
        return new Dynamics(points, settings.forSchool(schoolId).attendanceTarget());
    }

    // ------------------------------------------------------------------ 6.6 signals

    /** The five signals of the compact "E'tibor talab qiladi" card, each with its full list (for the drawer). */
    public List<Signal> signals(long schoolId, Set<Long> classIds, boolean withPhones) {
        Map<String, AttentionGroup> groups = new LinkedHashMap<>();
        for (AttentionGroup g : attention(schoolId, classIds, withPhones, 50)) groups.put(g.kind(), g);
        List<Signal> out = new ArrayList<>();
        for (String kind : List.of("UNMARKED_LESSON", "ABSENT_STREAK", "APPEAL_OVERDUE", "GRADE_DROP", "TEACHER_DISCIPLINE")) {
            AttentionGroup g = groups.get(kind);
            if (g == null) continue;
            out.add(new Signal(kind, g.level(), g.title(), g.total(), g.items()));
        }
        Signal year = missingYear(schoolId);
        if (year != null) out.add(0, year);
        return out;
    }

    /**
     * No school year holds today (outside the summer): attendance and grades have no year to belong
     * to. Shown first, as one row that leads to O'quv yillari where the year is created.
     */
    Signal missingYear(long schoolId) {
        LocalDate today = today();
        if (today.getMonthValue() >= 6 && today.getMonthValue() <= 8) return null;
        Long n = jdbc.queryForObject("""
                select count(*) from academic_year where school_id = :s and :d between start_date and end_date""",
                p(schoolId).addValue("d", today), Long.class);
        if (n != null && n > 0) return null;
        int y = today.getMonthValue() >= 7 ? today.getYear() : today.getYear() - 1;
        String title = y + "–" + (y + 1);
        AttentionItem item = new AttentionItem("NO_CURRENT_YEAR", "HIGH", title + " o'quv yili kiritilmagan",
                "Bugungi sana hech qaysi o'quv yiliga tushmaydi", null, Map.of("title", title));
        return new Signal("NO_CURRENT_YEAR", "HIGH", "Joriy o'quv yili kiritilmagan", 1, List.of(item));
    }

    // ------------------------------------------------------------------ 6.7 subjects

    public List<SubjectRow> subjectQuality(long schoolId, String period) {
        LocalDate today = today();
        List<SubjectRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select s.id, s.name, count(*) as total,
                       count(*) filter (where g.score >= 3) as passed, count(*) filter (where g.score >= 4) as good
                  from grade g join subject s on s.id = g.subject_id
                  join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by s.id, s.name""", p(schoolId).addValue("from", periodStart(period)).addValue("to", today))) {
            long total = l(r.get("total"));
            rows.add(new SubjectRow(l(r.get("id")), (String) r.get("name"), (int) total,
                    pct(l(r.get("passed")), total), pct(l(r.get("good")), total)));
        }
        rows.sort(Comparator.comparing((SubjectRow x) -> x.quality() == null ? 101 : x.quality()).thenComparing(SubjectRow::subject));
        return rows;
    }

    // ------------------------------------------------------------------ 6.8 grade distribution (weeks)

    public List<GradeWeek> gradeDistribution(long schoolId) {
        LocalDate today = today();
        LocalDate monday = today.with(DayOfWeek.MONDAY);
        LocalDate prevMonday = monday.minusWeeks(1);
        Map<Boolean, Map<Integer, Integer>> counts = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select (g.grade_date >= :monday) as cur, least(greatest(g.score, 2), 5) as score, count(*) as n
                  from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by 1, 2""", p(schoolId).addValue("monday", monday).addValue("from", prevMonday).addValue("to", today))) {
            counts.computeIfAbsent(Boolean.TRUE.equals(r.get("cur")), k -> new HashMap<>()).put(i(r.get("score")), i(r.get("n")));
        }
        return List.of(week("Shu hafta", monday, today, counts.getOrDefault(true, Map.of())),
                week("O'tgan hafta", prevMonday, monday.minusDays(1), counts.getOrDefault(false, Map.of())));
    }

    static GradeWeek week(String label, LocalDate from, LocalDate to, Map<Integer, Integer> c) {
        Map<Integer, Integer> ordered = new LinkedHashMap<>();
        int total = 0;
        long points = 0;
        for (int s = 5; s >= 2; s--) {
            int n = c.getOrDefault(s, 0);
            ordered.put(s, n);
            total += n;
            points += (long) s * n;
        }
        Double avg = total == 0 ? null : Math.round(points * 100.0 / total) / 100.0;
        return new GradeWeek(label, from, to, ordered, total, avg, pct(ordered.get(5), total), pct(ordered.get(2), total));
    }

    // ------------------------------------------------------------------ 6.10 journal completeness

    /** Per teacher over the last 7 school days; the lowest first. */
    public Journal journal(long schoolId) {
        return journal(schoolId, null, schoolDays.lastSchoolDays(schoolId, today(), 7));
    }

    /**
     * Per teacher over the given school days (today only for lessons already over): lessons held,
     * lessons with attendance entered, lessons with at least one grade of that class and subject that
     * day. Entered = by a person or a device; in demo mode the simulator's marks count too (it plays
     * the teachers). AUTO_CARRY never counts. Completeness = (attended + graded) / (2 × lessons).
     */
    public Journal journal(long schoolId, Long employeeId, List<LocalDate> days) {
        if (days.isEmpty()) return new Journal(List.of(), null, 0);
        LocalDateTime now = LocalDateTime.now(clock);
        String dayArray = "{" + String.join(",", days.stream().map(LocalDate::toString).toList()) + "}";
        String sources = demo.enabled() ? "('MANUAL', 'DEVICE', 'DEMO_SIM')" : "('MANUAL', 'DEVICE')";
        List<TeacherRow> rows = new ArrayList<>();
        long lessonsAll = 0, filledAll = 0;
        for (Map<String, Object> r : jdbc.queryForList("""
                with days(d) as (select unnest(cast(:days as date[]))),
                occ as (
                  select ls.id, ls.employee_id, ls.class_id, ls.subject_id, days.d
                    from lesson_slot ls
                    join school_class c on c.id = ls.class_id join academic_year y on y.id = c.academic_year_id
                    join days on ls.weekday = """ + DashboardQueries.weekdayOf("days.d") + """
                   where y.school_id = :s and (cast(:emp as bigint) is null or ls.employee_id = :emp)
                     and exists (select 1 from students st where st.class_id = ls.class_id)
                     and (days.d < :today or ls.end_time <= :nowTime))
                select o.employee_id, trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as name,
                       count(*) as lessons,
                       count(*) filter (where exists (select 1 from attendance a where a.lesson_slot_id = o.id
                                 and a.record_date = o.d and a.source in """ + sources + """
                                 )) as attended,
                       count(*) filter (where exists (select 1 from grade g join students st on st.id = g.student_id
                                 where st.class_id = o.class_id and g.subject_id = o.subject_id and g.grade_date = o.d)) as graded
                  from occ o join employee e on e.id = o.employee_id
                 group by o.employee_id, e.first_name, e.last_name""",
                p(schoolId).addValue("days", dayArray).addValue("emp", employeeId, java.sql.Types.BIGINT)
                        .addValue("today", now.toLocalDate()).addValue("nowTime", now.toLocalTime()))) {
            int lessons = i(r.get("lessons"));
            int attended = i(r.get("attended"));
            int graded = i(r.get("graded"));
            lessonsAll += lessons;
            filledAll += attended + graded;
            rows.add(new TeacherRow(l(r.get("employee_id")), (String) r.get("name"), lessons, attended, graded,
                    pct(attended, lessons), pct(graded, lessons), pct(attended + graded, 2L * lessons)));
        }
        rows.sort(Comparator.comparing((TeacherRow t) -> t.completeness() == null ? 101 : t.completeness())
                .thenComparing(TeacherRow::name));
        return new Journal(rows, pct(filledAll, 2 * lessonsAll), days.size());
    }

    // ------------------------------------------------------------------ 6.10 parents and Telegram

    public ParentsTelegram parentsTelegram(long schoolId) {
        LocalDate monday = today().with(DayOfWeek.MONDAY);
        Map<String, Object> st = jdbc.queryForMap("""
                select count(*) as students,
                       count(*) filter (where exists (select 1 from parent_telegram_link l where l.student_id = st.id and l.active)) as linked
                  from students st join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s""", p(schoolId));
        Map<String, Object> n = jdbc.queryForMap("""
                select count(*) filter (where status <> 'SKIPPED') as sent,
                       count(*) filter (where status = 'SENT') as delivered
                  from notification_log where school_id = :s and created_at >= :from""",
                p(schoolId).addValue("from", Timestamp.valueOf(monday.atStartOfDay())));
        Integer downloads = jdbc.queryForObject("select count(*) from book_download where school_id = :s and downloaded_at >= :from",
                p(schoolId).addValue("from", Timestamp.valueOf(monday.atStartOfDay())), Integer.class);
        int students = i(st.get("students"));
        int linked = i(st.get("linked"));
        int sent = i(n.get("sent"));
        int delivered = i(n.get("delivered"));
        return new ParentsTelegram(students, linked, pct(linked, students), sent, delivered, pct(delivered, sent), downloads);
    }

    // ------------------------------------------------------------------ 6.10 events

    public List<EventRow> events(long schoolId, int limit) {
        LocalDate today = today();
        List<EventRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select id, title, type, start_date, end_date from calendar_event
                 where school_id = :s and end_date >= :today order by start_date limit :n""",
                p(schoolId).addValue("today", today).addValue("n", Math.max(1, Math.min(limit, 20))))) {
            LocalDate start = date(r.get("start_date"));
            rows.add(new EventRow(l(r.get("id")), (String) r.get("title"), (String) r.get("type"), start, date(r.get("end_date")),
                    Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, start))));
        }
        return rows;
    }

    // ------------------------------------------------------------------ attention (signals and the bell)

    /** The bell's groups: top 7 of each. */
    public List<AttentionGroup> attention(long schoolId, Set<Long> classIds, boolean withPhones) {
        return attention(schoolId, classIds, withPhones, 7);
    }

    /**
     * The things that need someone today, most urgent first; each group has its top {@code limit}
     * rows and the total. classIds limits it to a teacher's classes (null = the whole school).
     */
    public List<AttentionGroup> attention(long schoolId, Set<Long> classIds, boolean withPhones, int limit) {
        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        LocalDateTime nowDt = LocalDateTime.now(clock);
        LocalDate today = nowDt.toLocalDate();
        List<AttentionGroup> groups = new ArrayList<>();

        // lessons without attendance, escalationMinutes after they started
        List<AttentionItem> unmarked = new ArrayList<>();
        String weekday = SchoolDays.WEEKDAY_UZ.get(today.getDayOfWeek());
        if (weekday != null && schoolDays.isSchoolDay(schoolId, today)) {
            List<Map<String, Object>> lessons = jdbc.queryForList(LESSONS_OF_WEEKDAY, p(schoolId).addValue("weekday", weekday));
            Map<Long, Map<String, Object>> marks = new HashMap<>();
            for (Map<String, Object> r : jdbc.queryForList(MARKS_BY_LESSON, p(schoolId).addValue("date", today))) {
                marks.put(l(r.get("lesson_slot_id")), r);
            }
            Map<Long, List<LocalTime>> starts = new HashMap<>();
            for (Map<String, Object> r : lessons) starts.computeIfAbsent(l(r.get("class_id")), k -> new ArrayList<>()).add(time(r.get("start_time")));
            for (Map<String, Object> r : lessons) {
                long classId = l(r.get("class_id"));
                if (classIds != null && !classIds.contains(classId)) continue;
                LocalTime start = time(r.get("start_time"));
                if (nowDt.toLocalTime().isBefore(start.plusMinutes(s.escalationMinutes()))) continue;
                int students = i(r.get("students"));
                Map<String, Object> m = marks.get(l(r.get("id")));
                int marked = m == null ? 0 : i(m.get("marked"));
                if (students == 0 || marked >= students) continue;
                List<LocalTime> st = starts.get(classId).stream().sorted().distinct().toList();
                int number = st.indexOf(start) + 1;
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("lessonSlotId", l(r.get("id")));
                ref.put("classId", classId);
                ref.put("date", today.toString());
                unmarked.add(new AttentionItem("UNMARKED_LESSON", "red", r.get("class_name") + " · " + number + "-dars · " + r.get("subject"),
                        (String) r.get("teacher"), marked == 0 ? "olinmagan" : (students - marked) + " o'quvchi belgilanmagan", ref));
            }
        }
        groups.add(group("UNMARKED_LESSON", "red", "Davomat olinmagan darslar", unmarked, limit));

        // absent N+ school days in a row
        groups.add(group("ABSENT_STREAK", "red", s.absentStreakDays() + "+ kun kelmaganlar",
                absentStreaks(schoolId, classIds, s.absentStreakDays(), withPhones), limit));

        // appeals unanswered for overdueHours
        List<AttentionItem> appeals = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select a.id, a.parent_name, a.last_message_at, st.first_name, st.last_name, st.class_id,
                       c.grade_number || '-' || c.section_letter as class_name
                  from appeal a join students st on st.id = a.student_id join school_class c on c.id = st.class_id
                 where a.school_id = :s and a.status in ('NEW', 'SEEN') and a.last_message_at < :overdue
                 order by a.last_message_at""",
                p(schoolId).addValue("overdue", Timestamp.valueOf(nowDt.minusHours(s.appealOverdueHours()))))) {
            if (classIds != null && !classIds.contains(l(r.get("class_id")))) continue;
            long hours = Duration.between(((Timestamp) r.get("last_message_at")).toLocalDateTime(), nowDt).toHours();
            appeals.add(new AttentionItem("APPEAL_OVERDUE", "orange", "#" + r.get("id") + " · " + nz(r.get("parent_name"), "Ota-ona"),
                    r.get("first_name") + " " + r.get("last_name") + " · " + r.get("class_name"), hours + " soat javobsiz",
                    Map.of("appealId", l(r.get("id")))));
        }
        groups.add(group("APPEAL_OVERDUE", "orange", "Javobsiz murojaatlar (" + s.appealOverdueHours() + " soat+)", appeals, limit));

        // grades dropped: a student's average over the last 2 weeks (all subjects) is 0.8+ below the 4 weeks
        // before — each window with 4+ grades, so one bad mark is not a "drop"
        List<AttentionItem> drops = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select g.student_id, st.first_name, st.last_name, st.class_id, c.grade_number || '-' || c.section_letter as class_name,
                       avg(g.score) filter (where g.grade_date < :mid) as before, avg(g.score) filter (where g.grade_date >= :mid) as now,
                       count(*) filter (where g.grade_date >= :mid) as n
                  from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by g.student_id, st.first_name, st.last_name, st.class_id, c.grade_number, c.section_letter
                having count(*) filter (where g.grade_date < :mid) >= 4 and count(*) filter (where g.grade_date >= :mid) >= 4
                   and avg(g.score) filter (where g.grade_date < :mid) - avg(g.score) filter (where g.grade_date >= :mid) >= 0.8
                 order by avg(g.score) filter (where g.grade_date < :mid) - avg(g.score) filter (where g.grade_date >= :mid) desc""",
                p(schoolId).addValue("from", today.minusWeeks(6)).addValue("mid", today.minusWeeks(2)).addValue("to", today))) {
            if (classIds != null && !classIds.contains(l(r.get("class_id")))) continue;
            double before = ((Number) r.get("before")).doubleValue();
            double now = ((Number) r.get("now")).doubleValue();
            drops.add(new AttentionItem("GRADE_DROP", "orange", r.get("first_name") + " " + r.get("last_name"),
                    r.get("class_name") + " · oxirgi 2 haftada " + r.get("n") + " ta baho",
                    String.format(java.util.Locale.ROOT, "%.1f → %.1f", before, now),
                    Map.of("studentId", l(r.get("student_id")), "classId", l(r.get("class_id")))));
        }
        groups.add(group("GRADE_DROP", "orange", "Bahosi tushganlar", drops, limit));

        if (classIds == null) {
            // teachers who leave lessons unfilled
            List<AttentionItem> lazy = new ArrayList<>();
            for (TeacherRow t : journal(schoolId).teachers()) {
                if (t.lessons() == 0 || t.completeness() == null) continue;
                double missing = 100 - t.completeness();
                if (missing < s.disciplineThreshold()) continue;
                lazy.add(new AttentionItem("TEACHER_DISCIPLINE", "yellow", t.name(),
                        "davomat " + fmt(t.attendedPct()) + " · baho " + fmt(t.gradedPct()),
                        fmt(t.completeness()) + " to'liq", Map.of("employeeId", t.employeeId())));
            }
            groups.add(group("TEACHER_DISCIPLINE", "yellow", "Jurnali to'liq bo'lmagan o'qituvchilar", lazy, limit));

            // classes where few parents joined the bot
            List<AttentionItem> tg = new ArrayList<>();
            for (Map<String, Object> r : jdbc.queryForList("""
                    select c.id, c.grade_number || '-' || c.section_letter as class_name, count(st.id) as students,
                           count(st.id) filter (where exists (select 1 from parent_telegram_link l where l.student_id = st.id and l.active)) as linked
                      from school_class c join academic_year y on y.id = c.academic_year_id
                      join students st on st.class_id = c.id
                     where y.school_id = :s
                     group by c.id, c.grade_number, c.section_letter
                     order by c.grade_number, c.section_letter""", p(schoolId))) {
                Double pc = pct(i(r.get("linked")), i(r.get("students")));
                if (pc == null || pc >= s.telegramLowPercent()) continue;
                tg.add(new AttentionItem("TELEGRAM_LOW", "yellow", (String) r.get("class_name"),
                        r.get("linked") + "/" + r.get("students") + " ota-ona ulangan", fmt(pc), Map.of("classId", l(r.get("id")))));
            }
            tg.sort(Comparator.comparing(a -> Double.parseDouble(a.detail().replace("%", "").replace(",", "."))));
            groups.add(group("TELEGRAM_LOW", "yellow", "Telegramga kam ulangan sinflar", tg, limit));

            // library permissions that ended or end within 30 days
            List<AttentionItem> lib = new ArrayList<>();
            for (uz.azizbek.maktabboshqaruv.dto.AttentionItemDto a : libraryPermissions.attention(schoolId)) {
                lib.add(new AttentionItem(a.getType(), "yellow", a.getDescription(), null, null,
                        Map.of("permissionId", a.getLinkId())));
            }
            groups.add(group("LIBRARY_PERMISSION", "yellow", "Kutubxona ruxsatnomalari", lib, limit));
        }
        return groups;
    }

    /** Students absent on each of the last N+ school days (Sunday and holidays do not break a run). */
    List<AttentionItem> absentStreaks(long schoolId, Set<Long> classIds, int minDays, boolean withPhones) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, 15);
        if (days.isEmpty()) return List.of();
        // per student: the latest-lesson status of each day
        Map<Long, Map<LocalDate, String>> byStudent = new HashMap<>();
        Map<Long, Map<String, Object>> info = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("with " + DAY_CTE + """
                select day.d, day.student_id, day.status, st.first_name, st.last_name, st.guardian_phone, st.class_id,
                       c.grade_number || '-' || c.section_letter as class_name
                  from day join students st on st.id = day.student_id join school_class c on c.id = st.class_id""",
                p(schoolId).addValue("from", days.get(0)).addValue("to", today))) {
            long id = l(r.get("student_id"));
            if (classIds != null && !classIds.contains(l(r.get("class_id")))) continue;
            byStudent.computeIfAbsent(id, k -> new HashMap<>()).put(date(r.get("d")), (String) r.get("status"));
            info.putIfAbsent(id, r);
        }
        List<AttentionItem> out = new ArrayList<>();
        for (Map.Entry<Long, Map<LocalDate, String>> e : byStudent.entrySet()) {
            Map<LocalDate, String> st = e.getValue();
            int k = days.size() - 1;
            // today may not be marked yet: start from the latest school day the student has a mark
            while (k >= 0 && !st.containsKey(days.get(k))) {
                if (days.get(k).equals(today)) k--;
                else break;
            }
            int streak = 0;
            for (; k >= 0; k--) {
                if ("ABSENT".equals(st.get(days.get(k)))) streak++;
                else break;
            }
            if (streak < minDays) continue;
            Map<String, Object> r = info.get(e.getKey());
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("studentId", e.getKey());
            ref.put("classId", l(r.get("class_id")));
            if (withPhones && r.get("guardian_phone") != null) ref.put("phone", r.get("guardian_phone"));
            out.add(new AttentionItem("ABSENT_STREAK", "red", r.get("first_name") + " " + r.get("last_name"),
                    (String) r.get("class_name"), streak + " kun", ref));
        }
        out.sort(Comparator.comparing((AttentionItem a) -> -Integer.parseInt(a.detail().split(" ")[0])).thenComparing(AttentionItem::title));
        return out;
    }

    private static AttentionGroup group(String kind, String level, String title, List<AttentionItem> items, int limit) {
        return new AttentionGroup(kind, level, title, items.size(), items.subList(0, Math.min(items.size(), limit)));
    }

    static String fmt(Double v) {
        return v == null ? "—" : (v % 1 == 0 ? String.valueOf(v.intValue()) : String.valueOf(v)) + "%";
    }

    private static String nz(Object o, String fallback) {
        return o == null || o.toString().isBlank() ? fallback : o.toString();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
