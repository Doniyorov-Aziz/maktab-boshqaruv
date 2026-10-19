package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.RecordApplicationEvents;
import uz.azizbek.maktabboshqaruv.dto.CalendarEventRequestDto;
import uz.azizbek.maktabboshqaruv.dto.CalendarEventResponseDto;
import uz.azizbek.maktabboshqaruv.dto.LessonSlotRequestDto;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.ParentTelegramLinkRepository;
import uz.azizbek.maktabboshqaruv.service.CalendarEventService;
import uz.azizbek.maktabboshqaruv.service.EventNotifier;
import uz.azizbek.maktabboshqaruv.service.LessonSlotService;
import uz.azizbek.maktabboshqaruv.service.RoomBookingService;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoMode;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.pages.CalendarPageService;
import uz.azizbek.maktabboshqaruv.service.pages.ClassesPageService;
import uz.azizbek.maktabboshqaruv.service.pages.RoomsPageService;
import uz.azizbek.maktabboshqaruv.service.pages.TimetablePageService;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The four v2 pages in a school of their own (Monday 2031-03-03): timetable conflicts are 409 with
 * the reason, a substitute changes one date only, the free-room finder counts the timetable and
 * bookings, moving an event re-checks its room, the .ics file follows RFC 5545, a new event goes
 * to the Telegram outbox (not in demo mode), and another school never leaks in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
@Import(AttendanceAutomationTestBase.FakeClock.class)
@RecordApplicationEvents
class PagesV2IntegrationTest extends AttendanceAutomationTestBase {

    static final LocalDate TUESDAY = MONDAY.plusDays(1);
    static final LocalDate WEDNESDAY = MONDAY.plusDays(2);
    static final LocalDate THURSDAY = MONDAY.plusDays(3);

    @Autowired
    LessonSlotService lessonSlots;
    @Autowired
    TimetablePageService timetable;
    @Autowired
    RoomsPageService roomsPage;
    @Autowired
    CalendarPageService calendarPage;
    @Autowired
    ClassesPageService classesPage;
    @Autowired
    CalendarEventService events;
    @Autowired
    CalendarEventRepository eventRepo;
    @Autowired
    RoomBookingService bookings;
    @Autowired
    EventNotifier notifier;
    @Autowired
    ParentTelegramLinkRepository links;
    @Autowired
    uz.azizbek.maktabboshqaruv.service.NotificationService notificationService;
    @Autowired
    DashboardCache cache;
    @Autowired
    PasswordEncoder encoder;

    private final List<TestSchool> others = new ArrayList<>();
    private final List<String> users = new ArrayList<>();

    @AfterEach
    void cleanUpMore() {
        users.forEach(u -> jdbc.update("delete from app_user where username = ?", u));
        others.forEach(TestSchool::delete);
        cache.clear();
    }

    // ------------------------------------------------------------------ helpers

    Employee teacher2() {
        Employee e = new Employee();
        e.setSchool(school.school);
        e.setPosition(school.teacher.getPosition());
        e.setFirstName("Dilnoza");
        e.setLastName("Karimova");
        e.setPhone("+9989" + (60_000_000 + (int) (school.id() % 10_000_000)));
        return employees.save(e);
    }

    Room room2() {
        Room r = new Room();
        r.setBuilding(school.room.getBuilding());
        r.setRoomNumber("204");
        r.setCapacity(24);
        r.setFloor(2);
        return rooms.save(r);
    }

    LessonSlotRequestDto slot(SchoolClass c, Employee t, Room r, String weekday, LocalTime start) {
        LessonSlotRequestDto d = new LessonSlotRequestDto();
        d.setSchoolClassId(c.getId());
        d.setSubjectId(school.subject.getId());
        d.setEmployeeId(t.getId());
        d.setRoomId(r.getId());
        d.setWeekday(weekday);
        d.setStartTime(start);
        d.setEndTime(start.plusMinutes(45));
        return d;
    }

    CalendarEventRequestDto event(String title, LocalDate day, LocalTime start, LocalTime end, Long roomId) {
        CalendarEventRequestDto r = new CalendarEventRequestDto();
        r.setSchoolId(school.id());
        r.setTitle(title);
        r.setType(CalendarEventType.SCHOOL_EVENT);
        r.setStartDate(day);
        r.setEndDate(day);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setRoomId(roomId);
        return r;
    }

