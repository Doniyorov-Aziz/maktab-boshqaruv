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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.MeetingService;
import uz.azizbek.maktabboshqaruv.service.ModuleJobs;
import uz.azizbek.maktabboshqaruv.service.ReportCardService;
import uz.azizbek.maktabboshqaruv.service.parent.ParentModulesService;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.support.MutableClock;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The seven school modules end to end against the test database (never the live one): homework with a file and
 * the parents' message, the quarter tabel (suggestion → stored QUARTERLY grade → PDF), teacher workload, exams and
 * their results, parent–teacher meetings (book / taken / cancel), payments (months, debtors, VIEWER 403) and
 * achievements; and the new bot pages through the real router.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.bot-username=maktab_test_bot",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "telegram.chat-rate-per-second=1000"
})
@ActiveProfiles("local")
@Import(SchoolModulesIntegrationTest.FakeClock.class)
class SchoolModulesIntegrationTest {

    static final ZoneId TASHKENT = ZoneId.of("Asia/Tashkent");
    /** Wednesday — a school day. */
    static final LocalDate DAY = LocalDate.of(2026, 10, 7);
    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final String TAG = "mod_" + ThreadLocalRandom.current().nextInt(100_000, 999_999);
    private static final AtomicLong UPDATE_ID = new AtomicLong(500_000);

