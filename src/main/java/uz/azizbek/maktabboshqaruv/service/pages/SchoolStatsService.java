package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardMetrics;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;

import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * A school card's numbers: teachers (employees with lessons), today's attendance (or the last school
 * day with marks), quality (4–5) and mastery (3–5) of the last 30 days' grades, parents in Telegram,
 * the director, and 30 school days of attendance for the sparkline. Cached 90 seconds per school
 * like the dashboard.
 */
@Service
public class SchoolStatsService {

    public record Director(long id, String name, String phone) {
    }

    public record SchoolCard(int teachers, Double today, String todayNote, Double quality, Double mastery,
                             Double telegram, int telegramLinked, Director director, List<Double> run) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final HomeDashboardService home;
    private final SchoolDays schoolDays;
    private final DashboardCache cache;
    private final Clock clock;

    public SchoolStatsService(NamedParameterJdbcTemplate jdbc, HomeDashboardService home, SchoolDays schoolDays,
                              DashboardCache cache, Clock clock) {
        this.jdbc = jdbc;
        this.home = home;
        this.schoolDays = schoolDays;
        this.cache = cache;
        this.clock = clock;
    }

    public SchoolCard card(long schoolId) {
        return cache.get(schoolId, "lists:school-card", () -> compute(schoolId)).data();
    }

    private SchoolCard compute(long schoolId) {
        LocalDate today = LocalDate.now(clock);
        MapSqlParameterSource p = new MapSqlParameterSource("s", schoolId)
                .addValue("f", Date.valueOf(today.minusDays(29))).addValue("t", Date.valueOf(today));
        Long teachers = jdbc.queryForObject("""
                select count(distinct ls.employee_id) from lesson_slot ls join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id where y.school_id = :s""", p, Long.class);
        Map<String, Object> g = jdbc.queryForMap("""
                select count(*) filter (where g.score >= 4) as good, count(*) filter (where g.score >= 3) as passed,
                       count(*) as n
                  from grade g join students st on st.id = g.student_id join school_class c on c.id = st.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and g.grade_date between :f and :t""", p);
        Director director = jdbc.queryForList("""
                select e.id, trim(coalesce(e.first_name, '') || ' ' || coalesce(e.last_name, '')) as name, e.phone
                  from employee e join position ps on ps.id = e.position_id
                 where e.school_id = :s and lower(ps.title) = 'direktor' order by e.id limit 1""", p).stream()
                .map(r -> new Director(l(r.get("id")), (String) r.get("name"), (String) r.get("phone")))
                .findFirst().orElse(null);
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, today, 30);
        Map<LocalDate, HomeDashboardService.DayRate> rates = days.isEmpty() ? Map.of()
                : home.dailyRates(schoolId, days.get(0), days.get(days.size() - 1));
        List<Double> run = days.stream().map(d -> rates.get(d) == null ? null : rates.get(d).rate()).toList();
        int k = run.size() - 1;
        while (k >= 0 && run.get(k) == null) k--;
        String note = k == run.size() - 1 && k >= 0 ? "bugun" : k < 0 ? "belgilanmagan"
                : days.get(k).getDayOfMonth() + "." + String.format("%02d", days.get(k).getMonthValue());
        HomeDashboardService.ParentsTelegram tg = home.parentsTelegram(schoolId);
        long n = l(g.get("n"));
        return new SchoolCard(teachers == null ? 0 : teachers.intValue(), k < 0 ? null : run.get(k), note,
                DashboardMetrics.pct(l(g.get("good")), n), DashboardMetrics.pct(l(g.get("passed")), n),
                tg.linkedPct(), tg.linked(), director, run);
    }
}