    String message(Runnable action) {
        IllegalStateException e = assertThrows(IllegalStateException.class, action::run);
        return e.getMessage();
    }

    // ------------------------------------------------------------------ 1. conflicts

    @Test
    void timetableConflicts_teacherRoomAndClass_are409WithTheReason() throws Exception {
        Employee t2 = teacher2();
        Room r2 = room2();
        SchoolClass a = school.classes.get(0), b = school.classes.get(1);
        lessonSlots.createLessonSlot(slot(a, school.teacher, school.room, "Seshanba", LocalTime.of(9, 0)));

        assertEquals("O'qituvchi T. bu vaqtda 5-A da",
                message(() -> lessonSlots.createLessonSlot(slot(b, school.teacher, r2, "Seshanba", LocalTime.of(9, 20)))));
        assertTrue(message(() -> lessonSlots.createLessonSlot(slot(b, t2, school.room, "Seshanba", LocalTime.of(9, 20))))
                .startsWith("301-xona bu vaqtda band (5-A, "));
        assertTrue(message(() -> lessonSlots.createLessonSlot(slot(a, t2, r2, "Seshanba", LocalTime.of(9, 30))))
                .startsWith("5-A sinfida bu vaqtda dars bor ("));
        // back to back is not a conflict
        lessonSlots.createLessonSlot(slot(b, school.teacher, r2, "Seshanba", LocalTime.of(9, 45)));

        // and over HTTP it is a 409 with that text
        String username = "pages_admin_" + school.id();
        String password = UUID.randomUUID().toString();
        jdbc.update("insert into app_user (username, password, role, employee_id) values (?, ?, 'ADMIN', ?)",
                username, encoder.encode(password), school.teacher.getId());
        users.add(username);
        ApiClient api = new ApiClient(port).login(username, password);
        ApiClient.Response r = api.post("/api/lesson-slots", Map.of("schoolClassId", b.getId(), "subjectId", school.subject.getId(),
                "employeeId", school.teacher.getId(), "roomId", school.room.getId(), "weekday", "Seshanba",
                "startTime", "09:00:00", "endTime", "09:45:00"));
        assertEquals(409, r.status());
        assertTrue(r.body().contains("bu vaqtda 5-A da"), r.body());

        // the page's hero finds the fixture's own clash (both classes share one teacher and room on Monday)
        TimetablePageService.Kpis k = timetable.kpis(school.id());
        assertFalse(k.conflicts().isEmpty());
        assertEquals("TEACHER", k.conflicts().get(0).kind());
    }

    // ------------------------------------------------------------------ 2. substitute

    @Test
    void substitute_changesOnlyThatOneDate() {
        Employee t2 = teacher2();
        Room r2 = room2();
        // t2 teaches something (on Tuesday), so it is a teacher and free on Monday at 08:30
        lessonSlots.createLessonSlot(slot(school.classes.get(0), t2, r2, "Seshanba", LocalTime.of(11, 0)));
        LessonSlot first = lesson(0, 0);

        TimetablePageService.Candidates c = timetable.candidates(school.id(), first.getId(), MONDAY);
        assertTrue(c.candidates().stream().anyMatch(x -> x.id() == t2.getId()), "the free teacher is offered");
        assertTrue(c.candidates().stream().noneMatch(x -> x.id() == school.teacher.getId()), "never the lesson's own teacher");
        assertEquals("SAME", c.candidates().get(0).match(), "t2 teaches the same subject");

        timetable.substitute(school.id(), first.getId(), MONDAY, t2.getId(), "kasal", "admin");
        assertEquals(1, timetable.day(school.id(), MONDAY).substitutions().size());
        assertTrue(timetable.day(school.id(), MONDAY.plusWeeks(1)).substitutions().isEmpty(), "next Monday is as usual");
        assertEquals(school.teacher.getId(), lessons.findById(first.getId()).orElseThrow().getEmployee().getId(),
                "the weekly timetable is unchanged");
        assertTrue(timetable.week(school.id(), true).lessons().stream()
                .filter(l -> l.id() == first.getId()).allMatch(l -> l.teacherId() == school.teacher.getId()));

        // a Tuesday date for a Monday lesson is refused
        assertThrows(IllegalStateException.class, () -> timetable.substitute(school.id(), first.getId(), TUESDAY, t2.getId(), null, "admin"));
        // t2 is now busy at 08:30 on Monday: not offered for the other class's 08:30 lesson
        TimetablePageService.Candidates other = timetable.candidates(school.id(), lesson(1, 0).getId(), MONDAY);
        assertTrue(other.candidates().stream().noneMatch(x -> x.id() == t2.getId()));
    }

