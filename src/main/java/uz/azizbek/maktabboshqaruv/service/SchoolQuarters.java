package uz.azizbek.maktabboshqaruv.service;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.service.parent.ParentStats;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The quarters of a school year as the school entered them on the O'quv yillari page (academic_term, kind QUARTER).
 * Everything that says "chorak" — the dashboards, the parents' bot, the calendar — takes its dates from here.
 * A year without terms falls back to the calendar quarters of {@link ParentStats}.
 */
@Component
public class SchoolQuarters {

    /** How far back a finished quarter still counts as "now" (a vacation, the summer). */
    static final int LOOKBACK_DAYS = 100;

    /** A quarter: its number in the school year (1..4) and its dates. */
    public record Quarter(int number, LocalDate from, LocalDate to) {
    }

    /** A term of a year, quarter or vacation, in date order. */
    public record Term(String kind, String name, LocalDate from, LocalDate to) {
    }

    private final NamedParameterJdbcTemplate jdbc;

    public SchoolQuarters(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** All terms of an academic year (quarters and vacations), in date order; empty when none were entered. */
    public List<Term> terms(long yearId) {
        List<Term> out = new ArrayList<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select kind, name, start_date, end_date from academic_term
                 where academic_year_id = :y order by start_date""", new MapSqlParameterSource("y", yearId))) {
            out.add(new Term((String) r.get("kind"), (String) r.get("name"), date(r.get("start_date")), date(r.get("end_date"))));
        }
        return out;
    }

    /**
     * The dates of a school year as entered (O'quv yillari) while {@code today} is in it or in the summer after it
     * (up to {@value #LOOKBACK_DAYS} days past its end); otherwise — the year has not started, or is long over — the
     * calendar school year around today (Sep 1 – Aug 31).
     */
    public LocalDate[] yearRange(long yearId, LocalDate today) {
        List<Map<String, Object>> rows = jdbc.queryForList("select start_date, end_date from academic_year where id = :y",
                new MapSqlParameterSource("y", yearId));
        if (!rows.isEmpty()) {
            LocalDate from = date(rows.get(0).get("start_date"));
            LocalDate to = date(rows.get(0).get("end_date"));
            if (from != null && to != null && !today.isBefore(from) && !today.isAfter(to.plusDays(LOOKBACK_DAYS))) {
                return new LocalDate[]{from, to};
            }
        }
        return ParentStats.schoolYearRange(today);
    }

    /** The quarters of an academic year, numbered 1.. in date order; empty when none were entered. */
    public List<Quarter> ofYear(long yearId) {
        List<Quarter> out = new ArrayList<>();
        for (Term t : terms(yearId)) {
            if ("QUARTER".equals(t.kind())) out.add(new Quarter(out.size() + 1, t.from(), t.to()));
        }
        return out;
    }

    /**
     * The school's quarter "now": the QUARTER term today falls in, or, in a vacation or the summer, the last one
     * that ended (up to {@value #LOOKBACK_DAYS} days back), so "shu chorak" does not go blank between quarters.
     * A school without such a term gets the calendar quarter.
     */
    public Quarter current(long schoolId, LocalDate today) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                select q.academic_year_id from academic_term q join academic_year y on y.id = q.academic_year_id
                 where y.school_id = :s and q.kind = 'QUARTER' and q.start_date <= :d and q.end_date >= :back
                 order by q.start_date desc limit 1""",
                new MapSqlParameterSource("s", schoolId).addValue("d", today).addValue("back", today.minusDays(LOOKBACK_DAYS)));
        if (rows.isEmpty()) return calendar(today, ParentStats.quarterOf(today));
        return currentOf(ofYear(((Number) rows.get(0).get("academic_year_id")).longValue()), today);
    }

    /** "Now" within one year's quarters (the parents' bot: the child's own year). */
    public Quarter currentOfYear(long yearId, LocalDate today) {
        return currentOf(ofYear(yearId), today);
    }

    /** Quarter {@code n} of the year; the calendar quarter when the year has no n-th quarter entered. */
    public Quarter nth(long yearId, int n, LocalDate today) {
        List<Quarter> qs = ofYear(yearId);
        return n >= 1 && n <= qs.size() ? qs.get(n - 1) : calendar(today, n);
    }

    private static Quarter currentOf(List<Quarter> qs, LocalDate today) {
        Quarter last = null;
        for (Quarter q : qs) {
            if (!q.from().isAfter(today)) last = q;
        }
        if (last != null && !last.to().isBefore(today.minusDays(LOOKBACK_DAYS))) return last;
        return calendar(today, ParentStats.quarterOf(today));
    }

    private static Quarter calendar(LocalDate today, int n) {
        int k = Math.max(1, Math.min(4, n));
        LocalDate[] r = ParentStats.quarterRange(today, k);
        return new Quarter(k, r[0], r[1]);
    }

    private static LocalDate date(Object o) {
        return o instanceof Date d ? d.toLocalDate() : (LocalDate) o;
    }
}
