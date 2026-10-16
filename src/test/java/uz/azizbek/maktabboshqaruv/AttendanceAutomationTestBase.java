package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import uz.azizbek.maktabboshqaruv.entity.AttendanceSource;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Student;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationService;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceAutomation;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceWriter;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceWriter.AutoMark;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceWriter.ManualMark;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoMode;
import uz.azizbek.maktabboshqaruv.service.attendance.DeviceCheckinService;
import uz.azizbek.maktabboshqaruv.support.MutableClock;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

/** Shared by the automation tests: a school of its own on Monday 2031-03-03 and a clock the test moves. */
abstract class AttendanceAutomationTestBase {

    static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);

    @TestConfiguration
    static class FakeClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(MONDAY.atTime(8, 0), TASHKENT);
        }
    }

    @LocalServerPort
    int port;
    @Autowired
    MutableClock clock;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    AttendanceAutomation automation;
    @Autowired
    AttendanceWriter writer;
    @Autowired
    DeviceCheckinService devices;
    @Autowired
    NotificationService notifications;
    @Autowired
    ApplicationEvents events;
    @Autowired
    SchoolRepository schools;
    @Autowired
    AcademicYearRepository years;
    @Autowired
    PositionRepository positions;
    @Autowired
    EmployeeRepository employees;
    @Autowired
    SubjectRepository subjects;
    @Autowired
    BuildingRepository buildings;
    @Autowired
    RoomRepository rooms;
    @Autowired
    SchoolClassRepository classes;
    @Autowired
    LessonSlotRepository lessons;
    @Autowired
    StudentRepository students;

    TestSchool school;

    @BeforeEach
    void setUp() {
        clock.set(MONDAY.atTime(8, 0));
        school = TestSchool.create(new TestSchool.Repos(schools, years, positions, employees, subjects, buildings, rooms,
                classes, lessons, students), jdbc, 2, 6);
    }

    @AfterEach
    void cleanUp() {
        school.delete();
    }

    // ------------------------------------------------------------------ helpers

    LessonSlot lesson(int cls, int n) {
        return school.lessons.get(cls)[n];
    }

    Student student(int cls, int i) {
        return school.students.get(cls).get(i);
    }

    void manual(int cls, int n, AttendanceStatus status, int... studentIdx) {
        List<ManualMark> marks = java.util.Arrays.stream(studentIdx)
                .mapToObj(i -> new ManualMark(student(cls, i).getId(), status, null)).toList();
        writer.saveManual(school.id(), lesson(cls, n).getId(), MONDAY, marks, "ustoz");
    }

    /** status and source of one mark, or null when there is none */
    String[] mark(int cls, int n, int i) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select status, source from attendance where lesson_slot_id = ? and student_id = ? and record_date = ?",
                lesson(cls, n).getId(), student(cls, i).getId(), java.sql.Date.valueOf(MONDAY));
        assertTrue(rows.size() <= 1, "never two rows for one lesson, student and day");
        return rows.isEmpty() ? null : new String[]{(String) rows.get(0).get("status"), (String) rows.get(0).get("source")};
    }

    int count(String source) {
        return jdbc.queryForObject("select count(*) from attendance a join lesson_slot ls on ls.id = a.lesson_slot_id "
                + "where ls.class_id in (?, ?) and a.source = ?", Integer.class,
                school.classes.get(0).getId(), school.classes.get(1).getId(), source);
    }

    void at(int hour, int minute) {
        clock.set(MONDAY.atTime(hour, minute));
    }

    @Autowired
    uz.azizbek.maktabboshqaruv.service.attendance.AttendanceSettingsService settings;

    <T> T asAdmin(java.util.concurrent.Callable<T> action) throws Exception {
        var ctx = org.springframework.security.core.context.SecurityContextHolder.getContext();
        var before = ctx.getAuthentication();
        ctx.setAuthentication(new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ADMIN"))));
        try {
            return action.call();
        } finally {
            ctx.setAuthentication(before);
        }
    }
}
