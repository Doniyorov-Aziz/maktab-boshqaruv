package uz.azizbek.maktabboshqaruv.support;

import org.springframework.jdbc.core.JdbcTemplate;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A school of its own for a test: one academic year, classes with a teacher, students and the
 * same three Monday lessons (08:30, 09:25, 10:20). Nothing else in the database is touched;
 * {@link #delete()} removes all of it, including what the code under test wrote.
 */
public class TestSchool {

    public static final LocalTime[] STARTS = {LocalTime.of(8, 30), LocalTime.of(9, 25), LocalTime.of(10, 20)};

    public School school;
    public Employee teacher;
    public Subject subject;
    public Room room;
    public final List<SchoolClass> classes = new ArrayList<>();
    /** lessons.get(class)[n] — the n-th Monday lesson of that class. */
    public final List<LessonSlot[]> lessons = new ArrayList<>();
    /** students.get(class) */
    public final List<List<Student>> students = new ArrayList<>();

    private final JdbcTemplate jdbc;

    public TestSchool(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static TestSchool create(Repos r, JdbcTemplate jdbc, int classCount, int studentsPerClass) {
        TestSchool t = new TestSchool(jdbc);
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        School s = new School();
        s.setName("Test maktab " + n);
        s.setAddress("Toshkent");
        t.school = r.schools.save(s);

        AcademicYear y = new AcademicYear();
        y.setSchool(t.school);
        y.setTitle("2030-2031 " + n);
        y.setStartDate(LocalDate.of(2030, 9, 1));
        y.setEndDate(LocalDate.of(2031, 6, 30));
        y = r.years.save(y);

        Position pos = r.positions.findAll().get(0);
        Employee e = new Employee();
        e.setSchool(t.school);
        e.setPosition(pos);
        e.setFirstName("Test");
        e.setLastName("O'qituvchi");
        e.setPhone("+9989" + (50_000_000 + n));
        t.teacher = r.employees.save(e);

        Subject sub = new Subject();
        sub.setName("Algebra " + n);
        sub.setSchool(t.school);
        sub.setActive(true);
        t.subject = r.subjects.save(sub);

        Building b = new Building();
        b.setSchool(t.school);
        b.setName("Bino " + n);
        b = r.buildings.save(b);
        Room room = new Room();
        room.setBuilding(b);
        room.setRoomNumber("301");
        room.setCapacity(30);
        t.room = r.rooms.save(room);

        for (int c = 0; c < classCount; c++) {
            SchoolClass sc = new SchoolClass();
            sc.setAcademicYear(y);
            sc.setGradeNumber(5 + c);
            sc.setSectionLetter("A");
            sc.setMaxStudents(30);
            if (c == 0) sc.setClassTeacher(t.teacher);
            sc = r.classes.save(sc);
            t.classes.add(sc);
            LessonSlot[] day = new LessonSlot[STARTS.length];
            for (int k = 0; k < STARTS.length; k++) {
                LessonSlot ls = new LessonSlot();
                ls.setSchoolClass(sc);
                ls.setSubject(t.subject);
                ls.setEmployee(t.teacher);
                ls.setRoom(t.room);
                ls.setWeekday("Dushanba");
                ls.setStartTime(STARTS[k]);
                ls.setEndTime(STARTS[k].plusMinutes(45));
                day[k] = r.lessons.save(ls);
            }
            t.lessons.add(day);
            List<Student> kids = new ArrayList<>();
            for (int i = 0; i < studentsPerClass; i++) {
                Student st = new Student();
                st.setSchoolClass(sc);
                st.setFirstName("O'quvchi" + i);
                st.setLastName("Test" + c);
                st.setBirthDate(LocalDate.of(2018, 1, 1));
                kids.add(r.students.save(st));
            }
            t.students.add(kids);
        }
        return t;
    }

    public long id() {
        return school.getId();
    }

    /** Everything this school has, in foreign-key order. */
    public void delete() {
        long s = id();
        String classes = "select c.id from school_class c join academic_year y on y.id = c.academic_year_id where y.school_id = " + s;
        String slots = "select id from lesson_slot where class_id in (" + classes + ")";
        jdbc.update("delete from notification_log where school_id = ?", s);
        jdbc.update("delete from staff_notification where school_id = ?", s);
        jdbc.update("delete from attendance where lesson_slot_id in (" + slots + ")");
        jdbc.update("delete from grade where student_id in (select id from students where class_id in (" + classes + "))");
        jdbc.update("delete from appeal_message where appeal_id in (select id from appeal where school_id = ?)", s);
        jdbc.update("delete from appeal where school_id = ?", s);
        jdbc.update("delete from parent_telegram_link where student_id in (select id from students where class_id in (" + classes + "))");
        jdbc.update("delete from lesson_substitution where lesson_slot_id in (" + slots + ")");
        jdbc.update("delete from room_booking where school_id = ?", s);
        jdbc.update("delete from calendar_feed_token where school_id = ?", s);
        jdbc.update("delete from lesson_slot where class_id in (" + classes + ")");
        jdbc.update("delete from students where class_id in (" + classes + ")");
        jdbc.update("delete from school_class where academic_year_id in (select id from academic_year where school_id = ?)", s);
        jdbc.update("delete from academic_year where school_id = ?", s);
        jdbc.update("delete from calendar_event where school_id = ?", s); // events may name a room
        jdbc.update("delete from room where building_id in (select id from building where school_id = ?)", s);
        jdbc.update("delete from building where school_id = ?", s);
        jdbc.update("delete from subject where school_id = ?", s);
        jdbc.update("delete from device_api_key where school_id = ?", s);
        jdbc.update("delete from school_settings where school_id = ?", s);
        jdbc.update("delete from calendar_event where school_id = ?", s);
        jdbc.update("delete from activity_log where school_id = ?", s);
        jdbc.update("delete from app_user where employee_id in (select id from employee where school_id = ?)", s);
        jdbc.update("delete from employee where school_id = ?", s);
        jdbc.update("delete from school where id = ?", s);
    }

    /** The repositories a test school needs. */
    public record Repos(SchoolRepository schools, AcademicYearRepository years, PositionRepository positions,
                        EmployeeRepository employees, SubjectRepository subjects, BuildingRepository buildings,
                        RoomRepository rooms, SchoolClassRepository classes, LessonSlotRepository lessons,
                        StudentRepository students) {
    }
}
