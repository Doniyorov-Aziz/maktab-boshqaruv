package uz.azizbek.maktabboshqaruv.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Which school a record belongs to — one small indexed query per kind, cached for
 * 5 minutes (a record never moves to another school). Used by the school-scope guard
 * so that a user of one school cannot read or change another school's data by id.
 */
@Component
public class SchoolOwnership {

    public enum Kind {
        SCHOOL, CLASS, STUDENT, EMPLOYEE, YEAR, BUILDING, ROOM, SUBJECT, LESSON_SLOT, ANNOUNCEMENT,
        CALENDAR_EVENT, ABSENCE, BEHAVIOR, GRADE, ATTENDANCE, USER, APPEAL, BROADCAST
    }

    private static final String CLASS_SCHOOL =
            "select ay.school_id from school_class c join academic_year ay on ay.id = c.academic_year_id where c.id = ?";
    private static final String STUDENT_SCHOOL = "select ay.school_id from students s join school_class c on c.id = s.class_id "
            + "join academic_year ay on ay.id = c.academic_year_id where s.id = ?";

    private static final Map<Kind, String> SQL = Map.ofEntries(
            Map.entry(Kind.SCHOOL, "select id from school where id = ?"),
            Map.entry(Kind.CLASS, CLASS_SCHOOL),
            Map.entry(Kind.STUDENT, STUDENT_SCHOOL),
            Map.entry(Kind.EMPLOYEE, "select school_id from employee where id = ?"),
            Map.entry(Kind.YEAR, "select school_id from academic_year where id = ?"),
            Map.entry(Kind.BUILDING, "select school_id from building where id = ?"),
            Map.entry(Kind.ROOM, "select b.school_id from room r join building b on b.id = r.building_id where r.id = ?"),
            Map.entry(Kind.SUBJECT, "select school_id from subject where id = ?"),
            Map.entry(Kind.LESSON_SLOT, CLASS_SCHOOL.replace("where c.id = ?",
                    "where c.id = (select class_id from lesson_slot where id = ?)")),
            Map.entry(Kind.ANNOUNCEMENT, "select school_id from announcement where id = ?"),
            Map.entry(Kind.CALENDAR_EVENT, "select school_id from calendar_event where id = ?"),
            Map.entry(Kind.ABSENCE, "select school_id from absence_request where id = ?"),
            Map.entry(Kind.BEHAVIOR, STUDENT_SCHOOL.replace("where s.id = ?",
                    "where s.id = (select student_id from behavior_record where id = ?)")),
            Map.entry(Kind.GRADE, STUDENT_SCHOOL.replace("where s.id = ?",
                    "where s.id = (select student_id from grade where id = ?)")),
            Map.entry(Kind.ATTENDANCE, STUDENT_SCHOOL.replace("where s.id = ?",
                    "where s.id = (select student_id from attendance where id = ?)")),
            Map.entry(Kind.USER, "select e.school_id from app_user u join employee e on e.id = u.employee_id where u.id = ?"),
            Map.entry(Kind.APPEAL, "select school_id from appeal where id = ?"),
            Map.entry(Kind.BROADCAST, "select school_id from broadcast where id = ?")
    );

    private final JdbcTemplate jdbc;
    private final Cache<String, Optional<Long>> cache = Caffeine.newBuilder()
            .maximumSize(50_000).expireAfterWrite(Duration.ofMinutes(5)).build();

    public SchoolOwnership(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The school of that record; empty when it does not exist (the controller answers 404). */
    public Optional<Long> schoolOf(Kind kind, long id) {
        return cache.get(kind + ":" + id, k -> {
            List<Long> rows = jdbc.queryForList(SQL.get(kind), Long.class, id);
            return rows.isEmpty() || rows.get(0) == null ? Optional.empty() : Optional.of(rows.get(0));
        });
    }
}