    // ------------------------------------------------------------------ 3. free rooms

    @Test
    void freeRoomFinder_countsTheTimetableAndBookings() {
        Room r2 = room2();
        RoomsPageService.Finder p1 = roomsPage.find(school.id(), MONDAY, List.of(1), null, null, List.of());
        assertTrue(p1.rooms().stream().anyMatch(m -> m.room().id() == r2.getId()));
        assertTrue(p1.rooms().stream().noneMatch(m -> m.room().id() == school.room.getId()), "301 has lessons in period 1");

        bookings.create(new RoomBookingService.BookingRequest(school.id(), r2.getId(), MONDAY, LocalTime.of(9, 30),
                LocalTime.of(10, 0), "Yig'ilish", "Direktor"));
        RoomsPageService.Finder p2 = roomsPage.find(school.id(), MONDAY, List.of(2), null, null, List.of());
        assertTrue(p2.rooms().stream().noneMatch(m -> m.room().id() == r2.getId()), "booked in period 2");
        assertTrue(roomsPage.find(school.id(), MONDAY, List.of(1), null, null, List.of()).rooms().stream()
                .anyMatch(m -> m.room().id() == r2.getId()), "still free in period 1");

        // a booking over a lesson, or over another booking, is refused
        assertTrue(message(() -> bookings.create(new RoomBookingService.BookingRequest(school.id(), school.room.getId(), MONDAY,
                LocalTime.of(8, 40), LocalTime.of(9, 0), "Qo'shimcha dars", null))).startsWith("301-xona bu vaqtda band: "));
        assertTrue(message(() -> bookings.create(new RoomBookingService.BookingRequest(school.id(), r2.getId(), MONDAY,
                LocalTime.of(9, 45), LocalTime.of(10, 15), "Boshqa", null))).contains("band qilingan: Yig'ilish"));

        // the live map: at 09:40 the room is booked, at 08:40 301 is in a lesson
        at(9, 40);
        RoomsPageService.Live live = roomsPage.live(school.id(), null, null);
        RoomsPageService.LiveRoom l2 = liveRoom(live, r2.getId());
        assertEquals("BOOKED", l2.state());
        RoomsPageService.LiveRoom l301 = liveRoom(roomsPage.live(school.id(), MONDAY, LocalTime.of(8, 40)), school.room.getId());
        assertEquals("BUSY", l301.state());
        assertEquals(LocalTime.of(9, 15), l301.until());
        // in the break between lessons 1 and 2 (09:15–09:25) it is free until 09:25
        RoomsPageService.LiveRoom brk = liveRoom(roomsPage.live(school.id(), MONDAY, LocalTime.of(9, 20)), school.room.getId());
        assertEquals("FREE", brk.state());
        assertEquals(LocalTime.of(9, 25), brk.until());
    }

    static RoomsPageService.LiveRoom liveRoom(RoomsPageService.Live live, long roomId) {
        return live.buildings().stream().flatMap(b -> b.floors().stream()).flatMap(f -> f.rooms().stream())
                .filter(r -> r.room().id() == roomId).findFirst().orElseThrow();
    }

    // ------------------------------------------------------------------ 4. events and rooms

    @Test
    void movingAnEvent_rechecksItsRoom() {
        Room r2 = room2();
        CalendarEventResponseDto ev = events.createEvent(event("Konsert", TUESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), r2.getId()));
        assertEquals(1, bookings.inRange(school.id(), TUESDAY, TUESDAY).size(), "the room is booked for the event");

