package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Predicate;

/**
 * O'qituvchi ish yuklamasi: from the weekly timetable, how many lessons each teacher has a week and
 * how many fell in a period — school days only (Sundays, Taqvim holidays/vacations and the year's
 * vacation terms are skipped) — minus the lessons another teacher took over (o'rinbosar) plus the
 * lessons the teacher took over for others.
 */
@Service
public class WorkloadService {

    /** One class × subject a teacher teaches, with its weekly lessons. */
    public record Load(String className, String subject, int weekly) {
    }

    public record Row(Long employeeId, String name, String position, int weeklyLessons, List<Load> loads,
                      int planned, int substitutedAway, int coveredForOthers, int total) {
    }

    /** {@code countedTo}: the last day counted — the period's end, or today when the period runs into the future. */
    public record Report(LocalDate from, LocalDate to, LocalDate countedTo, int schoolDays, List<Row> rows, int totalWeekly,
                         int totalHeld) {
    }

    /** Longest period one request may ask for. */
    static final int MAX_DAYS = 400;

    private final NamedParameterJdbcTemplate jdbc;
    private final SchoolDays schoolDays;
    private final SchoolAccessService access;
    private final java.time.Clock clock;

    public WorkloadService(NamedParameterJdbcTemplate jdbc, SchoolDays schoolDays, SchoolAccessService access,
                           java.time.Clock clock) {
        this.jdbc = jdbc;
        this.schoolDays = schoolDays;
        this.access = access;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Report report(Long schoolId, LocalDate from, LocalDate to) {
        access.requireSchool(schoolId);
        if (from == null || to == null || to.isBefore(from)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Davr noto'g'ri tanlangan");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_DAYS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Davr bir yildan oshmasin");
        }
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId).addValue("f", from).addValue("t", to);

        // the timetable of the years that overlap the period
        record Slot(long employeeId, String weekday, String className, String subject, Date yearFrom, Date yearTo) {
        }
        List<Slot> slots = jdbc.query("""
                select ls.employee_id, ls.weekday, c.grade_number || '-' || c.section_letter, sub.name, ay.start_date, ay.end_date
                  from lesson_slot ls join school_class c on c.id = ls.class_id join academic_year ay on ay.id = c.academic_year_id
                  join subject sub on sub.id = ls.subject_id
                 where ay.school_id = :s and ay.start_date <= :t and ay.end_date >= :f""", p,
                (rs, i) -> new Slot(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getDate(5), rs.getDate(6)));

        // vacation terms of those years are not school days either
        Set<LocalDate> vacation = new HashSet<>();
        jdbc.query("""
                select t.start_date, t.end_date from academic_term t join academic_year ay on ay.id = t.academic_year_id
                 where ay.school_id = :s and t.kind = 'VACATION' and t.start_date <= :t and t.end_date >= :f""", p, rs -> {
            for (LocalDate d = rs.getDate(1).toLocalDate(); !d.isAfter(rs.getDate(2).toLocalDate()); d = d.plusDays(1)) vacation.add(d);
        });
        Predicate<LocalDate> isSchool = schoolDays.predicate(schoolId, from, to);
        List<LocalDate> days = new ArrayList<>();
        // lessons are "held" only on days that have come
        LocalDate today = LocalDate.now(clock);
        LocalDate countedTo = to.isAfter(today) ? today : to;
        for (LocalDate d = from; !d.isAfter(countedTo); d = d.plusDays(1)) {
            if (!isSchool.test(d) || vacation.contains(d)) continue;
            days.add(d);
        }

        Map<Long, int[]> subs = new HashMap<>();
        jdbc.query("""
                select absent_teacher_id, substitute_id from lesson_substitution ls join employee e on e.id = ls.substitute_id
                 where e.school_id = :s and ls.sub_date between :f and :t""", p, rs -> {
            subs.computeIfAbsent(rs.getLong(1), k -> new int[2])[0]++;
            subs.computeIfAbsent(rs.getLong(2), k -> new int[2])[1]++;
        });

        Map<Long, String[]> people = new LinkedHashMap<>();
        jdbc.query("""
                select e.id, e.last_name || ' ' || e.first_name, p.title from employee e left join position p on p.id = e.position_id
                 where e.school_id = :s order by e.last_name, e.first_name""", p, rs -> {
            people.put(rs.getLong(1), new String[]{rs.getString(2), rs.getString(3)});
        });

        Map<Long, List<Slot>> byTeacher = new HashMap<>();
        for (Slot s : slots) byTeacher.computeIfAbsent(s.employeeId(), k -> new ArrayList<>()).add(s);

        List<Row> rows = new ArrayList<>();
        int totalWeekly = 0;
        int totalHeld = 0;
        for (Map.Entry<Long, String[]> person : people.entrySet()) {
            Long id = person.getKey();
            List<Slot> mine = byTeacher.getOrDefault(id, List.of());
            int[] sub = subs.getOrDefault(id, new int[2]);
            if (mine.isEmpty() && sub[0] == 0 && sub[1] == 0) continue;

            // weekly lessons: the slots of the year that is "now" for the period end (a teacher's two years never double)
            Date latestYear = mine.stream().map(Slot::yearFrom).max(Comparator.naturalOrder()).orElse(null);
            Map<String, Integer> loads = new TreeMap<>();
            int weekly = 0;
            for (Slot s : mine) {
                if (!s.yearFrom().equals(latestYear)) continue;
                weekly++;
                loads.merge(s.className() + "|" + s.subject(), 1, Integer::sum);
            }
            int planned = 0;
            for (LocalDate d : days) {
                String dayName = SchoolDays.WEEKDAY_UZ.get(d.getDayOfWeek());
                for (Slot s : mine) {
                    if (!s.weekday().equalsIgnoreCase(dayName)) continue;
                    if (d.isBefore(s.yearFrom().toLocalDate()) || d.isAfter(s.yearTo().toLocalDate())) continue;
                    planned++;
                }
            }
            List<Load> loadList = new ArrayList<>();
            loads.entrySet().stream()
                    .sorted(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getKey(), WorkloadService::classOrder))
                    .forEach(e -> {
                        String[] k = e.getKey().split("\\|", 2);
                        loadList.add(new Load(k[0], k[1], e.getValue()));
                    });
            int total = Math.max(0, planned - sub[0]) + sub[1];
            rows.add(new Row(id, person.getValue()[0], person.getValue()[1], weekly, loadList, planned, sub[0], sub[1], total));
            totalWeekly += weekly;
            totalHeld += total;
        }
        rows.sort(Comparator.comparingInt(Row::weeklyLessons).reversed().thenComparing(Row::name));
        return new Report(from, to, countedTo, days.size(), rows, totalWeekly, totalHeld);
    }

    /** "5-A|Matematika" before "10-A|…": the grade as a number. */
    static int classOrder(String a, String b) {
        return Comparator.comparingInt(WorkloadService::gradeOf).thenComparing(Comparator.naturalOrder()).compare(a, b);
    }

    private static int gradeOf(String key) {
        int dash = key.indexOf('-');
        try {
            return Integer.parseInt(key.substring(0, dash < 0 ? 0 : dash));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
