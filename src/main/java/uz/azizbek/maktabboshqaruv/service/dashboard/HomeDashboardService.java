package uz.azizbek.maktabboshqaruv.service.dashboard;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceSettingsService;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static uz.azizbek.maktabboshqaruv.service.dashboard.DashboardQueries.*;

/**
 * The home page of a school ("qaror qabul qilish markazi"): every widget answers one question
 * with the school's real data. Percentages have one decimal; null means "no data yet" (never 0).
 */
@Service
@Transactional(readOnly = true)
public class HomeDashboardService {

    // ------------------------------------------------------------------ result shapes

    /** One KPI card: value, change against the previous period, 14 school-day sparkline. */
    public record Kpi(String key, Double value, Double delta, boolean higherIsBetter, List<Double> spark,
                      Map<String, Object> extra) {
    }

    public record DailyPoint(LocalDate date, Double rate, Integer absent, Integer marked) {
    }

    public record MapTile(long classId, String name, int grade, String teacher, int students, int present, int late,
                          int absent, int excused, int marked, Double rate, String source, String takenBy,
                          LocalDateTime takenAt, boolean hasLessons) {
    }

    public record TodayMap(List<MapTile> classes, Map<String, Integer> ring, Double rate,
                           Map<String, Integer> sources, Map<String, Integer> thresholds) {
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

    public record Trend(List<DailyPoint> points, int target) {
    }

    public record RankingRow(long classId, String name, String teacher, Double attendance, Double attendanceDelta,
                             Double mastery, Double masteryDelta, Double quality, Double qualityDelta) {
    }

    public record SubjectRow(long subjectId, String subject, int total, Double mastery, Double quality) {
    }

    public record GradeWeek(String label, LocalDate from, LocalDate to, Map<Integer, Integer> counts, int total) {
    }

    public record TeacherRow(long employeeId, String name, int lessons, int attended, int graded,
                             Double attendedPct, Double gradedPct) {
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
    private final uz.azizbek.maktabboshqaruv.service.library.LibraryPermissions libraryPermissions;

    public HomeDashboardService(NamedParameterJdbcTemplate jdbc, SchoolDays schoolDays,
                                AttendanceSettingsService settings, Clock clock,
                                uz.azizbek.maktabboshqaruv.service.library.LibraryPermissions libraryPermissions) {
        this.libraryPermissions = libraryPermissions;
        this.jdbc = jdbc;
        this.schoolDays = schoolDays;
        this.settings = settings;
        this.clock = clock;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    static Double pct(long part, long whole) {
        return whole == 0 ? null : Math.round(part * 1000.0 / whole) / 10.0;
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

    private static LocalDate date(Object o) {
        return o instanceof Date d ? d.toLocalDate() : (LocalDate) o;
    }

    private static LocalTime time(Object o) {
        return o instanceof java.sql.Time t ? t.toLocalTime() : (LocalTime) o;
    }

    private Map<LocalDate, Map<String, Object>> dailyAttendance(long schoolId, LocalDate from, LocalDate to) {
        Map<LocalDate, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(DAILY, p(schoolId).addValue("from", from).addValue("to", to))) {
            byDate.put(date(r.get("d")), r);
        }
        return byDate;
    }

    // ------------------------------------------------------------------ 6.1 KPI

    public List<Kpi> kpis(long schoolId) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, 15);
        LocalDate from = days.get(0);
        Map<LocalDate, Map<String, Object>> att = dailyAttendance(schoolId, from, today);
        List<LocalDate> spark = days.subList(Math.max(0, days.size() - 14), days.size());
        LocalDate prevDay = days.size() >= 2 ? days.get(days.size() - 2) : null;
        boolean todayIsSchoolDay = !days.isEmpty() && days.get(days.size() - 1).equals(today);

        Map<String, Object> t = todayIsSchoolDay ? att.getOrDefault(today, Map.of()) : Map.of();
        Map<String, Object> y = prevDay == null ? Map.of() : att.getOrDefault(prevDay, Map.of());
        long classesTotal = jdbc.queryForObject("select count(*) from school_class c join academic_year y on y.id = c.academic_year_id where y.school_id = :s", p(schoolId), Long.class);

        List<Kpi> out = new ArrayList<>();
        Double rateToday = pct(i(t.get("present")), i(t.get("marked")));
        Double rateYesterday = pct(i(y.get("present")), i(y.get("marked")));
        Map<String, Object> e1 = new LinkedHashMap<>();
        e1.put("classesTaken", i(t.get("classes")));
        e1.put("classesTotal", classesTotal);
        e1.put("autoClasses", i(t.get("auto_classes")));
        e1.put("schoolDay", todayIsSchoolDay);
        out.add(new Kpi("attendance", rateToday, minus(rateToday, rateYesterday), true,
                spark.stream().map(d -> { var r = att.get(d); return r == null ? null : pct(i(r.get("present")), i(r.get("marked"))); }).toList(), e1));

        Integer absentToday = t.isEmpty() ? null : i(t.get("absent"));
        Map<String, Object> e2 = new LinkedHashMap<>();
        e2.put("excused", t.isEmpty() ? null : i(t.get("excused")));
        out.add(new Kpi("absent", absentToday == null ? null : absentToday.doubleValue(),
                absentToday == null || y.isEmpty() ? null : (double) (absentToday - i(y.get("absent"))), false,
                spark.stream().map(d -> { var r = att.get(d); return r == null ? null : (double) i(r.get("absent")); }).toList(), e2));

        Integer lateToday = t.isEmpty() ? null : i(t.get("late"));
        Map<String, Object> e3 = new LinkedHashMap<>();
        Object avgLate = t.get("avg_late");
        e3.put("avgMinutes", avgLate == null ? null : Math.round(((Number) avgLate).doubleValue()));
        out.add(new Kpi("late", lateToday == null ? null : lateToday.doubleValue(),
                lateToday == null || y.isEmpty() ? null : (double) (lateToday - i(y.get("late"))), false,
                spark.stream().map(d -> { var r = att.get(d); return r == null ? null : (double) i(r.get("late")); }).toList(), e3));

        // grades: this quarter for the value, last 7 vs previous 7 school days for the change
        LocalDate[] quarter = ParentStats.quarterRange(today, ParentStats.quarterOf(today));
        LocalDate gFrom = quarter[0].isBefore(days.get(0)) ? quarter[0] : days.get(0);
        Map<LocalDate, long[]> grades = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(GRADES_DAILY, p(schoolId).addValue("from", gFrom).addValue("to", today))) {
            grades.put(date(r.get("d")), new long[]{l(r.get("total")), l(r.get("passed")), l(r.get("good"))});
        }
        long[] q = sum(grades, d -> !d.isBefore(quarter[0]) && !d.isAfter(today));
        List<LocalDate> last7 = days.subList(Math.max(0, days.size() - 7), days.size());
        List<LocalDate> prev7 = days.subList(Math.max(0, days.size() - 14), Math.max(0, days.size() - 7));
        long[] g7 = sum(grades, last7::contains);
        long[] gp7 = sum(grades, prev7::contains);
        Map<String, Object> eq = Map.of("quarter", ParentStats.quarterOf(today), "grades", q[0]);
        out.add(new Kpi("mastery", pct(q[1], q[0]), minus(pct(g7[1], g7[0]), pct(gp7[1], gp7[0])), true,
                spark.stream().map(d -> { long[] v = grades.get(d); return v == null ? null : pct(v[1], v[0]); }).toList(), eq));
        out.add(new Kpi("quality", pct(q[2], q[0]), minus(pct(g7[2], g7[0]), pct(gp7[2], gp7[0])), true,
                spark.stream().map(d -> { long[] v = grades.get(d); return v == null ? null : pct(v[2], v[0]); }).toList(), eq));

        // appeals waiting for an answer
        int overdueHours = settings.forSchool(schoolId).appealOverdueHours();
        Map<String, Object> a = jdbc.queryForMap("""
                select count(*) filter (where status in ('NEW', 'SEEN')) as pending,
                       count(*) filter (where status in ('NEW', 'SEEN') and last_message_at < :overdue) as overdue
                  from appeal where school_id = :s""",
                p(schoolId).addValue("overdue", Timestamp.valueOf(LocalDateTime.now(clock).minusHours(overdueHours))));
        Map<LocalDate, Integer> newPerDay = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select cast(created_at as date) as d, count(*) as n from appeal
                 where school_id = :s and status <> 'DRAFT' and created_at >= :from group by 1""",
                p(schoolId).addValue("from", Timestamp.valueOf(spark.get(0).atStartOfDay())))) {
            newPerDay.put(date(r.get("d")), i(r.get("n")));
        }
        Map<String, Object> ea = new LinkedHashMap<>();
        ea.put("overdue", i(a.get("overdue")));
        ea.put("overdueHours", overdueHours);
        ea.put("sparkMeaning", "new");
        out.add(new Kpi("appeals", (double) i(a.get("pending")), null, false,
                spark.stream().map(d -> (double) newPerDay.getOrDefault(d, 0)).toList(), ea));
        return out;
    }

    private static long[] sum(Map<LocalDate, long[]> grades, Predicate<LocalDate> in) {
        long[] s = new long[3];
        grades.forEach((d, v) -> {
            if (in.test(d)) for (int k = 0; k < 3; k++) s[k] += v[k];
        });
        return s;
    }

    // ------------------------------------------------------------------ 6.2 today map

    public TodayMap todayMap(long schoolId) {
        LocalDate today = today();
        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        Map<Long, Map<String, Object>> classes = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(CLASSES, p(schoolId))) classes.put(l(r.get("id")), r);

        Map<Long, int[]> counts = new HashMap<>(); // present, late, absent, excused, auto, demo, manual
        Map<String, Integer> sources = new LinkedHashMap<>(Map.of("MANUAL", 0, "DEVICE", 0, "AUTO_CARRY", 0, "DEMO_SIM", 0));
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
            sources.merge(src, n, Integer::sum);
            if (src.equals("AUTO_CARRY")) c[4] += n;
            else if (src.equals("DEMO_SIM")) c[5] += n;
            else c[6] += n;
        }
        // who took it and when: the latest MANUAL mark of the class today
        Map<Long, Object[]> taken = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select distinct on (ls.class_id) ls.class_id, a.created_by, a.created_date
                  from attendance a join lesson_slot ls on ls.id = a.lesson_slot_id
                  join school_class c on c.id = ls.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and a.record_date = :d and a.source = 'MANUAL'
                 order by ls.class_id, a.created_date desc""", p(schoolId).addValue("d", today))) {
            taken.put(l(r.get("class_id")), new Object[]{r.get("created_by"), r.get("created_date")});
        }
        // classes that have lessons today (the rest are not "olinmagan", they simply have no lessons)
        String weekday = SchoolDays.WEEKDAY_UZ.get(today.getDayOfWeek());
        java.util.Set<Long> withLessons = new java.util.HashSet<>(weekday == null || !schoolDays.isSchoolDay(schoolId, today) ? List.of()
                : jdbc.queryForList("select distinct ls.class_id from lesson_slot ls join school_class c on c.id = ls.class_id "
                + "join academic_year y on y.id = c.academic_year_id where y.school_id = :s and ls.weekday = :w",
                p(schoolId).addValue("w", weekday), Long.class));

        List<MapTile> tiles = new ArrayList<>();
        int[] ring = new int[5];
        for (Map.Entry<Long, Map<String, Object>> e : classes.entrySet()) {
            Map<String, Object> r = e.getValue();
            int[] c = counts.getOrDefault(e.getKey(), new int[7]);
            int marked = c[0] + c[1] + c[2] + c[3];
            int students = i(r.get("students"));
            String source = marked == 0 ? null : c[6] > 0 ? (c[4] + c[5] > 0 ? "MIXED" : "MANUAL") : (c[5] > 0 ? "DEMO_SIM" : "AUTO_CARRY");
            Object[] tk = taken.get(e.getKey());
            boolean lessons = withLessons.contains(e.getKey());
            tiles.add(new MapTile(e.getKey(), i(r.get("grade_number")) + "-" + r.get("section_letter"), i(r.get("grade_number")),
                    blankToNull((String) r.get("teacher")), students, c[0], c[1], c[2], c[3], marked, pct(c[0] + c[1], marked),
                    source, tk == null ? null : (String) tk[0], tk == null ? null : ((Timestamp) tk[1]).toLocalDateTime(), lessons));
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
        int marked = ring[0] + ring[1] + ring[2] + ring[3];
        return new TodayMap(tiles, ringMap, pct(ring[0] + ring[1], marked), sources,
                Map.of("good", s.heatGood(), "ok", s.heatOk(), "warn", s.heatWarn()));
    }

    // ------------------------------------------------------------------ 6.4 live lessons

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
            return new Live("BEFORE", null, null, null, null, 1, periods.get(0)[0], List.of(), Map.of());
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
        java.util.Set<String> seen = new java.util.HashSet<>();
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
        return new LiveLesson(l(r.get("id")), l(r.get("class_id")), (String) r.get("class_name"), (String) r.get("subject"),
                (String) r.get("teacher"), (String) r.get("room_number"), state, marked, i(r.get("students")));
    }

    // ------------------------------------------------------------------ 6.5 trend

    public Trend trend(long schoolId, int count) {
        LocalDate today = today();
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, Math.max(5, Math.min(count, 90)));
        Map<LocalDate, Map<String, Object>> att = dailyAttendance(schoolId, days.get(0), today);
        List<DailyPoint> points = new ArrayList<>();
        for (LocalDate d : days) {
            Map<String, Object> r = att.get(d);
            points.add(r == null ? new DailyPoint(d, null, null, null)
                    : new DailyPoint(d, pct(i(r.get("present")), i(r.get("marked"))), i(r.get("absent")), i(r.get("marked"))));
        }
        return new Trend(points, settings.forSchool(schoolId).attendanceTarget());
    }

    // ------------------------------------------------------------------ 6.6 class ranking (this month)

    public List<RankingRow> classRanking(long schoolId) {
        LocalDate today = today();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate prevStart = monthStart.minusMonths(1);
        Map<Long, Map<String, Object>> classes = new LinkedHashMap<>();
        for (Map<String, Object> r : jdbc.queryForList(CLASSES, p(schoolId))) classes.put(l(r.get("id")), r);

        Map<Long, long[]> att = new HashMap<>(); // present, marked, prevPresent, prevMarked
        for (Map<String, Object> r : jdbc.queryForList("with " + DAY_CTE + """
                select class_id, (d >= :month) as cur,
                       count(*) filter (where status in ('PRESENT', 'LATE')) as present, count(*) as marked
                  from day group by 1, 2""",
                p(schoolId).addValue("from", prevStart).addValue("to", today).addValue("month", monthStart))) {
            long[] v = att.computeIfAbsent(l(r.get("class_id")), k -> new long[4]);
            int off = Boolean.TRUE.equals(r.get("cur")) ? 0 : 2;
            v[off] = l(r.get("present"));
            v[off + 1] = l(r.get("marked"));
        }
        Map<Long, long[]> gr = new HashMap<>(); // total, passed, good, prevTotal, prevPassed, prevGood
        for (Map<String, Object> r : jdbc.queryForList("""
                select st.class_id, (g.grade_date >= :month) as cur, count(*) as total,
                       count(*) filter (where g.score >= 3) as passed, count(*) filter (where g.score >= 4) as good
                  from grade g join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by 1, 2""",
                p(schoolId).addValue("from", prevStart).addValue("to", today).addValue("month", monthStart))) {
            long[] v = gr.computeIfAbsent(l(r.get("class_id")), k -> new long[6]);
            int off = Boolean.TRUE.equals(r.get("cur")) ? 0 : 3;
            v[off] = l(r.get("total"));
            v[off + 1] = l(r.get("passed"));
            v[off + 2] = l(r.get("good"));
        }
        List<RankingRow> rows = new ArrayList<>();
        for (Map.Entry<Long, Map<String, Object>> e : classes.entrySet()) {
            long[] a = att.getOrDefault(e.getKey(), new long[4]);
            long[] g = gr.getOrDefault(e.getKey(), new long[6]);
            Double attNow = pct(a[0], a[1]);
            Double mNow = pct(g[1], g[0]);
            Double qNow = pct(g[2], g[0]);
            Map<String, Object> r = e.getValue();
            rows.add(new RankingRow(e.getKey(), i(r.get("grade_number")) + "-" + r.get("section_letter"),
                    blankToNull((String) r.get("teacher")), attNow, minus(attNow, pct(a[2], a[3])),
                    mNow, minus(mNow, pct(g[4], g[3])), qNow, minus(qNow, pct(g[5], g[3]))));
        }
        return rows;
    }

    // ------------------------------------------------------------------ 6.7 subjects (this quarter)

    public List<SubjectRow> subjectQuality(long schoolId) {
        LocalDate today = today();
        LocalDate[] q = ParentStats.quarterRange(today, ParentStats.quarterOf(today));
        List<SubjectRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select s.id, s.name, count(*) as total,
                       count(*) filter (where g.score >= 3) as passed, count(*) filter (where g.score >= 4) as good
                  from grade g join subject s on s.id = g.subject_id
                  join students st on st.id = g.student_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by s.id, s.name""", p(schoolId).addValue("from", q[0]).addValue("to", today))) {
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

    private static GradeWeek week(String label, LocalDate from, LocalDate to, Map<Integer, Integer> c) {
        Map<Integer, Integer> ordered = new LinkedHashMap<>();
        int total = 0;
        for (int s = 5; s >= 2; s--) {
            ordered.put(s, c.getOrDefault(s, 0));
            total += c.getOrDefault(s, 0);
        }
        return new GradeWeek(label, from, to, ordered, total);
    }

    // ------------------------------------------------------------------ 6.9 teacher discipline (7 school days)

    /**
     * Per teacher over the last 7 school days (today only for lessons already over): lessons held,
     * lessons with attendance a person or device took (AUTO_CARRY / DEMO_SIM do not count), lessons
     * with at least one grade of that class and subject on that day.
     */
    public List<TeacherRow> teacherDiscipline(long schoolId, Long employeeId) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, now.toLocalDate(), 7);
        if (days.isEmpty()) return List.of();
        String dayArray = "{" + String.join(",", days.stream().map(LocalDate::toString).toList()) + "}";
        List<TeacherRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                with days(d) as (select unnest(cast(:days as date[]))),
                occ as (
                  select ls.id, ls.employee_id, ls.class_id, ls.subject_id, days.d
                    from lesson_slot ls
                    join school_class c on c.id = ls.class_id join academic_year y on y.id = c.academic_year_id
                    join days on ls.weekday = """ + DashboardQueries.weekdayOf("days.d") + """
                   where y.school_id = :s and (:emp is null or ls.employee_id = :emp)
                     and (days.d < :today or ls.end_time <= :nowTime))
                select o.employee_id, trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as name,
                       count(*) as lessons,
                       count(*) filter (where exists (select 1 from attendance a where a.lesson_slot_id = o.id
                                 and a.record_date = o.d and a.source in ('MANUAL', 'DEVICE'))) as attended,
                       count(*) filter (where exists (select 1 from grade g join students st on st.id = g.student_id
                                 where st.class_id = o.class_id and g.subject_id = o.subject_id and g.grade_date = o.d)) as graded
                  from occ o join employee e on e.id = o.employee_id
                 group by o.employee_id, e.first_name, e.last_name""",
                p(schoolId).addValue("days", dayArray).addValue("emp", employeeId, java.sql.Types.BIGINT)
                        .addValue("today", now.toLocalDate()).addValue("nowTime", now.toLocalTime()))) {
            int lessons = i(r.get("lessons"));
            int attended = i(r.get("attended"));
            int graded = i(r.get("graded"));
            rows.add(new TeacherRow(l(r.get("employee_id")), (String) r.get("name"), lessons, attended, graded,
                    pct(attended, lessons), pct(graded, lessons)));
        }
        rows.sort(Comparator.comparing((TeacherRow t) -> t.attendedPct() == null ? 101 : t.attendedPct())
                .thenComparing(t -> t.gradedPct() == null ? 101 : t.gradedPct()));
        return rows;
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

    // ------------------------------------------------------------------ 6.11 events

    public List<EventRow> events(long schoolId) {
        LocalDate today = today();
        List<EventRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select id, title, type, start_date, end_date from calendar_event
                 where school_id = :s and end_date >= :today order by start_date limit 5""",
                p(schoolId).addValue("today", today))) {
            LocalDate start = date(r.get("start_date"));
            rows.add(new EventRow(l(r.get("id")), (String) r.get("title"), (String) r.get("type"), start, date(r.get("end_date")),
                    Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(today, start))));
        }
        return rows;
    }

    // ------------------------------------------------------------------ 6.3 attention

    /**
     * The things that need someone today, most urgent first; each group has its top rows and the
     * total. classIds limits it to a teacher's classes (null = the whole school).
     */
    public List<AttentionGroup> attention(long schoolId, java.util.Set<Long> classIds, boolean withPhones) {
        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        LocalDateTime nowDt = LocalDateTime.now(clock);
        LocalDate today = nowDt.toLocalDate();
        List<AttentionGroup> groups = new ArrayList<>();

        // 🔴 lessons without attendance, escalationMinutes after they started
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
        groups.add(group("UNMARKED_LESSON", "red", "Davomat olinmagan darslar", unmarked));

        // 🔴 absent N+ school days in a row
        groups.add(group("ABSENT_STREAK", "red", s.absentStreakDays() + "+ kun ketma-ket kelmaganlar",
                absentStreaks(schoolId, classIds, s.absentStreakDays(), withPhones)));

        // 🟠 appeals unanswered for overdueHours
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
        groups.add(group("APPEAL_OVERDUE", "orange", "Javob berilmagan murojaatlar", appeals));

        // 🟠 grades dropped: last 2 weeks average ≥ 1.0 below the 4 weeks before
        List<AttentionItem> drops = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select g.student_id, st.first_name, st.last_name, st.class_id, c.grade_number || '-' || c.section_letter as class_name,
                       s.id as subject_id, s.name as subject,
                       avg(g.score) filter (where g.grade_date < :mid) as before, avg(g.score) filter (where g.grade_date >= :mid) as now
                  from grade g join students st on st.id = g.student_id join subject s on s.id = g.subject_id
                  join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :from and :to
                 group by g.student_id, st.first_name, st.last_name, st.class_id, c.grade_number, c.section_letter, s.id, s.name
                having count(*) filter (where g.grade_date < :mid) >= 2 and count(*) filter (where g.grade_date >= :mid) >= 2
                   and avg(g.score) filter (where g.grade_date < :mid) - avg(g.score) filter (where g.grade_date >= :mid) >= 1.0
                 order by avg(g.score) filter (where g.grade_date < :mid) - avg(g.score) filter (where g.grade_date >= :mid) desc""",
                p(schoolId).addValue("from", today.minusWeeks(6)).addValue("mid", today.minusWeeks(2)).addValue("to", today))) {
            if (classIds != null && !classIds.contains(l(r.get("class_id")))) continue;
            double before = ((Number) r.get("before")).doubleValue();
            double now = ((Number) r.get("now")).doubleValue();
            drops.add(new AttentionItem("GRADE_DROP", "orange", r.get("first_name") + " " + r.get("last_name"),
                    r.get("class_name") + " · " + r.get("subject"),
                    String.format(java.util.Locale.ROOT, "%.1f → %.1f", before, now),
                    Map.of("studentId", l(r.get("student_id")), "subjectId", l(r.get("subject_id")), "classId", l(r.get("class_id")))));
        }
        groups.add(group("GRADE_DROP", "orange", "Bahosi keskin tushganlar", drops));

        if (classIds == null) {
            // 🟡 teachers who leave lessons unmarked
            List<AttentionItem> lazy = new ArrayList<>();
            for (TeacherRow t : teacherDiscipline(schoolId, null)) {
                if (t.lessons() == 0) continue;
                int missing = t.lessons() - Math.min(t.attended(), t.graded());
                double missingPct = missing * 100.0 / t.lessons();
                if (missingPct < s.disciplineThreshold()) continue;
                lazy.add(new AttentionItem("TEACHER_DISCIPLINE", "yellow", t.name(),
                        "davomat " + fmt(t.attendedPct()) + " · baho " + fmt(t.gradedPct()),
                        Math.round(missingPct) + "% darslar to'ldirilmagan", Map.of("employeeId", t.employeeId())));
            }
            groups.add(group("TEACHER_DISCIPLINE", "yellow", "Jurnalni to'ldirmayotgan o'qituvchilar", lazy));

            // 🟡 classes where few parents joined the bot
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
            groups.add(group("TELEGRAM_LOW", "yellow", "Telegramga kam ulangan sinflar", tg));

            // 🟡 library permissions that ended or end within 30 days
            List<AttentionItem> lib = new ArrayList<>();
            for (uz.azizbek.maktabboshqaruv.dto.AttentionItemDto a : libraryPermissions.attention(schoolId)) {
                lib.add(new AttentionItem(a.getType(), "yellow", a.getDescription(), null, null,
                        Map.of("permissionId", a.getLinkId())));
            }
            groups.add(group("LIBRARY_PERMISSION", "yellow", "Kutubxona ruxsatnomalari", lib));
        }
        return groups;
    }

    /** Students absent on each of the last N+ school days (Sunday and holidays do not break a run). */
    List<AttentionItem> absentStreaks(long schoolId, java.util.Set<Long> classIds, int minDays, boolean withPhones) {
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

    private static AttentionGroup group(String kind, String level, String title, List<AttentionItem> items) {
        return new AttentionGroup(kind, level, title, items.size(), items.subList(0, Math.min(items.size(), 7)));
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