    @TestConfiguration
    static class FakeClock {
        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(DAY.atTime(10, 0), TASHKENT);
        }
    }

    @LocalServerPort
    private int port;
    @Autowired private MutableClock clock;
    @Autowired private LessonSlotRepository slots;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder encoder;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private ParentModulesService parentModules;
    @Autowired private MeetingService meetings;
    @Autowired private ModuleJobs jobs;
    @Autowired private BotRouter router;
    @Autowired private TelegramClient telegram;
    @Autowired private ParentTelegramLinkRepository links;
    @Autowired private ParentSessionRepository sessions;

    private LessonSlot slot;
    private SchoolClass cls;
    private Long schoolId;
    private List<Student> pupils;
    private Long tempTermId;
    private long chatId;
    private final List<User> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        clock.set(DAY.atTime(10, 0));
        slot = slots.findAll().stream()
                .filter(l -> l.getWeekday().equals("Chorshanba") && l.getSchoolClass().getAcademicYear().getSchool().getId() == 1L)
                .findFirst().orElseThrow();
        cls = slot.getSchoolClass();
        schoolId = 1L;
        pupils = students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(cls.getId());
        assertFalse(pupils.isEmpty(), "test class has pupils");
        chatId = 7_300_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from notification_log where chat_id = ? or reference_id in (select id from homework where created_by like 'mod_%')", chatId);
        jdbc.update("delete from homework where created_by like 'mod_%'");
        jdbc.update("delete from grade where created_by like 'mod_%' or comment like '%" + TAG + "%'");
        jdbc.update("delete from grade where exam_id in (select id from exam where created_by like 'mod_%')");
        jdbc.update("delete from exam where created_by like 'mod_%'");
        jdbc.update("delete from staff_notification where kind = 'MEETING' and employee_id = ?", slot.getEmployee().getId());
        jdbc.update("delete from meeting_slot where created_by like 'mod_%'");
        jdbc.update("delete from payment where created_by like 'mod_%'");
        jdbc.update("delete from student_fee where note = ?", TAG);
        jdbc.update("delete from payment_settings where updated_by like 'mod_%'");
        jdbc.update("delete from achievement where created_by like 'mod_%'");
        if (tempTermId != null) jdbc.update("delete from academic_term where id = ?", tempTermId);
        links.findAll().stream().filter(l -> l.getChatId() == chatId).forEach(links::delete);
        sessions.findByChatId(chatId).ifPresent(sessions::delete);
        users.deleteAll(createdUsers);
    }

    private ApiClient as(Role role, Employee e) throws Exception {
        User u = new User();
        u.setUsername(TAG + "_" + role + "_" + ThreadLocalRandom.current().nextInt(1_000_000));
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmployee(e);
        createdUsers.add(users.save(u));
        return new ApiClient(port).login(u.getUsername(), PASSWORD);
    }

    // ------------------------------------------------------------------ 1. homework

    @Test
    void homework_withFile_listedAndSentToParents() throws Exception {
        ApiClient teacher = as(Role.EDITOR, slot.getEmployee());
        ApiClient other = as(Role.EDITOR, null);
        ApiClient viewer = as(Role.VIEWER, null);
        Map<String, Object> body = new HashMap<>(Map.of("schoolClassId", cls.getId(), "subjectId", slot.getSubject().getId(),
                "dueDate", DAY.plusDays(2).toString(), "title", "45-mashq " + TAG, "description", "Darslikdan 45-mashq"));

        assertEquals(403, viewer.post("/api/homework", body).status());
        ApiClient.Response created = teacher.post("/api/homework", body);
        assertEquals(201, created.status(), created.body());
        long id = created.json().get("id").asLong();
        assertEquals(slot.getEmployee().getId(), created.json().get("employeeId").asLong(), "the teacher is filled in");
        assertEquals(DAY.toString(), created.json().get("assignedDate").asString());

        // due before given → 400
        Map<String, Object> bad = new HashMap<>(body);
        bad.put("assignedDate", DAY.toString());
        bad.put("dueDate", DAY.minusDays(1).toString());
        assertEquals(400, teacher.post("/api/homework", bad).status());

        byte[] file = ("Vazifa matni " + TAG).getBytes(StandardCharsets.UTF_8);
        ApiClient.Response exe = teacher.multipart("/api/homework/" + id + "/file",
                List.of(ApiClient.Part.file("file", "virus.exe", "application/octet-stream", file)));
        assertEquals(400, exe.status());
        ApiClient.Response att = teacher.multipart("/api/homework/" + id + "/file",
                List.of(ApiClient.Part.file("file", "vazifa.txt", "text/plain", file)));
        assertEquals(200, att.status(), att.body());
        assertEquals("vazifa.txt", att.json().get("fileName").asString());
        ApiClient.Response dl = teacher.get("/api/homework/" + id + "/file");
        assertEquals(200, dl.status());
        assertArrayEquals(file, dl.raw().body());

        JsonNode list = teacher.get("/api/homework?schoolId=1&schoolClassId=" + cls.getId()).json();
        assertTrue(list.get("content").toString().contains(TAG));

        assertEquals(200, teacher.post("/api/homework/" + id + "/notify", Map.of()).status());
        assertEquals(409, teacher.post("/api/homework/" + id + "/notify", Map.of()).status(), "only once");

        assertTrue(parentModules.homeworkUpcoming(pupils.get(0)).stream().anyMatch(h -> h.title().contains(TAG)));
        assertEquals(403, other.delete("/api/homework/" + id).status(), "only the teacher who gave it or an admin");
        assertEquals(204, teacher.delete("/api/homework/" + id).status());
    }

    // ------------------------------------------------------------------ 2. tabel

    @Test
    void tabel_suggestsStoresAndPrints() throws Exception {
        ensureQuarterAroundDay();
        ApiClient admin = as(Role.ADMIN, null);
        Student s = pupils.get(0);
        for (int score : new int[]{5, 4, 4}) {
            jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, comment, created_by, source) " +
                    "values (?, ?, ?, ?, 'CURRENT', ?, ?, 'MANUAL')", s.getId(), slot.getSubject().getId(), DAY.minusDays(1), score, TAG, TAG);
        }
        JsonNode card = admin.get("/api/report-cards?classId=" + cls.getId()).json();
        JsonNode cell = cellOf(card, s.getId(), slot.getSubject().getId());
        assertNotNull(cell, card.toString());
        // the test database has other grades too: the suggestion is the rounded average of all of them
        assertEquals(ReportCardService.suggest(cell.get("average").asDouble()), cell.get("suggested").asInt());
        int suggested = cell.get("suggested").asInt();
        assertTrue(cell.get("count").asInt() >= 3);
        long termId = card.get("term").get("id").asLong();

        JsonNode saved = admin.post("/api/report-cards/finalize", Map.of("classId", cls.getId(), "termId", termId)).json();
        assertTrue(saved.get("saved").asInt() >= 1);
        card = admin.get("/api/report-cards?classId=" + cls.getId() + "&termId=" + termId).json();
        assertEquals(suggested, cellOf(card, s.getId(), slot.getSubject().getId()).get("quarterly").asInt());

        Map<String, Object> five = new HashMap<>(Map.of("classId", cls.getId(), "termId", termId, "studentId", s.getId(),
                "subjectId", slot.getSubject().getId(), "score", 5));
        card = admin.put("/api/report-cards/cell", five).json();
        assertEquals(5, cellOf(card, s.getId(), slot.getSubject().getId()).get("quarterly").asInt());
        five.put("score", 7);
        assertEquals(400, admin.put("/api/report-cards/cell", five).status());

        ApiClient.Response pdf = admin.get("/api/report-cards/pdf?classId=" + cls.getId() + "&studentId=" + s.getId());
        assertEquals(200, pdf.status());
        assertEquals("%PDF", new String(pdf.raw().body(), 0, 4, StandardCharsets.US_ASCII));

        ReportCardService.StudentYear y = parentModules.tabel(s);
        assertTrue(y.subjects().stream().anyMatch(x -> x.quarters().contains(5)), "the bot sees the stored grade");
        jdbc.update("update grade set created_by = ? where type = 'QUARTERLY' and student_id = ? and grade_date between ? and ?",
                TAG, s.getId(), DAY.minusDays(60), DAY.plusDays(60));
    }

    // ------------------------------------------------------------------ 3. workload

    @Test
    void workload_countsTheTimetableOnSchoolDays() throws Exception {
        ApiClient admin = as(Role.ADMIN, null);
        LocalDate monday = DAY.with(DayOfWeek.MONDAY);
        JsonNode r = admin.get("/api/workload?schoolId=1&from=" + monday + "&to=" + monday.plusDays(5)).json();
        JsonNode row = null;
        for (JsonNode x : r.get("rows")) if (x.get("employeeId").asLong() == slot.getEmployee().getId()) row = x;
        assertNotNull(row, r.toString());
        assertTrue(row.get("weeklyLessons").asInt() >= 1);
        assertTrue(row.get("planned").asInt() >= 1);
        assertEquals(400, admin.get("/api/workload?schoolId=1&from=" + DAY + "&to=" + DAY.minusDays(1)).status());
    }

    // ------------------------------------------------------------------ 4. exams

    @Test
    void exams_resultsOnlyFromTheDay_storedAsExamGrades() throws Exception {
        ApiClient teacher = as(Role.EDITOR, slot.getEmployee());
        Map<String, Object> body = new HashMap<>(Map.of("schoolClassId", cls.getId(), "subjectId", slot.getSubject().getId(),
                "kind", "CONTROL", "title", "1-nazorat " + TAG, "examDate", DAY.plusDays(1).toString(), "startTime", "09:00"));
        ApiClient.Response tomorrow = teacher.post("/api/exams", body);
        assertEquals(201, tomorrow.status(), tomorrow.body());
        long tomorrowId = tomorrow.json().get("id").asLong();
        Student s = pupils.get(0);
        assertEquals(409, teacher.put("/api/exams/" + tomorrowId + "/results",
                List.of(Map.of("studentId", s.getId(), "score", 5))).status(), "not before the exam");

        body.put("examDate", DAY.with(DayOfWeek.SUNDAY).toString());
        assertEquals(400, teacher.post("/api/exams", body).status(), "never on a Sunday");

        body.put("examDate", DAY.toString());
        long todayId = teacher.post("/api/exams", body).json().get("id").asLong();
        JsonNode res = teacher.put("/api/exams/" + todayId + "/results", List.of(Map.of("studentId", s.getId(), "score", 4))).json();
        JsonNode mine = null;
        for (JsonNode r : res.get("rows")) if (r.get("studentId").asLong() == s.getId()) mine = r;
        assertEquals(4, mine.get("score").asInt());
        Integer stored = jdbc.queryForObject("select score from grade where exam_id = ? and student_id = ? and type = 'EXAM'",
                Integer.class, todayId, s.getId());
        assertEquals(4, stored);
        assertEquals(1L, res.get("exam").get("resultCount").asLong());

        // the bot: tomorrow's exam ahead, today's with the result
        List<ParentModulesService.ExamItem> items = parentModules.exams(s);
        assertTrue(items.stream().anyMatch(x -> x.id() == tomorrowId));
        jobs.runExamReminders(DAY.atTime(18, 0), true);
    }

    // ------------------------------------------------------------------ 5. meetings

    @Test
    void meetings_bookOnce_takenForOthers_cancelFrees() throws Exception {
        ApiClient teacher = as(Role.EDITOR, slot.getEmployee());
        ApiClient.Response bulk = teacher.post("/api/meeting-slots/bulk", Map.of("employeeId", slot.getEmployee().getId(),
                "date", DAY.plusDays(1).toString(), "from", "15:00", "to", "16:00", "minutes", 30, "place", "204-xona"));
        assertEquals(201, bulk.status(), bulk.body());
        assertEquals(2, bulk.json().size(), "15:00 and 15:30");
        long first = bulk.json().get(0).get("id").asLong();
        // the same range again: nothing new
        assertEquals(409, teacher.post("/api/meeting-slots/bulk", Map.of("employeeId", slot.getEmployee().getId(),
                "date", DAY.plusDays(1).toString(), "from", "15:00", "to", "16:00", "minutes", 30)).status());

        Student s = pupils.get(0);
        assertTrue(parentModules.meetingTeachers(s).stream().anyMatch(t -> t.employeeId().equals(slot.getEmployee().getId())));
        assertEquals(MeetingService.BookResult.OK, meetings.book(chatId, s, first, "Malika", null));
        assertEquals(MeetingService.BookResult.TAKEN, meetings.book(chatId + 1, s, first, "Boshqa", null));
        long second = bulk.json().get(1).get("id").asLong();
        assertEquals(MeetingService.BookResult.ALREADY_WITH_TEACHER, meetings.book(chatId, s, second, "Malika", null));

        JsonNode list = teacher.get("/api/meeting-slots?schoolId=1&employeeId=" + slot.getEmployee().getId()
                + "&from=" + DAY + "&to=" + DAY.plusDays(2)).json();
        boolean booked = false;
        for (JsonNode m : list) if (m.get("id").asLong() == first) booked = m.get("booked").asBoolean();
        assertTrue(booked);
        Integer bells = jdbc.queryForObject("select count(*) from staff_notification where kind = 'MEETING' and employee_id = ?",
                Integer.class, slot.getEmployee().getId());
        assertTrue(bells >= 1, "the teacher's bell");

        JsonNode cancelled = teacher.post("/api/meeting-slots/" + first + "/cancel", Map.of()).json();
        assertFalse(cancelled.get("booked").asBoolean());
        ApiClient otherTeacher = as(Role.EDITOR, null);
        assertEquals(403, otherTeacher.delete("/api/meeting-slots/" + second).status());
    }

    // ------------------------------------------------------------------ 6. payments

    @Test
    void payments_monthsDebtorsAndRoles() throws Exception {
        ApiClient admin = as(Role.ADMIN, null);
        ApiClient viewer = as(Role.VIEWER, null);
        Student s = pupils.get(0);
        assertEquals(200, admin.put("/api/payments/settings?schoolId=1", Map.of("enabled", true, "monthlyFee", 100_000,
                "dueDay", 5, "payInfo", "Karta 8600 0000 0000 0000")).status());
        ApiClient.Response pay = admin.post("/api/payments", Map.of("studentId", s.getId(), "month", "2026-09", "amount", 40_000,
                "method", "CARD", "note", "qisman"));
        assertEquals(201, pay.status(), pay.body());

        JsonNode acc = admin.get("/api/payments/student/" + s.getId()).json();
        JsonNode sep = null;
        for (JsonNode m : acc.get("months")) if (m.get("month").asString().equals("2026-09")) sep = m;
        if (sep != null) {
            assertEquals("PARTIAL", sep.get("status").asString());
            assertTrue(acc.get("debt").asLong() >= 60_000, acc.toString());
        }
        assertEquals(403, viewer.get("/api/payments/summary?schoolId=1").status(), "viewers never see fees");
        assertEquals(403, viewer.put("/api/payments/settings?schoolId=1", Map.of("enabled", false, "monthlyFee", 0, "dueDay", 5)).status());

        // a pupil's own fee: exempt → no debt
        assertEquals(200, admin.put("/api/payments/fee", Map.of("studentId", s.getId(), "monthlyFee", 0, "note", TAG)).status());
        JsonNode exempt = admin.get("/api/payments/student/" + s.getId()).json();
        assertEquals(0, exempt.get("debt").asLong());
        assertTrue(parentModules.paymentsEnabled(s));
    }

    // ------------------------------------------------------------------ 7. achievements

    @Test
    void achievements_listStatsAndStudent() throws Exception {
        ApiClient editor = as(Role.EDITOR, null);
        Student s = pupils.get(0);
        ApiClient.Response a = editor.post("/api/achievements", Map.of("studentId", s.getId(), "title", "Matematika olimpiadasi " + TAG,
                "category", "OLYMPIAD", "level", "DISTRICT", "place", 2, "achievedOn", DAY.minusDays(3).toString(),
                "subjectId", slot.getSubject().getId()));
        assertEquals(201, a.status(), a.body());
        assertEquals(400, editor.post("/api/achievements", Map.of("studentId", s.getId(), "title", "x", "category", "SPORT",
                "level", "SCHOOL", "place", 4, "achievedOn", DAY.toString())).status(), "place 1..3");
        assertTrue(editor.get("/api/achievements/student/" + s.getId()).body().contains(TAG));
        JsonNode stats = editor.get("/api/achievements/stats?schoolId=1").json();
        assertTrue(stats.get("prizes").asLong() >= 1);
        assertTrue(parentModules.achievements(s).stream().anyMatch(x -> x.title().contains(TAG)));
    }

    // ------------------------------------------------------------------ bot pages

    @Test
    void bot_newPagesRender() throws Exception {
        Student s = students.findAll().stream()
                .filter(x -> x.getTelegramLinkCode() != null && x.getSchoolClass().getAcademicYear().getSchool().getId() == 1L)
                .findFirst().orElseThrow();
        ApiClient teacher = as(Role.EDITOR, slot.getEmployee());
        ApiClient.Response hw = teacher.post("/api/homework", Map.of("schoolClassId", s.getSchoolClass().getId(),
                "subjectId", slot.getSubject().getId(), "dueDate", DAY.plusDays(1).toString(), "title", "Bot vazifasi " + TAG));
        assertEquals(201, hw.status(), hw.body());
        MockTelegramClient mock = (MockTelegramClient) telegram;
        send("/start " + s.getTelegramLinkCode());
        assertTrue(tap("home", mock).contains("Uy vazifasi"), "home has the new button row");
        assertTrue(tap("hw", mock).contains(TAG));
        String more = tap("more", mock);
        assertTrue(more.contains("Yana"), more);
        for (String screen : List.of("exm", "mt", "tab", "ach", "pay")) {
            String text = tap(screen, mock);
            assertFalse(text.isBlank(), screen);
            assertFalse(text.contains("xato"), screen + ": " + text);
        }
    }

    private void send(String text) {
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), new TelegramModels.Message(1L,
                new TelegramModels.User(chatId, false, "Malika", "malika"), new TelegramModels.Chat(chatId, "private"), text, null)));
    }

    private String tap(String data, MockTelegramClient mock) {
        int before = mock.size();
        TelegramModels.User from = new TelegramModels.User(chatId, false, "Malika", "malika");
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), null, new TelegramModels.CallbackQuery("cb", from,
                new TelegramModels.Message(null, from, new TelegramModels.Chat(chatId, "private"), null, null), data)));
        StringBuilder sb = new StringBuilder();
        for (MockTelegramClient.SentMessage m : mock.since(before, chatId)) {
            if (m.text() != null) sb.append(m.text()).append("\n");
            if (m.keyboard() != null) sb.append(m.keyboard()).append("\n");
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ helpers

    /** A quarter containing DAY in the class's year: an existing one, else a temporary one. */
    private void ensureQuarterAroundDay() {
        Long yearId = cls.getAcademicYear().getId();
        List<Long> found = jdbc.queryForList("select id from academic_term where academic_year_id = ? and kind = 'QUARTER' " +
                "and start_date <= ? and end_date >= ?", Long.class, yearId, DAY, DAY);
        if (!found.isEmpty()) return;
        Integer overlapping = jdbc.queryForObject("select count(*) from academic_term where academic_year_id = ? and kind = 'QUARTER'",
                Integer.class, yearId);
        LocalDate from = DAY.minusDays(20);
        LocalDate to = DAY.plusDays(20);
        if (overlapping != null && overlapping > 0) {
            // a year with quarters that miss DAY: keep the test independent of them
            from = DAY.minusDays(2);
            to = DAY.plusDays(2);
        }
        tempTermId = jdbc.queryForObject("insert into academic_term (academic_year_id, name, kind, start_date, end_date) " +
                "values (?, ?, 'QUARTER', ?, ?) returning id", Long.class, yearId, "Test chorak " + TAG, from, to);
    }

    private static JsonNode cellOf(JsonNode card, long studentId, long subjectId) {
        List<Long> subjectIds = new ArrayList<>();
        for (JsonNode sub : card.get("subjects")) subjectIds.add(sub.get("id").asLong());
        for (JsonNode row : card.get("rows")) {
            if (row.get("studentId").asLong() != studentId) continue;
            for (JsonNode c : row.get("cells")) if (c.get("subjectId").asLong() == subjectId) return c;
        }
        return null;
    }
}
