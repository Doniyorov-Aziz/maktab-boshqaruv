package uz.azizbek.maktabboshqaruv;

import tools.jackson.databind.JsonNode;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationSender;
import uz.azizbek.maktabboshqaruv.service.appeal.AppealFiles;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 9 — "✉️ Ma'muriyatga xat" end to end with Telegram mocked: a parent sends text, a photo,
 * a video, a voice note, a document and an album; "✅ Yuborish" creates one appeal; the files
 * are copied to our storage; a reply from the platform goes through the outbox to the
 * parent's chat; wrong file types and >20 MB files are refused; another school's user and a
 * stranger's link get 403; and the Telegram file URL (which holds the token) appears nowhere.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "telegram.chat-rate-per-second=1000",
        "app.storage.path=build/test-storage"
})
@ActiveProfiles("local")
class AppealFlowIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final AtomicLong UPDATE_ID = new AtomicLong(500_000);

    @LocalServerPort
    private int port;
    @Autowired
    private BotRouter router;
    @Autowired
    private TelegramClient client;
    @Autowired
    private StudentRepository students;
    @Autowired
    private ParentTelegramLinkRepository links;
    @Autowired
    private ParentSessionRepository sessions;
    @Autowired
    private AppealRepository appeals;
    @Autowired
    private AppealMessageRepository appealMessages;
    @Autowired
    private NotificationLogRepository outbox;
    @Autowired
    private NotificationSender sender;
    @Autowired
    private AppealFiles files;
    @Autowired
    private UserRepository users;
    @Autowired
    private EmployeeRepository employees;
    @Autowired
    private SchoolRepository schools;
    @Autowired
    private PositionRepository positions;
    @Autowired
    private PasswordEncoder encoder;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private uz.azizbek.maktabboshqaruv.telegram.TelegramProperties telegramProperties;

    private MockTelegramClient mock;
    private long chatId;
    private Student student;
    private User admin;
    private User otherSchoolUser;
    private Employee otherSchoolEmployee;
    private long messageId = 1;

    @BeforeEach
    void setUp() {
        mock = (MockTelegramClient) client;
        chatId = 7_100_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        student = students.findAll().get(0);
        ParentTelegramLink link = new ParentTelegramLink();
        link.setStudent(student);
        link.setChatId(chatId);
        link.setFirstName("Furqat");
        link.setActive(true);
        link.setLinkedAt(LocalDateTime.now());
        links.save(link);

        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        admin = new User();
        admin.setUsername("murojaat_admin_" + n);
        admin.setPassword(encoder.encode(PASSWORD));
        admin.setRole(Role.ADMIN);
        admin = users.save(admin);

        // an editor of the OTHER school (linked to that school's employee)
        Long ownSchool = student.getSchoolClass().getAcademicYear().getSchool().getId();
        School other = schools.findAll().stream().filter(s -> !s.getId().equals(ownSchool)).findFirst().orElseThrow();
        otherSchoolEmployee = new Employee();
        otherSchoolEmployee.setSchool(other);
        otherSchoolEmployee.setPosition(positions.findAll().get(0));
        otherSchoolEmployee.setFirstName("Boshqa");
        otherSchoolEmployee.setLastName("Maktab");
        otherSchoolEmployee.setPhone("+9989" + (10_000_000 + n));
        otherSchoolEmployee = employees.save(otherSchoolEmployee);
        otherSchoolUser = new User();
        otherSchoolUser.setUsername("boshqa_maktab_" + n);
        otherSchoolUser.setPassword(encoder.encode(PASSWORD));
        otherSchoolUser.setRole(Role.ADMIN);
        otherSchoolUser.setEmployee(otherSchoolEmployee);
        otherSchoolUser = users.save(otherSchoolUser);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from notification_log where chat_id = ?", chatId);
        jdbc.update("delete from appeal_message where appeal_id in (select id from appeal where chat_id = ?)", chatId);
        jdbc.update("delete from appeal where chat_id = ?", chatId);
        links.findByChatIdAndActiveTrue(chatId).forEach(links::delete);
        sessions.findByChatId(chatId).ifPresent(sessions::delete);
        users.delete(admin);
        users.delete(otherSchoolUser);
        employees.delete(otherSchoolEmployee);
    }

    // ------------------------------------------------------------------ bot helpers

    private TelegramModels.User from() {
        return new TelegramModels.User(chatId, false, "Furqat", "furqat_ota", "uz");
    }

    private TelegramModels.Chat chat() {
        return new TelegramModels.Chat(chatId, "private");
    }

    private List<MockTelegramClient.SentMessage> tap(String data) {
        int before = mock.size();
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), null, new TelegramModels.CallbackQuery("cb",
                from(), new TelegramModels.Message(99L, from(), chat(), null, null), data)));
        return mock.since(before, chatId);
    }

    private List<MockTelegramClient.SentMessage> send(TelegramModels.Message m) {
        int before = mock.size();
        router.handle(new TelegramModels.Update(UPDATE_ID.getAndIncrement(), m));
        return mock.since(before, chatId);
    }

    private TelegramModels.Message message(String text, List<TelegramModels.PhotoSize> photo, String caption,
                                           TelegramModels.Video video, TelegramModels.Voice voice,
                                           TelegramModels.Document doc, String group) {
        return new TelegramModels.Message(messageId++, from(), chat(), text, null, photo, caption, video, voice, null,
                doc, null, group);
    }

    private String upload(String content) {
        return mock.storeIncomingFile(content.getBytes(StandardCharsets.UTF_8));
    }

    private static String lastText(List<MockTelegramClient.SentMessage> ops) {
        return ops.isEmpty() ? "" : ops.get(ops.size() - 1).text();
    }

    // ---------------------------------------------------------------------- tests

    @Test
    void everyKindOfMessage_isAccepted_storedAndAnswered() throws Exception {
        tap("msg:a:to:to:CT");
        assertTrue(lastText(send(message("Assalomu alaykum, farzandim haqida savol", null, null, null, null, null, null)))
                .contains("Qabul qilindi"));
        send(message(null, List.of(new TelegramModels.PhotoSize(upload("rasm-1"), 800, 600, "u1", 6L)), "Daftar rasmi",
                null, null, null, null));
        send(message(null, null, null, new TelegramModels.Video(upload("video"), "u2", 12, "video/mp4", 5L, "dars.mp4"),
                null, null, null));
        send(message(null, null, null, null, new TelegramModels.Voice(upload("ovoz"), "u3", 7, "audio/ogg", 4L), null, null));
        send(message(null, null, null, null, null,
                new TelegramModels.Document(upload("%PDF-1.4 ma'lumotnoma"), "u4", "malumotnoma.pdf", "application/pdf", 20L), null));
        // an album: three photos with one media_group_id → one ✓
        List<MockTelegramClient.SentMessage> albumAcks = new java.util.ArrayList<>();
        for (int i = 0; i < 3; i++) {
            albumAcks.addAll(send(message(null, List.of(new TelegramModels.PhotoSize(upload("albom-" + i), 640, 480, "a" + i, 7L)),
                    null, null, null, null, "album-42")));
        }
        assertEquals(1, albumAcks.size(), "one ✓ for the whole album");

        // nothing is visible to the school before "✅ Yuborish"
        Appeal draft = appeals.findFirstByChatIdAndStatusOrderByIdDesc(chatId, AppealEnums.Status.DRAFT).orElseThrow();
        assertTrue(appealMessages.visibleOf(draft.getId()).isEmpty());

        String sent = lastText(tap("msg:a:ok"));
        assertTrue(sent.contains("#" + draft.getId()), sent);
        Appeal appeal = appeals.findById(draft.getId()).orElseThrow();
        assertEquals(AppealEnums.Status.NEW, appeal.getStatus());
        assertEquals(AppealEnums.Target.CLASS_TEACHER, appeal.getTarget());
        List<AppealMessage> list = appealMessages.visibleOf(appeal.getId());
        assertEquals(8, list.size());
        assertEquals(8, appeal.getUnreadCount());

        // files land in our storage (the download runs on its own pool; run it now)
        for (AppealMessage m : list) if (m.hasFile()) files.fetch(m.getId());
        for (AppealMessage m : appealMessages.visibleOf(appeal.getId())) {
            if (!m.hasFile()) continue;
            assertEquals(AppealEnums.FileState.STORED, m.getFileState(), m.getKind().name());
            assertTrue(Files.exists(Path.of("build/test-storage").resolve(m.getStoragePath())), m.getStoragePath());
        }

        // the school opens it (as admin) and answers; the reply goes through the outbox to this chat
        ApiClient api = new ApiClient(port).login(admin.getUsername(), PASSWORD);
        JsonNode chatJson = api.get("/api/appeals/" + appeal.getId()).json();
        assertEquals(8, chatJson.get("messages").size());
        String fileUrl = null;
        for (JsonNode m : chatJson.get("messages")) if ("VOICE".equals(m.get("kind").asString())) fileUrl = m.get("url").asString();
        assertNotNull(fileUrl);

        // the signed link works without a JWT, with a Range request (audio/video seeking)
        HttpResponse<byte[]> voice = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + fileUrl))
                .header("Range", "bytes=0-1").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(206, voice.statusCode());
        assertEquals("audio/ogg", voice.headers().firstValue("Content-Type").orElse("").split(";")[0]);
        assertEquals(2, voice.body().length);

        ApiClient.Response reply = api.send("POST", "/api/appeals/" + appeal.getId() + "/reply", null);
        assertEquals(415, reply.status(), "replies are multipart");
        String boundary = "----t" + System.nanoTime();
        String body = "--" + boundary + "\r\nContent-Disposition: form-data; name=\"text\"\r\n\r\n"
                + "Rahmat, ertaga uchrashamiz.\r\n--" + boundary + "--\r\n";
        HttpResponse<String> replied = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + "/api/appeals/" + appeal.getId() + "/reply"))
                .header("Authorization", "Bearer " + login(admin.getUsername()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, replied.statusCode(), replied.body());
        assertEquals(AppealEnums.Status.ANSWERED, appeals.findById(appeal.getId()).orElseThrow().getStatus());

        List<NotificationLog> queued = outbox.findAll().stream().filter(n -> n.getChatId().equals(chatId)).toList();
        assertEquals(1, queued.size());
        assertTrue(queued.get(0).getText().contains("Maktabdan javob (#" + appeal.getId() + ")"));
        int before = mock.size();
        Long rowId = queued.get(0).getId();
        // other due rows in the shared test database may come first: pass until ours is done
        for (int i = 0; i < 200 && outbox.findById(rowId).orElseThrow().getStatus() == NotificationStatus.PENDING; i++) {
            sender.sendDue();
        }
        List<MockTelegramClient.SentMessage> delivered = mock.since(before, chatId);
        assertTrue(delivered.stream().anyMatch(o -> o.text() != null && o.text().contains("Rahmat, ertaga uchrashamiz")),
                () -> "the reply reached the parent's chat: " + delivered + " / row status="
                        + outbox.findById(queued.get(0).getId()).map(n -> n.getStatus() + " at " + n.getScheduledAt() + " err=" + n.getLastError()).orElse("-"));
        assertTrue(delivered.get(delivered.size() - 1).keyboard().contains("msg:a:re:ap:" + appeal.getId()),
                "↩️ Javob yozish continues the same appeal");

        // the parent answers back into the same appeal; it is NEW again
        tap("msg:a:re:ap:" + appeal.getId());
        send(message("Yaxshi, kelaman", null, null, null, null, null, null));
        tap("msg:a:ok");
        assertEquals(AppealEnums.Status.NEW, appeals.findById(appeal.getId()).orElseThrow().getStatus());
        assertEquals(10, appealMessages.visibleOf(appeal.getId()).size());

        // the Telegram file URL (it contains the bot token) is nowhere: database, API answers
        Integer leaks = jdbc.queryForObject("select count(*) from appeal_message where file_id like '%api.telegram.org%' " +
                "or storage_path like '%api.telegram.org%' or text like '%api.telegram.org/file/bot%'", Integer.class);
        assertEquals(0, leaks);
        assertFalse(api.get("/api/appeals/" + appeal.getId()).body().contains("api.telegram.org/file/bot"));
        Long schoolId = appeal.getSchool().getId();
        ApiClient.Response page = api.get("/api/appeals?schoolId=" + schoolId);
        assertEquals(200, page.status(), page.body());
        assertFalse(page.body().contains("api.telegram.org"));
        // filters: #id, parent name, dates, status, target
        assertEquals(1, api.get("/api/appeals?schoolId=" + schoolId + "&q=%23" + appeal.getId()).json().get("totalElements").asInt());
        assertTrue(api.get("/api/appeals?schoolId=" + schoolId + "&q=furqat").json().get("totalElements").asInt() >= 1);
        String today = java.time.LocalDate.now().toString();
        assertEquals(200, api.get("/api/appeals?schoolId=" + schoolId + "&from=" + today + "&to=" + today
                + "&status=NEW&target=CLASS_TEACHER&classId=" + student.getSchoolClass().getId()).status());
        assertEquals(0, api.get("/api/appeals?schoolId=" + schoolId + "&q=%23" + appeal.getId() + "&from=2001-01-01&to=2001-01-02")
                .json().get("totalElements").asInt());
    }

    @Test
    void anAlbumBurst_isNotDroppedByTheChatRateLimit() {
        int limit = telegramProperties.getChatRatePerSecond();
        telegramProperties.setChatRatePerSecond(2);
        try {
            tap("msg:a:to:to:AD");
            // Telegram delivers a 6-photo album as 6 updates within milliseconds
            for (int i = 0; i < 6; i++) {
                send(message(null, List.of(new TelegramModels.PhotoSize(upload("burst-" + i), 640, 480, "b" + i, 6L)),
                        null, null, null, null, "burst-album"));
            }
            Appeal draft = appeals.findFirstByChatIdAndStatusOrderByIdDesc(chatId, AppealEnums.Status.DRAFT).orElseThrow();
            assertEquals(6, appealMessages.pendingOf(draft.getId()).size(), "every photo of the album is kept");
        } finally {
            telegramProperties.setChatRatePerSecond(limit);
        }
    }

    @Test
    void wrongTypesAndHugeFiles_areRefused_politely() {
        tap("msg:a:to:to:AD");
        String exe = lastText(send(message(null, null, null, null, null,
                new TelegramModels.Document(upload("MZ"), "u9", "dastur.exe", "application/x-msdownload", 10L), null)));
        assertTrue(exe.contains("qabul qila olmaymiz"), exe);
        String svg = lastText(send(message(null, null, null, null, null,
                new TelegramModels.Document(upload("<svg/>"), "u10", "rasm.svg", "image/svg+xml", 10L), null)));
        assertTrue(svg.contains("qabul qila olmaymiz"), svg);
        String renamed = lastText(send(message(null, null, null, null, null,
                new TelegramModels.Document(upload("<script>"), "u11", "hujjat.pdf", "text/html", 10L), null)));
        assertTrue(renamed.contains("qabul qila olmaymiz"), "a script renamed to .pdf: " + renamed);
        String huge = lastText(send(message(null, null, null,
                new TelegramModels.Video(upload("x"), "u12", 60, "video/mp4", 21L * 1024 * 1024, "katta.mp4"), null, null, null)));
        assertTrue(huge.contains("20 MB"), huge);
        Appeal draft = appeals.findFirstByChatIdAndStatusOrderByIdDesc(chatId, AppealEnums.Status.DRAFT).orElseThrow();
        assertEquals(0, appealMessages.pendingOf(draft.getId()).size(), "nothing refused was kept");
    }

    @Test
    void anotherSchool_andStrangersLink_get403() throws Exception {
        tap("msg:a:to:to:AD");
        send(message(null, null, null, null, null,
                new TelegramModels.Document(upload("%PDF-1.4"), "u20", "a.pdf", "application/pdf", 8L), null));
        Long appealId = appeals.findFirstByChatIdAndStatusOrderByIdDesc(chatId, AppealEnums.Status.DRAFT).orElseThrow().getId();
        tap("msg:a:ok");
        AppealMessage file = appealMessages.visibleOf(appealId).get(0);
        files.fetch(file.getId());
        Long ownSchool = student.getSchoolClass().getAcademicYear().getSchool().getId();

        ApiClient other = new ApiClient(port).login(otherSchoolUser.getUsername(), PASSWORD);
        assertEquals(403, other.get("/api/appeals?schoolId=" + ownSchool).status(), "another school's appeal list");
        assertEquals(403, other.get("/api/appeals/" + appealId).status(), "another school's appeal");
        assertEquals(403, other.get("/api/appeals/" + appealId + "/attachments/" + file.getId()).status(), "another school's file");
        assertEquals(403, other.get("/api/students?schoolId=" + ownSchool).status(), "another school's students");
        assertEquals(200, other.get("/api/students?schoolId=" + otherSchoolEmployee.getSchool().getId()).status());

        // a link signed for one file does not open another file, and a tampered link is refused
        ApiClient api = new ApiClient(port).login(admin.getUsername(), PASSWORD);
        String url = api.get("/api/appeals/" + appealId).json().get("messages").get(0).get("url").asString();
        String token = url.substring(url.indexOf("?t=") + 3);
        HttpClient http = HttpClient.newHttpClient();
        assertEquals(200, http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + url)).GET().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode());
        assertEquals(403, http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/appeals/" + appealId
                + "/attachments/" + (file.getId() + 1) + "?t=" + token)).GET().build(), HttpResponse.BodyHandlers.discarding()).statusCode());
        assertEquals(403, http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + url + "x")).GET().build(),
                HttpResponse.BodyHandlers.discarding()).statusCode());
    }

    private String login(String username) throws Exception {
        HttpResponse<String> r = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .build(), HttpResponse.BodyHandlers.ofString());
        return r.body().trim();
    }
}
