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
import uz.azizbek.maktabboshqaruv.service.attendance.DemoSimulatorSwitch;
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
 * The real-mode automation (no demo simulator) on a real database with a clock the test moves:
 * AUTO_CARRY, reminders, the device check-in and — first of all — that nothing automatic ever
 * changes a MANUAL mark.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.attendance-simulator.enabled=false"
})
@ActiveProfiles("local")
@Import(AttendanceAutomationTestBase.FakeClock.class)
@RecordApplicationEvents
class AttendanceAutomationIntegrationTest extends AttendanceAutomationTestBase {

    @Test
    void staffChangingAnAutomaticMark_makesItManual_andItStaysSo() {
        at(9, 0);
        manual(0, 0, AttendanceStatus.ABSENT, 0);
        at(9, 40);
        automation.runFor(school.id());
        assertArrayEquals(new String[]{"ABSENT", "AUTO_CARRY"}, mark(0, 1, 0));

        manual(0, 1, AttendanceStatus.PRESENT, 0); // the child came after all
        assertArrayEquals(new String[]{"PRESENT", "MANUAL"}, mark(0, 1, 0));

        at(10, 40);
        automation.runFor(school.id());
        assertArrayEquals(new String[]{"PRESENT", "MANUAL"}, mark(0, 1, 0), "the next run does not touch it");
    }

