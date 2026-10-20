package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.AcademicTerm;
import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.repository.AcademicTermRepository;
import uz.azizbek.maktabboshqaruv.repository.AcademicYearRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardMetrics;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * What the five list pages show beyond their rows: the hero numbers of O'quvchilar and Xodimlar,
 * the side panels' extras (a student's 30 school days and risk, a teacher's week and journal run,
 * a subject's teachers, classes and grades) and the school-year cards (status, progress through the
 * school days, terms, students, attendance and quality). Shared, costly parts come from the
 * 90-second {@link DashboardCache}.
 */
@Service
public class ListPagesService {

    /** A hero number: the KpiCard's value, its change and its run. */
    public record Kpi(String key, Double value, Double delta, List<Double> spark, String note) {
    }

    public record DayDot(LocalDate date, String status) {
    }

    public record StudentExtra(Integer risk, Integer absentDays, Double avgBefore, Double avgNow, Integer twos,
                               DashboardMetrics.Risk parts, boolean telegram, List<DayDot> days) {
    }

    public record EmployeeExtra(List<String> weekdays, int lessonsPerDay, int[][] heat, int weeklyLessons,
                                List<LocalDate> journalDays, List<Double> journal, Double journal7) {
    }

    public record ClassRow(long classId, String name, int lessons, Double quality, Double average, int grades) {
    }

    public record SubjectExtra(List<ListStatsService.Person> teachers, List<ClassRow> classes,
                               Map<Integer, Integer> distribution, String window, Double quality, Double qualityPrev) {
    }

    public record Term(Long id, String name, String kind, LocalDate startDate, LocalDate endDate) {
    }

    public record YearCard(long id, String title, LocalDate startDate, LocalDate endDate, boolean current, String status,
                           int workDays, int passedDays, int leftDays, Double progress, int classes, int students,
                           Double attendance, Double quality, List<Term> terms) {
    }

    public record MoveResult(int moved, String className) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final HomeDashboardService home;
    private final ListStatsService stats;
    private final SchoolDays schoolDays;
    private final DashboardCache cache;
    private final Clock clock;
    private final AcademicYearRepository years;
    private final AcademicTermRepository terms;
    private final StudentRepository students;
    private final SchoolClassRepository classes;

    public ListPagesService(NamedParameterJdbcTemplate jdbc, HomeDashboardService home, ListStatsService stats,
                            SchoolDays schoolDays, DashboardCache cache, Clock clock, AcademicYearRepository years,
                            AcademicTermRepository terms, StudentRepository students, SchoolClassRepository classes) {
        this.jdbc = jdbc;
        this.home = home;
        this.stats = stats;
        this.schoolDays = schoolDays;
        this.cache = cache;
        this.clock = clock;
        this.years = years;
        this.terms = terms;
        this.students = students;
        this.classes = classes;
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    // ------------------------------------------------------------------ O'quvchilar

    /** Students, today's attendance (with its 14-day run), at risk, parents in Telegram. */
    public List<Kpi> studentsHero(long schoolId) {
        Long total = jdbc.queryForObject("""
                select count(*) from students st join school_class c on c.id = st.class_id
                  join academic_year y on y.id = c.academic_year_id where y.school_id = :s""", p(schoolId), Long.class);
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today(), 14);
        Map<LocalDate, HomeDashboardService.DayRate> rates = days.isEmpty() ? Map.of()
                : home.dailyRates(schoolId, days.get(0), days.get(days.size() - 1));
        List<Double> run = days.stream().map(d -> rates.get(d) == null ? null : rates.get(d).rate()).toList();
        // today before the first mark: the last school day that has marks, and the note says which
        int k = run.size() - 1;
        while (k >= 0 && run.get(k) == null) k--;
        Double now = k < 0 ? null : run.get(k);
        Double before = null;
        for (int j = k - 1; j >= 0 && before == null; j--) before = run.get(j);
        String todayNote = k == run.size() - 1 ? "bugun keldi"
                : k < 0 ? "hali belgilanmagan" : "oxirgi ish kuni, " + days.get(k).getDayOfMonth() + "." + String.format("%02d", days.get(k).getMonthValue());
        int atRisk = stats.riskScores(schoolId).size();
        HomeDashboardService.ParentsTelegram tg = cache.get(schoolId, "lists:telegram", () -> home.parentsTelegram(schoolId)).data();
        return List.of(
                new Kpi("students", total == null ? 0.0 : total, null, List.of(), "maktabda"),
                new Kpi("today", now, now == null || before == null ? null : round1(now - before), run, todayNote),
                new Kpi("risk", (double) atRisk, null, List.of(), "xavf balli 40 dan"),
                new Kpi("telegram", tg.linkedPct(), null, List.of(), tg.linked() + " / " + tg.students() + " ota-ona"));
    }

