package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import uz.azizbek.maktabboshqaruv.entity.DemoVisit;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.DemoExcludedVisitorRepository;
import uz.azizbek.maktabboshqaruv.repository.DemoVisitRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.support.ApiClient;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The public demo visitor (GUEST, from /api/auth/guest-login): reads every page an ADMIN reads,
 * changes nothing — every POST/PUT/PATCH/DELETE of every endpoint is 403, collected from the
 * application's own request mappings so a new endpoint is checked without editing this test —
 * cannot open parents' messages, Telegram history, link codes or the visit statistics, cannot
 * export, and never sees chat ids, tokens or password hashes. The administrator is not affected.
 * Visits: one browser twice = 1 visitor, 2 visits; an administrator's browser is not counted;
 * the IP address is stored only as a hash.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "app.guest.login-enabled=true",
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class GuestDemoIntegrationTest {

    private static final String BROWSER_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0 Safari/537.36";
    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final Set<String> WRITES = Set.of("POST", "PUT", "PATCH", "DELETE");
    /** The only writes a guest may make (GuestPolicy.WRITE_ALLOWED). */
    private static final Set<String> GUEST_WRITES = Set.of("/api/auth/guest-login", "/api/auth/login", "/api/auth/logout",
            "/api/demo/visit/ping");

    @LocalServerPort
    private int port;
    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping mappings;
    @Autowired
    private UserRepository users;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private DemoVisitRepository visits;
    @Autowired
    private DemoExcludedVisitorRepository excluded;
    @Autowired
    private JdbcTemplate jdbc;

    private ApiClient guest;
    private ApiClient admin;
    private User adminUser;
    private final List<String> visitorIds = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        ApiClient anon = new ApiClient(port).header("User-Agent", BROWSER_UA);
        ApiClient.Response r = anon.post("/api/auth/guest-login", Map.of("visitorId", newVisitor(), "ref", "linkedin"));
        assertEquals(200, r.status(), r.body());
        guest = new ApiClient(port).header("User-Agent", BROWSER_UA).withToken(r.body().trim());

        User u = new User();
        u.setUsername("guest_test_admin_" + ThreadLocalRandom.current().nextInt(1_000_000));
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(Role.ADMIN);
        adminUser = users.save(u);
        admin = new ApiClient(port).header("User-Agent", BROWSER_UA).login(adminUser.getUsername(), PASSWORD);
    }

    @AfterEach
    void cleanUp() {
        users.delete(adminUser);
        for (String id : visitorIds) {
            jdbc.update("delete from demo_visit where visitor_id = ?", id);
            jdbc.update("delete from demo_excluded_visitor where visitor_id = ?", id);
        }
    }

    private String newVisitor() {
        String id = UUID.randomUUID().toString();
        visitorIds.add(id);
        return id;
    }

    // ------------------------------------------------------------------ read-only, everywhere

    @Test
    void everyWriteOfEveryEndpointIs403ForTheGuest() throws Exception {
        List<String> checked = new ArrayList<>();
        List<String> wrong = new ArrayList<>();
        for (Map.Entry<RequestMappingInfo, ?> e : mappings.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = e.getKey();
            Set<RequestMethod> methods = info.getMethodsCondition().getMethods();
            Set<String> names = new TreeSet<>();
            if (methods.isEmpty()) names.addAll(WRITES);
            for (RequestMethod m : methods) if (WRITES.contains(m.name())) names.add(m.name());
            for (String pattern : info.getPatternValues()) {
                if (!pattern.startsWith("/api/") || GUEST_WRITES.contains(pattern)) continue;
                String path = pattern.replaceAll("\\{[^}]+}", "1");
                for (String method : names) {
                    ApiClient.Response r = guest.send(method, path + "?schoolId=1", "{}");
                    checked.add(method + " " + path);
                    if (r.status() != 403 || !r.body().contains("Demo rejimida o'zgartirish mumkin emas")) {
                        wrong.add(method + " " + path + " -> " + r.status() + " " + r.body());
                    }
                }
            }
        }
        assertTrue(checked.size() > 100, "too few endpoints found: " + checked.size());
        assertEquals(List.of(), wrong);
    }

    @Test
    void lockedSectionsAre403EvenToRead() throws Exception {
        String[] locked = {
                "/api/appeals?schoolId=1", "/api/appeals/unread?schoolId=1", "/api/appeals/1",
                "/api/appeals/1/attachments/1", "/api/dashboard/my-appeals?schoolId=1",
                "/api/absence-requests?schoolId=1", "/api/absence-requests/1/photo",
                "/api/notifications?schoolId=1", "/api/notifications/stats?schoolId=1", "/api/notifications/settings?schoolId=1",
                "/api/broadcasts?schoolId=1", "/api/broadcasts/1", "/api/broadcasts/1/recipients",
                "/api/telegram/students/1", "/api/telegram/classes/1/codes", "/api/telegram/files/abc",
                "/api/telegram/mock/messages?chatId=1", "/api/library/stats/parents/1?schoolId=1",
                "/api/library/permissions/1/document", "/api/library/wikisource/works?q=a",
                "/api/device-keys?schoolId=1", "/api/pages/calendar/feeds?schoolId=1",
                "/api/demo/visits/stats", "/api/demo/visits/summary", "/api/auth/hash/abc"
        };
        for (String path : locked) {
            ApiClient.Response r = guest.get(path);
            assertEquals(403, r.status(), path + " " + r.body());
            assertEquals("LOCKED", r.json().get("reason").asString(), path);
        }
    }

    @Test
    void exportsAre403() throws Exception {
        String month = LocalDate.now().withDayOfMonth(1).toString();
        String[] exports = {
                "/api/attendance/calendar/export?schoolId=1&month=" + month,
                "/api/pages/journal/export.xlsx?schoolId=1", "/api/pages/journal/export.pdf?schoolId=1",
                "/api/dashboard/risk/export?schoolId=1", "/api/lesson-slots/timetable.pdf?schoolId=1",
                "/api/library/stats/journal/export?schoolId=1", "/api/pages/calendar/export.ics?schoolId=1",
                "/api/library/permissions/template"
        };
        for (String path : exports) {
            ApiClient.Response r = guest.get(path);
            assertEquals(403, r.status(), path + " " + r.body());
            assertEquals("EXPORT", r.json().get("reason").asString(), path);
        }
    }

    /** The pages of the app, read the way the pages read them: 200 for the guest, as for the admin. */
    String[] openPages() {
        long classId = jdbc.queryForObject("select min(sc.id) from school_class sc join academic_year y on y.id = sc.academic_year_id "
                + "where y.school_id = 1", Long.class);
        String today = LocalDate.now().toString();
        String month = LocalDate.now().toString().substring(0, 7);
        long subjectId = jdbc.queryForObject("select min(subject_id) from lesson_slot where class_id = ?", Long.class, classId);
        return new String[]{
                "/api/dashboard/home?schoolId=1", "/api/dashboard/kpis?schoolId=1", "/api/dashboard/attention?schoolId=1",
                "/api/pages/attendance/day?schoolId=1&classId=" + classId + "&date=" + today,
                "/api/attendance/calendar?schoolId=1&schoolClassId=" + classId + "&month=" + month,
                "/api/pages/journal/summary?schoolId=1&classId=" + classId + "&subjectId=" + subjectId + "&month=" + month,
                "/api/grades/gradebook?schoolId=1&schoolClassId=" + classId + "&subjectId=" + subjectId + "&from=" + month + "-01&to=" + today,
                "/api/pages/timetable/week?schoolId=1", "/api/lesson-slots/timetable?schoolId=1",
                "/api/pages/calendar/overview?schoolId=1",
                "/api/pages/classes/overview?schoolId=1", "/api/school-classes?schoolId=1",
                "/api/pages/rooms/live?schoolId=1", "/api/rooms?schoolId=1",
                "/api/students?schoolId=1", "/api/pages/lists/students/hero?schoolId=1",
                "/api/employees?schoolId=1", "/api/pages/lists/employees/hero?schoolId=1",
                "/api/library/books?schoolId=1", "/api/library/categories?schoolId=1", "/api/library/authors",
                "/api/library/stats?schoolId=1", "/api/library/stats/journal?schoolId=1",
                "/api/bot/stats?schoolId=1", "/api/bot/settings?schoolId=1", "/api/telegram/status",
                "/api/telegram/parents?schoolId=1&classId=" + classId,
                "/api/users", "/api/schools", "/api/academic-years?schoolId=1", "/api/subjects?schoolId=1",
                "/api/positions", "/api/announcements?schoolId=1", "/api/calendar-events?schoolId=1",
                "/api/school-settings?schoolId=1", "/api/system/info", "/api/regions"
        };
    }

    @Test
    void everyPageReadsWithStatus200LikeTheAdmin() throws Exception {
        List<String> wrong = new ArrayList<>();
        for (String path : openPages()) {
            ApiClient.Response a = admin.get(path);
            ApiClient.Response g = guest.get(path);
            if (a.status() != 200 || g.status() != 200) {
                wrong.add(path + " admin=" + a.status() + " guest=" + g.status() + " " + g.body());
            }
        }
        assertEquals(List.of(), wrong);
    }

    private static final Pattern PASSWORD_HASH = Pattern.compile("\\$2[aby]\\$\\d\\d\\$");
    private static final Pattern CHAT_ID = Pattern.compile("\"(chatId|chat_id|telegramUserId|telegramId)\"\\s*:\\s*\"?-?\\d{3,}");
    private static final Pattern BOT_TOKEN = Pattern.compile("\\d{6,12}:[A-Za-z0-9_-]{30,}");

    @Test
    void guestAnswersHoldNoChatIdsTokensOrPasswordHashes() throws Exception {
        List<String> leaks = new ArrayList<>();
        for (String path : openPages()) {
            String body = guest.get(path).body();
            if (PASSWORD_HASH.matcher(body).find()) leaks.add(path + ": password hash");
            if (CHAT_ID.matcher(body).find()) leaks.add(path + ": chat id");
            if (BOT_TOKEN.matcher(body).find()) leaks.add(path + ": bot token");
            if (body.contains("\"linkCode\":\"") && !body.contains("\"linkCode\":\"****\"") && body.matches("(?s).*\"linkCode\":\"[^*\"]{5,}\".*")) {
                leaks.add(path + ": link code");
            }
        }
        assertEquals(List.of(), leaks);
    }

    @Test
    void chatIdsAreMaskedNotDropped() throws Exception {
        // the reading journal lists parents by chat id: the admin sees the number, the guest a mask
        int inserted = jdbc.update("insert into book_download (school_id, book_id, chat_id, downloaded_at, source) "
                + "select 1, b.id, 998901234567, now(), 'SEARCH' from book b where b.school_id = 1 limit 1");
        assertEquals(1, inserted, "the test database needs a library book in school 1");
        try {
            String adminBody = admin.get("/api/library/stats/journal?schoolId=1").body();
            String guestBody = guest.get("/api/library/stats/journal?schoolId=1").body();
            assertTrue(adminBody.contains("998901234567"), adminBody);
            assertFalse(guestBody.contains("998901234567"), guestBody);
            assertTrue(guestBody.contains("99****67"), guestBody);
        } finally {
            jdbc.update("delete from book_download where chat_id = 998901234567");
        }
    }

    @Test
    void theAdministratorIsNotAffected() throws Exception {
        assertEquals(200, admin.get("/api/appeals?schoolId=1").status());
        assertEquals(200, admin.get("/api/notifications?schoolId=1").status());
        assertEquals(200, admin.get("/api/broadcasts?schoolId=1").status());
        assertEquals(200, admin.get("/api/demo/visits/stats").status());
        assertEquals(200, admin.get("/api/dashboard/risk/export?schoolId=1").status());
        // a write: an event created and removed again
        ApiClient.Response created = admin.post("/api/calendar-events", Map.of(
                "schoolId", 1, "title", "Guest test", "type", "HOLIDAY",
                "startDate", "2031-01-01", "endDate", "2031-01-01"));
        assertTrue(created.status() == 200 || created.status() == 201, created.body());
        assertEquals(200, admin.delete("/api/calendar-events/" + created.json().get("id").asLong()).status());
        // the same event for the guest: 403 and nothing written
        long before = jdbc.queryForObject("select count(*) from calendar_event", Long.class);
        assertEquals(403, guest.post("/api/calendar-events", Map.of("schoolId", 1, "title", "Guest test",
                "type", "HOLIDAY", "startDate", "2031-01-01", "endDate", "2031-01-01")).status());
        assertEquals(before, jdbc.queryForObject("select count(*) from calendar_event", Long.class));
    }

    @Test
    void theGuestCannotSignInWithAPasswordAndHasAnEightHourToken() throws Exception {
        ApiClient.Response r = new ApiClient(port).send("POST", "/api/auth/login", "{\"username\":\"mehmon\",\"password\":\"x\"}");
        assertEquals(401, r.status());
        String token = new ApiClient(port).header("User-Agent", BROWSER_UA)
                .post("/api/auth/guest-login", Map.of()).body().trim();
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        long iat = Long.parseLong(payload.replaceAll(".*\"iat\":(\\d+).*", "$1"));
        long exp = Long.parseLong(payload.replaceAll(".*\"exp\":(\\d+).*", "$1"));
        assertEquals(8 * 3600, exp - iat);
        assertTrue(payload.contains("\"role\":\"GUEST\""), payload);
    }

    // ------------------------------------------------------------------ visits

    private ApiClient.Response ping(ApiClient as, String visitorId, String ref, String... paths) throws Exception {
        return as.post("/api/demo/visit/ping", Map.of("visitorId", visitorId, "ref", ref, "paths", List.of(paths)));
    }

    @Test
    void oneBrowserTwiceIsOneVisitorAndTwoVisits() throws Exception {
        long visitorsBefore = admin.get("/api/demo/visits/stats").json().get("visitors").asLong();
        String v = newVisitor();
        assertEquals("COUNTED", ping(guest, v, "linkedin", "/", "/attendance").json().get("result").asString());
        // within 30 s: not written again
        assertEquals("THROTTLED", ping(guest, v, "linkedin", "/timetable").json().get("result").asString());

        List<DemoVisit> rows = visits.findAll().stream().filter(x -> x.getVisitorId().equals(v)).toList();
        assertEquals(1, rows.size());
        DemoVisit first = rows.get(0);
        assertEquals(2, first.getPageCount());
        assertEquals("linkedin", first.getRef());
        assertEquals("DESKTOP", first.getDevice());
        assertEquals("Chrome", first.getBrowser());
        // the IP is only a salted SHA-256: 64 hex characters, never the address itself
        assertTrue(first.getIpHash().matches("[0-9a-f]{64}"), first.getIpHash());
        for (Field f : DemoVisit.class.getDeclaredFields()) {
            f.setAccessible(true);
            Object value = f.get(first);
            assertFalse(String.valueOf(value).contains("127.0.0.1") || String.valueOf(value).contains("0:0:0:0"),
                    f.getName() + " = " + value);
        }

        // 31 minutes of silence: the next page starts a second visit of the same visitor
        jdbc.update("update demo_visit set first_seen = first_seen - interval '31 minutes', last_seen = last_seen - interval '31 minutes' "
                + "where visitor_id = ?", v);
        assertEquals("COUNTED", ping(guest, v, "linkedin", "/calendar").json().get("result").asString());
        assertEquals(2, visits.findAll().stream().filter(x -> x.getVisitorId().equals(v)).count());

        var stats = admin.get("/api/demo/visits/stats").json();
        assertEquals(visitorsBefore + 1, stats.get("visitors").asLong());
        boolean linkedin = false;
        for (var s : stats.get("sources")) linkedin |= s.get("key").asString().equals("linkedin");
        assertTrue(linkedin, stats.toString());
    }

    @Test
    void anAdministratorsBrowserAndCrawlersAreNotCounted() throws Exception {
        String v = newVisitor();
        assertEquals("COUNTED", ping(guest, v, "telegram", "/").json().get("result").asString());
        // the administrator signs in from the same browser: its visits go, and it is never counted again
        new ApiClient(port).post("/api/auth/login", Map.of("username", adminUser.getUsername(), "password", PASSWORD, "visitorId", v));
        assertEquals(0, visits.findAll().stream().filter(x -> x.getVisitorId().equals(v)).count());
        jdbc.update("update demo_visit set last_seen = last_seen - interval '1 hour' where visitor_id = ?", v);
        assertEquals("IGNORED", ping(guest, v, "telegram", "/").json().get("result").asString());
        // the administrator's own pings are not visits either
        assertEquals("IGNORED", ping(admin, newVisitor(), "direct", "/").json().get("result").asString());
        // a crawler
        ApiClient bot = new ApiClient(port).header("User-Agent", "Mozilla/5.0 (compatible; Googlebot/2.1)")
                .withToken(new ApiClient(port).header("User-Agent", BROWSER_UA).post("/api/auth/guest-login", Map.of()).body().trim());
        assertEquals("IGNORED", ping(bot, newVisitor(), "direct", "/").json().get("result").asString());
    }
}
