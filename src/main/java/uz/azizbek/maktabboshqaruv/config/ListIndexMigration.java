package uz.azizbek.maktabboshqaruv.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Indexes for the list pages (O'quvchilar, Xodimlar, Fanlar, Lavozimlar, O'quv yillari) and their
 * per-row numbers: a student's attendance over 30 days, a class's students, a teacher's or a
 * class's lessons, a school's employees. ddl-auto makes none of them.
 */
@Component
@Order(0)
public class ListIndexMigration implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    public ListIndexMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("create index if not exists idx_attendance_student_date on attendance (student_id, record_date)");
        jdbc.execute("create index if not exists idx_students_class on students (class_id)");
        jdbc.execute("create index if not exists idx_school_class_year on school_class (academic_year_id)");
        jdbc.execute("create index if not exists idx_lesson_slot_employee on lesson_slot (employee_id)");
        jdbc.execute("create index if not exists idx_lesson_slot_class on lesson_slot (class_id)");
        jdbc.execute("create index if not exists idx_lesson_slot_subject on lesson_slot (subject_id)");
        jdbc.execute("create index if not exists idx_employee_school_position on employee (school_id, position_id)");
    }
}
