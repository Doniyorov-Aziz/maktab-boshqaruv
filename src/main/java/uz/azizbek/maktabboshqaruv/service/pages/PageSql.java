package uz.azizbek.maktabboshqaruv.service.pages;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalTime;

/** Small helpers shared by the page services (column values of NamedParameterJdbcTemplate rows). */
final class PageSql {

    private PageSql() {
    }

    static final String[] WEEKDAYS = {"Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba"};

    static int i(Object o) {
        return o == null ? 0 : ((Number) o).intValue();
    }

    static long l(Object o) {
        return o == null ? 0 : ((Number) o).longValue();
    }

    static Long lOrNull(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    static Double round1(Double v) {
        return v == null ? null : Math.round(v * 10) / 10.0;
    }

    static LocalDate date(Object o) {
        return o instanceof Date dt ? dt.toLocalDate() : (LocalDate) o;
    }

    static LocalTime time(Object o) {
        return o instanceof java.sql.Time t ? t.toLocalTime() : (LocalTime) o;
    }

    static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** "Karimova D." from last and first name. */
    static String shortName(String last, String first) {
        String l = last == null ? "" : last.trim();
        String f = first == null || first.isBlank() ? "" : " " + first.trim().charAt(0) + ".";
        return (l + f).trim();
    }

    static int weekdayIndex(String weekday) {
        for (int k = 0; k < WEEKDAYS.length; k++) if (WEEKDAYS[k].equals(weekday)) return k;
        return -1;
    }

    /** Lessons of a school, one row each, with everything a timetable cell shows. */
    static final String LESSONS = """
            select ls.id, ls.class_id, c.grade_number, c.section_letter, ls.subject_id, s.name as subject, s.color as subject_color,
                   ls.employee_id, e.first_name, e.last_name, e.phone, ls.room_id, r.room_number, ls.weekday, ls.start_time, ls.end_time
              from lesson_slot ls
              join school_class c on c.id = ls.class_id
              join academic_year y on y.id = c.academic_year_id
              join subject s on s.id = ls.subject_id
              join employee e on e.id = ls.employee_id
              join room r on r.id = ls.room_id
             where y.school_id = :s
             order by c.grade_number, c.section_letter, ls.weekday, ls.start_time""";
}