    @Test
    void staffAndSchedulerAtTheSameTime_noDuplicate_andManualWins() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            for (int i = 0; i < 6; i++) {
                long studentId = student(1, i).getId();
                long lessonId = lesson(1, 0).getId();
                CountDownLatch go = new CountDownLatch(1);
                Future<?> staff = pool.submit(() -> {
                    go.await();
                    writer.saveManual(school.id(), lessonId, MONDAY,
                            List.of(new ManualMark(studentId, AttendanceStatus.PRESENT, null)), "ustoz");
                    return null;
                });
                Future<?> scheduler = pool.submit(() -> {
                    go.await();
                    writer.insertIfAbsent(school.id(),
                            List.of(new AutoMark(lessonId, studentId, MONDAY, AttendanceStatus.ABSENT, "DEMO simulyatsiya", null)),
                            AttendanceSource.DEMO_SIM);
                    return null;
                });
                go.countDown();
                staff.get();
                scheduler.get();
                assertArrayEquals(new String[]{"PRESENT", "MANUAL"}, mark(1, 0, i), "student " + i);
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void carry_copiesTheEarlierLesson_onlyAfterTheDelay_andNothingWithoutAnEarlierMark() {
        at(8, 40);
        manual(0, 0, AttendanceStatus.ABSENT, 0);
        manual(0, 0, AttendanceStatus.LATE, 1);
        manual(0, 0, AttendanceStatus.EXCUSED, 2);
        // students 3–5 have no mark in lesson 1

        at(9, 34); // lesson 2 started 9 minutes ago: too early
        assertEquals(0, automation.runFor(school.id()).carried());

        at(9, 35); // 10 minutes
        assertEquals(3, automation.runFor(school.id()).carried());
        assertArrayEquals(new String[]{"ABSENT", "AUTO_CARRY"}, mark(0, 1, 0));
        assertArrayEquals(new String[]{"PRESENT", "AUTO_CARRY"}, mark(0, 1, 1), "late → present");
        assertArrayEquals(new String[]{"EXCUSED", "AUTO_CARRY"}, mark(0, 1, 2));
        assertNull(mark(0, 1, 3), "no earlier mark: nothing is written");
        assertNull(mark(0, 0, 3), "the first lesson is never carried into");
    }

    @Test
    void carry_canBeSwitchedOff() {
        jdbc.update("""
                insert into school_settings (school_id, auto_carry_enabled, auto_carry_delay_minutes, reminder_minutes,
                  escalation_minutes, notify_on_auto, attendance_target, absent_streak_days, discipline_threshold,
                  appeal_overdue_hours, telegram_low_percent, heat_good, heat_ok, heat_warn)
                values (?, false, 10, 5, 10, false, 95, 3, 30, 24, 50, 95, 90, 85)""", school.id());
        settings.forget(school.id());
        at(8, 40);
        manual(0, 0, AttendanceStatus.ABSENT, 0);
        at(9, 40);
        assertEquals(0, automation.runFor(school.id()).carried());
        settings.forget(school.id());
    }

    @Test
    void reminder_fiveMinutesIn_once_onlyWhenNobodyTookIt() {
        at(8, 34);
        assertEquals(0, automation.runFor(school.id()).reminders());
        at(8, 35);
        assertEquals(2, automation.runFor(school.id()).reminders(), "both classes' first lessons");
        assertEquals(0, automation.runFor(school.id()).reminders(), "never twice for one lesson");
        manual(0, 1, AttendanceStatus.PRESENT, 0, 1, 2, 3, 4, 5);
        at(9, 31);
        assertEquals(1, automation.runFor(school.id()).reminders(), "class 0 took lesson 2, class 1 did not");
        String text = jdbc.queryForObject("select text from staff_notification where school_id = ? order by id limit 1",
                String.class, school.id());
        assertTrue(text.contains("davomat hali olinmagan"), text);
    }

    @Test
    void carriedMarks_sendNothingToParents_manualDoes() {
        at(8, 40);
        manual(0, 0, AttendanceStatus.ABSENT, 0);
        assertEquals(1, events.stream(AttendanceMarkedEvent.class).count(), "a manual absence is told");
        at(9, 40);
        assertEquals(1, automation.runFor(school.id()).carried());
        assertEquals(1, events.stream(AttendanceMarkedEvent.class).count(), "nothing for AUTO_CARRY (notifyOnAuto off)");
        Long carried = jdbc.queryForObject("select id from attendance where source = 'AUTO_CARRY' and lesson_slot_id = ?",
                Long.class, lesson(0, 1).getId());
        assertEquals(0, notifications.enqueueAttendance(new AttendanceMarkedEvent(carried, null, AttendanceStatus.ABSENT)),
                "the outbox refuses a carried mark too");
    }

    @Test
    void checkin_needsAValidKey_andMarksLateWithMinutes() throws Exception {
        DeviceCheckinService.CreatedKey key = asAdmin(() -> devices.create(school.id(), "Turniket", "admin"));
        HttpClient http = HttpClient.newHttpClient();
        String body = "{\"studentExternalId\":\"" + student(0, 3).getId() + "\",\"timestamp\":\"2031-03-03T08:47:00+05:00\",\"deviceId\":\"T1\"}";

        HttpResponse<String> bad = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/attendance/checkin"))
                .header("Content-Type", "application/json").header("X-Api-Key", "mk_wrong")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(401, bad.statusCode());

        at(8, 48);
        manual(0, 1, AttendanceStatus.ABSENT, 3); // a teacher already marked lesson 2 — the device must not change it
        HttpResponse<String> ok = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/attendance/checkin"))
                .header("Content-Type", "application/json").header("X-Api-Key", key.apiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, ok.statusCode(), ok.body());
        assertTrue(ok.body().contains("\"status\":\"LATE\"") && ok.body().contains("\"lateMinutes\":17"), ok.body());
        assertArrayEquals(new String[]{"LATE", "DEVICE"}, mark(0, 0, 3));
        assertEquals(17, jdbc.queryForObject("select late_minutes from attendance where lesson_slot_id = ? and student_id = ?",
                Integer.class, lesson(0, 0).getId(), student(0, 3).getId()));
        assertArrayEquals(new String[]{"ABSENT", "MANUAL"}, mark(0, 1, 3));
        assertArrayEquals(new String[]{"PRESENT", "DEVICE"}, mark(0, 2, 3));
        assertEquals(0, jdbc.queryForObject("select count(*) from device_api_key where key_hash = ? or prefix = ?",
                Integer.class, key.apiKey(), key.apiKey()), "only the hash of the key is stored");
    }
}
