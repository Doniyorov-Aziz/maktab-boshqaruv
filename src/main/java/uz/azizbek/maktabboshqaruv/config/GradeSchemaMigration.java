package uz.azizbek.maktabboshqaruv.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * What Hibernate's ddl-auto cannot make on the grade table: the partial unique key that keeps
 * the demo simulator to one grade per lesson, student and day (staff grades have no lesson and
 * are not limited), and indexes for the home page's per-student and per-day grade queries.
 */
@Component
@Order(0)
public class GradeSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public GradeSchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("create unique index if not exists uk_grade_lesson_student_day on grade (lesson_slot_id, student_id, grade_date) "
                + "where lesson_slot_id is not null");
        jdbc.execute("create index if not exists idx_grade_student_date on grade (student_id, grade_date)");
        jdbc.execute("create index if not exists idx_grade_date on grade (grade_date)");
    }
}