    /**
     * A student's risk (when on the school's risk list) and the last 30 days that were school days,
     * each with the day's worst mark (absent, late, excused, present) or none.
     */
    public StudentExtra studentExtra(long studentId) {
        Map<String, Object> st = jdbc.queryForMap("""
                select y.school_id, exists (select 1 from parent_telegram_link l where l.student_id = s.id and l.active) as tg
                  from students s join school_class c on c.id = s.class_id join academic_year y on y.id = c.academic_year_id
                 where s.id = :id""", new MapSqlParameterSource("id", studentId));
        long schoolId = l(st.get("school_id"));
        LocalDate today = today();
        LocalDate from = today.minusDays(29);
        Predicate<LocalDate> isSchool = schoolDays.predicate(schoolId, from, today);
        Map<LocalDate, String> worst = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select record_date, string_agg(distinct status, ',') as statuses from attendance
                 where student_id = :id and record_date between :f and :t group by record_date""",
                new MapSqlParameterSource("id", studentId).addValue("f", Date.valueOf(from)).addValue("t", Date.valueOf(today)))) {
            String s = (String) r.get("statuses");
            worst.put(date(r.get("record_date")), s.contains("ABSENT") ? "ABSENT" : s.contains("LATE") ? "LATE"
                    : s.contains("EXCUSED") ? "EXCUSED" : "PRESENT");
        }
        List<DayDot> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            if (isSchool.test(d)) days.add(new DayDot(d, worst.getOrDefault(d, "NONE")));
        }
        HomeDashboardService.RiskRow risk = cache.get(schoolId, "risk:2000:false",
                () -> home.riskStudents(schoolId, null, 2000, false)).data().rows().stream()
                .filter(r -> r.studentId() == studentId).findFirst().orElse(null);
        return new StudentExtra(risk == null ? null : risk.score(), risk == null ? null : risk.absentDays(),
                risk == null ? null : risk.avgBefore(), risk == null ? null : risk.avgNow(),
                risk == null ? null : risk.twos(), risk == null ? null : risk.parts(),
                Boolean.TRUE.equals(st.get("tg")), days);
    }

    /** "Boshqa sinfga ko'chirish": the chosen students into one class of the same school. */
    @Transactional
    public MoveResult moveStudents(List<Long> studentIds, long classId) {
        SchoolClass target = classes.findById(classId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday sinf mavjud emas"));
        long school = target.getAcademicYear().getSchool().getId();
        int moved = 0;
        for (Student s : students.findAllById(studentIds)) {
            if (s.getSchoolClass().getAcademicYear().getSchool().getId() != school) {
                throw new IllegalStateException("O'quvchi boshqa maktabda: " + s.getFirstName() + " " + s.getLastName());
            }
            if (s.getSchoolClass().getId().equals(classId)) continue;
            s.setSchoolClass(target);
            students.save(s);
            moved++;
        }
        cache.evictSchool(school);
        return new MoveResult(moved, target.getGradeNumber() + "-" + target.getSectionLetter());
    }

    // ------------------------------------------------------------------ Xodimlar

    /** Employees, at work / on leave, teachers' average lessons a week, journal completeness (7 school days). */
    public List<Kpi> employeesHero(long schoolId) {
        Map<String, Object> r = jdbc.queryForMap("""
                select count(*) as total,
                       count(*) filter (where coalesce(e.status, 'ACTIVE') = 'ACTIVE') as active,
                       count(*) filter (where e.status = 'ON_LEAVE') as leave
                  from employee e where e.school_id = :s""", p(schoolId));
        Double load = jdbc.queryForObject("""
                select avg(n) from (select count(*) as n from lesson_slot ls join employee e on e.id = ls.employee_id
                                     where e.school_id = :s group by ls.employee_id) x""", p(schoolId), Double.class);
        HomeDashboardService.Journal journal = cache.get(schoolId, "journal", () -> home.journal(schoolId)).data();
        return List.of(
                new Kpi("employees", (double) l(r.get("total")), null, List.of(), "maktabda"),
                new Kpi("active", (double) l(r.get("active")), null, List.of(), l(r.get("leave")) + " ta ta'tilda"),
                new Kpi("load", load == null ? null : round1(load), null, List.of(), "soat / hafta (o'qituvchi)"),
                new Kpi("journal", journal.school(), null, List.of(), "oxirgi " + journal.days() + " ish kuni"));
    }

    /**
     * A teacher's week as a heat map (weekday × lesson number, the school's numbering by start time)
     * and journal completeness over the last 10 school days, day by day.
     */
    public EmployeeExtra employeeExtra(long employeeId) {
        Long schoolId = jdbc.queryForObject("select school_id from employee where id = :id",
                new MapSqlParameterSource("id", employeeId), Long.class);
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Xodimning maktabi yo'q");
        List<String> weekdays = List.of(WEEKDAYS);
        int[][] heat = new int[weekdays.size()][];
        int perDay = 1;
        List<Map<String, Object>> rows = jdbc.queryForList("""
                with s as (select ls.employee_id, ls.weekday,
                                  dense_rank() over (partition by ls.weekday order by ls.start_time) as n
                             from lesson_slot ls join school_class c on c.id = ls.class_id
                             join academic_year y on y.id = c.academic_year_id where y.school_id = :s)
                select weekday, n, count(*) filter (where employee_id = :e) as mine from s group by weekday, n""",
                p(schoolId).addValue("e", employeeId));
        for (Map<String, Object> r : rows) perDay = Math.max(perDay, i(r.get("n")));
        for (int d = 0; d < heat.length; d++) heat[d] = new int[perDay];
        int weekly = 0;
        for (Map<String, Object> r : rows) {
            int d = weekdayIndex((String) r.get("weekday"));
            if (d < 0) continue;
            int mine = i(r.get("mine"));
            heat[d][i(r.get("n")) - 1] = mine;
            weekly += mine;
        }
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today(), 10);
        List<Double> series = cache.get(schoolId, "lists:journal-run:" + employeeId, () -> {
            List<Double> out = new ArrayList<>();
            for (LocalDate d : days) {
                Double v = home.journal(schoolId, employeeId, List.of(d)).teachers().stream()
                        .filter(t -> t.employeeId() == employeeId).map(HomeDashboardService.TeacherRow::completeness)
                        .findFirst().orElse(null);
                out.add(v);
            }
            return out;
        }).data();
        Double seven = cache.get(schoolId, "journal", () -> home.journal(schoolId)).data().teachers().stream()
                .filter(t -> t.employeeId() == employeeId).map(HomeDashboardService.TeacherRow::completeness)
                .findFirst().orElse(null);
        return new EmployeeExtra(weekdays, perDay, heat, weekly, days, series, seven);
    }

    // ------------------------------------------------------------------ Fanlar

    /** A subject's teachers (lessons a week), classes (lessons a week, quality and average this quarter), grade spread. */
    public SubjectExtra subjectExtra(long subjectId) {
        Long schoolId = jdbc.queryForObject("select school_id from subject where id = :id",
                new MapSqlParameterSource("id", subjectId), Long.class);
        if (schoolId == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fanning maktabi yo'q");
        ListStatsService.Window w = stats.window(schoolId);
        ListStatsService.SubjectStats st = stats.subjects(schoolId).get(subjectId);
        MapSqlParameterSource q = new MapSqlParameterSource("sub", subjectId)
                .addValue("f", Date.valueOf(w.from())).addValue("t", Date.valueOf(w.to()));
        List<ClassRow> rows = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select c.id, c.grade_number, c.section_letter,
                       (select count(*) from lesson_slot ls where ls.class_id = c.id and ls.subject_id = :sub) as lessons,
                       count(g.id) as n, count(g.id) filter (where g.score >= 4) as good, avg(g.score) as a
                  from school_class c
                  left join students st on st.class_id = c.id
                  left join grade g on g.student_id = st.id and g.subject_id = :sub and g.grade_date between :f and :t
                 where c.id in (select ls.class_id from lesson_slot ls where ls.subject_id = :sub)
                    or c.id in (select st2.class_id from grade g2 join students st2 on st2.id = g2.student_id
                                 where g2.subject_id = :sub and g2.grade_date between :f and :t)
                 group by c.id, c.grade_number, c.section_letter
                 order by c.grade_number, c.section_letter""", q)) {
            long n = l(r.get("n"));
            rows.add(new ClassRow(l(r.get("id")), r.get("grade_number") + "-" + r.get("section_letter"),
                    i(r.get("lessons")), DashboardMetrics.pct(l(r.get("good")), n),
                    r.get("a") == null ? null : Math.round(((Number) r.get("a")).doubleValue() * 100) / 100.0, (int) n));
        }
        Map<Integer, Integer> dist = new LinkedHashMap<>();
        for (int v = 5; v >= 2; v--) dist.put(v, 0);
        for (Map<String, Object> r : jdbc.queryForList("""
                select score, count(*) as n from grade where subject_id = :sub and grade_date between :f and :t
                 group by score""", q)) {
            int v = i(r.get("score"));
            if (dist.containsKey(v)) dist.put(v, i(r.get("n")));
        }
        return new SubjectExtra(st == null ? List.of() : st.teachers(), rows, dist, w.label(),
                st == null ? null : st.qualityNow(), st == null ? null : st.qualityPrev());
    }

    // ------------------------------------------------------------------ O'quv yillari

    /**
     * Every year of the school, newest first: status (CURRENT — the flagged one, or the one today is
     * in when none is flagged; FINISHED; PLANNED; ACTIVE), school days in it, passed and left (for the
     * current), classes and students, attendance and quality over its days so far, and its terms.
     */
    @Transactional(readOnly = true)
    public List<YearCard> years(long schoolId) {
        List<AcademicYear> list = years.findBySchoolIdOrderByStartDateDesc(schoolId);
        if (list.isEmpty()) return List.of();
        LocalDate today = today();
        boolean anyFlagged = list.stream().anyMatch(AcademicYear::isCurrent);
        Map<Long, ListStatsService.YearStats> counts = stats.years(schoolId);
        Map<Long, List<Term>> byYear = new HashMap<>();
        for (AcademicTerm t : terms.findByYears(list.stream().map(AcademicYear::getId).toList())) {
            byYear.computeIfAbsent(t.getAcademicYear().getId(), k -> new ArrayList<>())
                    .add(new Term(t.getId(), t.getName(), t.getKind().name(), t.getStartDate(), t.getEndDate()));
        }
        Map<Long, double[]> att = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select y.id, count(*) filter (where a.status in ('PRESENT', 'LATE')) as came,
                       count(*) filter (where a.status in ('PRESENT', 'LATE', 'ABSENT')) as counted
                  from academic_year y join school_class c on c.academic_year_id = y.id
                  join lesson_slot ls on ls.class_id = c.id
                  join attendance a on a.lesson_slot_id = ls.id and a.record_date between y.start_date and y.end_date
                 where y.school_id = :s group by y.id""", p(schoolId))) {
            att.put(l(r.get("id")), new double[]{l(r.get("came")), l(r.get("counted"))});
        }
        Map<Long, double[]> qual = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select y.id, count(*) filter (where g.score >= 4) as good, count(*) as n
                  from academic_year y join school_class c on c.academic_year_id = y.id
                  join students st on st.class_id = c.id
                  join grade g on g.student_id = st.id and g.grade_date between y.start_date and y.end_date
                 where y.school_id = :s group by y.id""", p(schoolId))) {
            qual.put(l(r.get("id")), new double[]{l(r.get("good")), l(r.get("n"))});
        }
        LocalDate from = list.get(list.size() - 1).getStartDate();
        LocalDate to = list.get(0).getEndDate();
        Predicate<LocalDate> isSchool = schoolDays.predicate(schoolId, from, to);
        List<YearCard> out = new ArrayList<>();
        for (AcademicYear y : list) {
            int work = 0;
            int passed = 0;
            for (LocalDate d = y.getStartDate(); !d.isAfter(y.getEndDate()); d = d.plusDays(1)) {
                if (!isSchool.test(d)) continue;
                work++;
                if (!d.isAfter(today)) passed++;
            }
            boolean inside = !today.isBefore(y.getStartDate()) && !today.isAfter(y.getEndDate());
            String status = y.isCurrent() || (!anyFlagged && inside) ? "CURRENT"
                    : today.isAfter(y.getEndDate()) ? "FINISHED" : today.isBefore(y.getStartDate()) ? "PLANNED" : "ACTIVE";
            ListStatsService.YearStats c = counts.get(y.getId());
            double[] a = att.getOrDefault(y.getId(), new double[2]);
            double[] q = qual.getOrDefault(y.getId(), new double[2]);
            out.add(new YearCard(y.getId(), y.getTitle(), y.getStartDate(), y.getEndDate(), y.isCurrent(), status,
                    work, passed, work - passed, work == 0 ? null : round1(passed * 100.0 / work),
                    c == null ? 0 : c.classes(), c == null ? 0 : c.students(),
                    DashboardMetrics.pct((long) a[0], (long) a[1]), DashboardMetrics.pct((long) q[0], (long) q[1]),
                    byYear.getOrDefault(y.getId(), List.of())));
        }
        return out;
    }
}
