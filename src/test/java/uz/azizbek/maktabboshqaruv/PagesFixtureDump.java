package uz.azizbek.maktabboshqaruv;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.service.CalendarEventService;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.pages.CalendarPageService;
import uz.azizbek.maktabboshqaruv.service.pages.ClassesPageService;
import uz.azizbek.maktabboshqaruv.service.pages.RoomsPageService;
import uz.azizbek.maktabboshqaruv.service.pages.TimetablePageService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Not a check: writes what the v2 pages' endpoints answer for school 1 of the test database
 * into one JSON file (path → body), so the UI tests can show the pages with real data and no
 * sign-in (tests/ui/pages-v2-mock.js serves it). Runs only when PAGES_FIXTURES names the file:
 *   DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test PAGES_FIXTURES=/path/fixtures.json \n *     ./gradlew test --tests '*PagesFixtureDump'
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "PAGES_FIXTURES", matches = ".+")
class PagesFixtureDump {

    static final long SCHOOL = 1;

    @Autowired
    TimetablePageService timetable;
    @Autowired
    CalendarPageService calendar;
    @Autowired
    CalendarEventService events;
    @Autowired
    ClassesPageService classes;
    @Autowired
    RoomsPageService rooms;
    @Autowired
    ObjectMapper mapper;

    static <T> DashboardCache.Widget<T> w(T data) {
        return new DashboardCache.Widget<>(LocalDateTime.now().withNano(0), data);
    }

    @Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void dump() throws Exception {
        // the fill below writes: only ever on the test database, never on the live maktab_db
        String db = jdbc.queryForObject("select current_database()", String.class);
        if (!"maktab_tg_test".equals(db)) {
            throw new IllegalStateException("PagesFixtureDump faqat maktab_tg_test da ishlaydi (DB_URL), hozir: " + db);
        }
        // what demo mode gives the rooms (only rooms with nothing filled in)
        new uz.azizbek.maktabboshqaruv.config.RoomDetailsDemoFill(jdbc,
                new uz.azizbek.maktabboshqaruv.service.attendance.DemoMode(new org.springframework.mock.env.MockEnvironment(), true, 10)).fill();
        Map<String, Object> out = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        out.put("today", today.toString());

        TimetablePageService.Week week = timetable.week(SCHOOL, true);
        out.put("/api/pages/timetable/week", week);
        out.put("/api/pages/timetable/kpis", w(timetable.kpis(SCHOOL)));
        out.put("/api/pages/timetable/day", timetable.day(SCHOOL, today));
        if (!week.lessons().isEmpty()) {
            TimetablePageService.Lesson l = week.lessons().get(0);
            LocalDate d = today;
            while (!uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays.WEEKDAY_UZ.getOrDefault(d.getDayOfWeek(), "").equals(l.weekday())) {
                d = d.plusDays(1);
            }
            out.put("/api/pages/timetable/substitutes", timetable.candidates(SCHOOL, l.id(), d));
        }

        out.put("/api/pages/calendar/overview", w(calendar.overview(SCHOOL)));
        LocalDate from = today.withDayOfMonth(1).minusDays(7);
        LocalDate to = today.withDayOfMonth(today.lengthOfMonth()).plusDays(14);
        out.put("/api/pages/calendar/attendance", calendar.attendance(SCHOOL, from, to));
        out.put("/api/pages/calendar/day", calendar.day(SCHOOL, today));
        int y = today.getMonthValue() >= 8 ? today.getYear() : today.getYear() - 1;
        out.put("/api/calendar-events/range", events.getInRange(SCHOOL, LocalDate.of(y - 1, 8, 1), LocalDate.of(y + 1, 8, 31)));

        ClassesPageService.Overview ov = classes.overview(SCHOOL);
        out.put("/api/pages/classes/overview", w(ov));
        if (!ov.classes().isEmpty()) {
            long first = ov.classes().get(0).id();
            out.put("/api/pages/classes/detail", classes.detail(SCHOOL, first, true));
            out.put("/api/pages/classes/month", classes.month(SCHOOL, first, today));
            if (ov.classes().size() > 2) {
                out.put("/api/pages/classes/compare", classes.compare(SCHOOL,
                        List.of(ov.classes().get(0).id(), ov.classes().get(1).id(), ov.classes().get(2).id())));
            }
        }

        out.put("/api/pages/rooms/live", rooms.live(SCHOOL, null, null));
        // a lesson time on a school day, for a busy map whatever the hour is now
        LocalDate lessonDay = today.getDayOfWeek() == java.time.DayOfWeek.SUNDAY ? today.plusDays(1) : today;
        out.put("/api/pages/rooms/live@lesson", rooms.live(SCHOOL, lessonDay, LocalTime.of(10, 40)));
        out.put("/api/pages/rooms/analytics", w(rooms.analytics(SCHOOL)));
        out.put("/api/pages/rooms/find", rooms.find(SCHOOL, lessonDay, List.of(2), 20, null, List.of("PROJECTOR")));
        RoomsPageService.Live live = rooms.live(SCHOOL, lessonDay, LocalTime.of(10, 40));
        if (!live.buildings().isEmpty()) {
            long roomId = live.buildings().get(0).floors().get(0).rooms().get(0).room().id();
            out.put("/api/pages/rooms/detail", rooms.detail(SCHOOL, roomId));
        }

        Path file = Path.of(System.getenv("PAGES_FIXTURES"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
    }
}