        bookings.create(new RoomBookingService.BookingRequest(school.id(), r2.getId(), WEDNESDAY, LocalTime.of(10, 30),
                LocalTime.of(11, 30), "Olimpiada", null));
        CalendarEventRequestDto moved = event("Konsert", WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), r2.getId());
        assertTrue(message(() -> events.updateEvent(ev.getId(), moved)).contains("Olimpiada"));
        assertEquals(TUESDAY, eventRepo.findById(ev.getId()).orElseThrow().getStartDate(), "a refused move changes nothing");
        assertEquals(1, bookings.inRange(school.id(), TUESDAY, TUESDAY).size());

        events.updateEvent(ev.getId(), event("Konsert", THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 0), r2.getId()));
        assertTrue(bookings.inRange(school.id(), TUESDAY, TUESDAY).isEmpty(), "the booking moved with the event");
        assertEquals(1, bookings.inRange(school.id(), THURSDAY, THURSDAY).size());

        // a lesson is in the way too
        assertTrue(message(() -> events.createEvent(event("Yig'ilish", MONDAY, LocalTime.of(8, 30), LocalTime.of(9, 0),
                school.room.getId()))).startsWith("301-xona bu vaqtda band"));
    }

    // ------------------------------------------------------------------ 5. .ics

    @Test
    void ics_followsRfc5545() {
        CalendarEventRequestDto whole = event("Navro'z bayrami, dam olish; hammaga", LocalDate.of(2031, 3, 21), null, null, null);
        whole.setType(CalendarEventType.HOLIDAY);
        whole.setEndDate(LocalDate.of(2031, 3, 22));
        whole.setDescription("Birinchi qator\nikkinchi qator — " + "uzun matn ".repeat(12));
        events.createEvent(whole);
        events.createEvent(event("Ota-onalar yig'ilishi", TUESDAY, LocalTime.of(17, 0), LocalTime.of(18, 30), null));

        String ics = new String(calendarPage.ics(school.id(), "maktab.test"), StandardCharsets.UTF_8);
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
        assertFalse(ics.replace("\r\n", "").contains("\n"), "every line ends in CRLF");
        for (String line : ics.split("\r\n")) {
            assertTrue(line.getBytes(StandardCharsets.UTF_8).length <= 75, "folded at 75 octets: " + line);
        }
        String unfolded = ics.replace("\r\n ", "");
        assertTrue(unfolded.contains("TZID:Asia/Tashkent"));
        assertTrue(unfolded.contains("TZOFFSETTO:+0500"));
        assertTrue(unfolded.contains("DTSTART;VALUE=DATE:20310321"));
        assertTrue(unfolded.contains("DTEND;VALUE=DATE:20310323"), "the end of a whole-day event is exclusive");
        assertTrue(unfolded.contains("DTSTART;TZID=Asia/Tashkent:20310304T170000"));
        assertTrue(unfolded.contains("DTEND;TZID=Asia/Tashkent:20310304T183000"));
        assertTrue(unfolded.contains("SUMMARY:Navro'z bayrami\\, dam olish\\; hammaga"));
        assertTrue(unfolded.contains("DESCRIPTION:Birinchi qator\\nikkinchi qator"));
        assertEquals(2, unfolded.split("BEGIN:VEVENT").length - 1);
        assertTrue(unfolded.matches("(?s).*DTSTAMP:\\d{8}T\\d{6}Z.*"));

        // a subscription link reads the same file; a revoked one is gone
        CalendarPageService.Feed feed = calendarPage.createFeed(school.id(), "admin");
        assertTrue(new String(calendarPage.feed(feed.token(), "maktab.test"), StandardCharsets.UTF_8).contains("BEGIN:VEVENT"));
        calendarPage.revokeFeed(school.id(), feed.id());
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> calendarPage.feed(feed.token(), "x"));
    }

    // ------------------------------------------------------------------ 6. Telegram

    @Test
    void newEvent_goesToTheOutbox_butInDemoModeOnlyToTheLog() {
        Student kid = student(0, 0);
        long chat = 900_000_000L + school.id();
        jdbc.update("insert into parent_telegram_link (active, chat_id, first_name, linked_at, student_id) values (true, ?, 'Ota', now(), ?)",
                chat, kid.getId());

        CalendarEventRequestDto r = event("Ochiq dars", TUESDAY, LocalTime.of(14, 0), LocalTime.of(15, 0), null);
        r.setNotifyParents(true);
        r.setClassIds(List.of(school.classes.get(0).getId()));
        CalendarEventResponseDto ev = events.createEvent(r);
        Integer rows = jdbc.queryForObject("select count(*) from notification_log where school_id = ? and reference_id = ? and chat_id = ?",
                Integer.class, school.id(), ev.getId(), chat);
        assertEquals(1, rows, "one message to the class's parent");

        // demo mode: nothing is queued
        EventNotifier demo = new EventNotifier(links, notificationService, new DemoMode(new MockEnvironment(), true, 10), clock);
        CalendarEvent second = eventRepo.findById(ev.getId()).orElseThrow();
        jdbc.update("delete from notification_log where school_id = ?", school.id());
        assertEquals(0, demo.notifyCreated(second));
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_log where school_id = ?", Integer.class, school.id()));

        // no "tell the parents": no message
        CalendarEventRequestDto quiet = event("Ichki yig'ilish", TUESDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), null);
        quiet.setNotifyParents(false);
        events.createEvent(quiet);
        assertEquals(0, jdbc.queryForObject("select count(*) from notification_log where school_id = ?", Integer.class, school.id()));
    }

    // ------------------------------------------------------------------ 7. another school

    @Test
    void anotherSchool_neverLeaksIn() {
        TestSchool other = TestSchool.create(new TestSchool.Repos(schools, years, positions, employees, subjects, buildings, rooms,
                classes, lessons, students), jdbc, 1, 3);
        others.add(other);

        assertTrue(timetable.week(school.id(), true).lessons().stream()
                .allMatch(l -> school.classes.stream().anyMatch(c -> c.getId() == l.classId())));
        RoomsPageService.Live live = roomsPage.live(school.id(), MONDAY, LocalTime.of(8, 40));
        assertTrue(live.buildings().stream().flatMap(b -> b.floors().stream()).flatMap(f -> f.rooms().stream())
                .noneMatch(r -> r.room().id() == other.room.getId()));
        assertEquals(2, classesPage.overview(school.id()).classes().size());

        // ids of the other school are refused even with our school id
        assertThrows(ForbiddenException.class, () -> timetable.candidates(school.id(), other.lessons.get(0)[0].getId(), MONDAY));
        Employee t2 = teacher2();
        assertThrows(ForbiddenException.class, () -> timetable.substitute(other.id(), other.lessons.get(0)[0].getId(), MONDAY,
                t2.getId(), null, "admin"));
        RoomBookingService.BookingView theirs = bookings.create(new RoomBookingService.BookingRequest(other.id(),
                other.room.getId(), TUESDAY, LocalTime.of(15, 0), LocalTime.of(16, 0), "Ularniki", null));
        assertThrows(ForbiddenException.class, () -> bookings.delete(theirs.id(), school.id()));
        assertThrows(ForbiddenException.class, () -> bookings.create(new RoomBookingService.BookingRequest(school.id(),
                other.room.getId(), TUESDAY, LocalTime.of(16, 0), LocalTime.of(17, 0), "Begona", null)));
        assertTrue(calendarPage.day(school.id(), TUESDAY).events().isEmpty());
    }

    // ------------------------------------------------------------------ 8. classes page numbers

    @Test
    void classCards_useTheHomePageFormulas() {
        at(10, 40);
        manual(0, 0, AttendanceStatus.PRESENT, 0, 1, 2, 3, 4);
        manual(0, 0, AttendanceStatus.ABSENT, 5);
        ClassesPageService.Overview o = classesPage.overview(school.id());
        ClassesPageService.ClassCard a = o.classes().get(0);
        assertEquals(83.3, a.today(), "5 of 6, as on the home page");
        assertNull(o.classes().get(1).today(), "no marks: no number");
        assertEquals(12, o.students());
        ClassesPageService.Detail d = classesPage.detail(school.id(), a.id(), false);
        assertEquals(6, d.students().size());
        assertTrue(d.students().stream().allMatch(s -> s.guardianPhone() == null), "no phones without the right");
        assertEquals(Map.of(5, 0, 4, 0, 3, 0, 2, 0), d.gradeCounts());
    }
}
