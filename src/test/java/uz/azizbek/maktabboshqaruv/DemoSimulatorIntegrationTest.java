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

/**
 * The demo simulator (app.demo.attendance-simulator.enabled=true): deterministic, idempotent,
 * never over a MANUAL mark, never a message to parents, removable; off under "prod".
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=true"
})
@ActiveProfiles("local")
@Import(AttendanceAutomationTestBase.FakeClock.class)
@RecordApplicationEvents
class DemoSimulatorIntegrationTest extends AttendanceAutomationTestBase {

    @Test
    void manualMark_isNeverChangedByCarryNorDemo() {
        at(8, 45);
        manual(0, 0, AttendanceStatus.PRESENT, 0, 1, 2, 3, 4, 5);
        manual(0, 1, AttendanceStatus.ABSENT, 0); // a teacher marked student 0 absent in lesson 2

        at(10, 40); // lesson 2 and 3 started long ago: carry and the simulator both run
        automation.runFor(school.id());
        automation.runFor(school.id());

        assertArrayEquals(new String[]{"ABSENT", "MANUAL"}, mark(0, 1, 0), "the manual mark stays exactly as it was");
        for (int i = 0; i < 6; i++) assertEquals("MANUAL", mark(0, 0, i)[1]);
        // student 0's 3rd lesson is carried from the manual ABSENT, not simulated
        assertArrayEquals(new String[]{"ABSENT", "AUTO_CARRY"}, mark(0, 2, 0));
    }

    @Test
    void simulator_isDeterministic_idempotent_andLeavesManualAlone() {
        at(8, 40);
        manual(0, 0, AttendanceStatus.ABSENT, 0);
        automation.runFor(school.id());
        int first = count("DEMO_SIM");
        assertEquals(11, first, "lesson 1 of both classes minus the one manual mark");
        Map<String, Object> snapshot = jdbc.queryForMap("select string_agg(student_id || ':' || status, ',' order by student_id) as s "
                + "from attendance where lesson_slot_id = ? and record_date = ?", lesson(1, 0).getId(), java.sql.Date.valueOf(MONDAY));

        assertEquals(0, automation.runFor(school.id()).simulated(), "a second run adds nothing");
        jdbc.update("delete from attendance where lesson_slot_id = ? and source = 'DEMO_SIM'", lesson(1, 0).getId());
        automation.runFor(school.id());
        assertEquals(snapshot, jdbc.queryForMap("select string_agg(student_id || ':' || status, ',' order by student_id) as s "
                        + "from attendance where lesson_slot_id = ? and record_date = ?", lesson(1, 0).getId(), java.sql.Date.valueOf(MONDAY)),
                "the same day simulated again gives the same marks");
        assertArrayEquals(new String[]{"ABSENT", "MANUAL"}, mark(0, 0, 0));
    }

    @Test
    void clearingDemo_removesOnlyDemoMarks() {
        at(8, 40);
        manual(0, 0, AttendanceStatus.PRESENT, 0);
        automation.runFor(school.id());
        assertTrue(count("DEMO_SIM") > 0);
        writer.deleteDemo(school.id());
        assertEquals(0, count("DEMO_SIM"));
        assertEquals(1, count("MANUAL"));
    }

    @Test
    void simulator_staysOff_underTheProdProfile() {
        MockEnvironment prod = new MockEnvironment();
        prod.setActiveProfiles("prod");
        assertFalse(new DemoMode(prod, true, 10).enabled());
        MockEnvironment dev = new MockEnvironment();
        dev.setActiveProfiles("local");
        assertTrue(new DemoMode(dev, true, 10).enabled());
        assertFalse(new DemoMode(dev, false, 10).enabled());
    }

    @Test
    void demoAndCarriedMarks_sendNothingToParents_manualDoes() {
        at(8, 40);
        manual(0, 0, AttendanceStatus.ABSENT, 0);
        assertEquals(1, events.stream(AttendanceMarkedEvent.class).count(), "a manual absence is told");

        at(9, 40); // carry (student 0 → absent) and the simulator (everyone else) run
        automation.runFor(school.id());
        assertEquals(1, events.stream(AttendanceMarkedEvent.class).count(), "nothing for AUTO_CARRY / DEMO_SIM");

        // and even if an event slipped through, the outbox refuses a simulated mark
        Long demoAbsent = jdbc.queryForObject("select id from attendance where source = 'DEMO_SIM' and lesson_slot_id in (?, ?) limit 1",
                Long.class, lesson(1, 0).getId(), lesson(1, 1).getId());
        jdbc.update("update attendance set status = 'ABSENT' where id = ?", demoAbsent);
        assertEquals(0, notifications.enqueueAttendance(new AttendanceMarkedEvent(demoAbsent, null, AttendanceStatus.ABSENT)));
    }
}
