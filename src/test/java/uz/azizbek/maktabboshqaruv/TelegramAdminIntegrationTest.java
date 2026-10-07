package uz.azizbek.maktabboshqaruv;

import tools.jackson.databind.JsonNode;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationSender;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.support.ApiClient.Part;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 8 — Telegram admin: parents by class (masked chat id for ADMIN only, invite links for
 * EDITOR+), a broadcast with an album and a PDF to chosen parents (each file uploaded once,
 * then sent by file_id), its recipients with filters, limits, the statistics, and 403 for
 * another school.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.bot-username=maktab_test_bot",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "telegram.chat-rate-per-second=1000",
        "app.storage.path=build/test-storage"
})
@ActiveProfiles("local")
class TelegramAdminIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);

    @LocalServerPort
    private int port;
    @Autowired
    private TelegramClient client;
    @Autowired
    private StudentRepository students;
    @Autowired
    private ParentTelegramLinkRepository links;
    @Autowired
    private NotificationLogRepository outbox;
    @Autowired
    private NotificationSender sender;
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

    private MockTelegramClient mock;
    private Student student;
    private Long schoolId;
    private long chatA;
    private long chatB;
    private final List<ParentTelegramLink> created = new ArrayList<>();
    private final List<User> createdUsers = new ArrayList<>();
    private Employee otherEmployee;

    @BeforeEach
    void setUp() {
        mock = (MockTelegramClient) client;
        student = students.findAll().get(0);
        schoolId = student.getSchoolClass().getAcademicYear().getSchool().getId();
        chatA = 7_200_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        chatB = chatA + 1;
        created.add(link(student, chatA, "Furqat"));
        created.add(link(student, chatB, "Nodira"));
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from notification_log where chat_id in (?, ?)", chatA, chatB);
        jdbc.update("delete from broadcast_attachment where broadcast_id in (select id from broadcast where created_by like 'tg_admin_%')");
        jdbc.update("delete from broadcast where created_by like 'tg_admin_%'");
        links.deleteAll(created);
        users.deleteAll(createdUsers);
        if (otherEmployee != null) employees.delete(otherEmployee);
    }

    private ParentTelegramLink link(Student s, long chatId, String name) {
        ParentTelegramLink l = new ParentTelegramLink();
        l.setStudent(s);
        l.setChatId(chatId);
        l.setFirstName(name);
        l.setActive(true);
        l.setLinkedAt(LocalDateTime.now());
        return links.save(l);
    }

    private ApiClient as(Role role, Employee employee) throws Exception {
        User u = new User();
        u.setUsername("tg_admin_" + role + "_" + ThreadLocalRandom.current().nextInt(1_000_000));
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmployee(employee);
        createdUsers.add(users.save(u));
        return new ApiClient(port).login(u.getUsername(), PASSWORD);
    }

    @Test
    void parentsByClass_masksChatIds_andHidesInviteLinksFromViewers() throws Exception {
        Long classId = student.getSchoolClass().getId();
        ApiClient admin = as(Role.ADMIN, null);
        JsonNode all = admin.get("/api/telegram/parents?classId=" + classId).json();
        assertTrue(all.get("total").asInt() >= 1);
        JsonNode row = null;
        for (JsonNode r : all.get("rows")) if (r.get("studentId").asLong() == student.getId()) row = r;
        assertNotNull(row);
        assertTrue(row.get("linked").asBoolean());
        String body = all.toString();
        assertFalse(body.contains(String.valueOf(chatA)), "the full chat id never leaves the server");
        boolean masked = false;
        for (JsonNode p : row.get("parents")) masked |= p.get("chatId").asString().matches("\\d{2}\\*+\\d{2}");
        assertTrue(masked, row.toString());

        // filters
        for (JsonNode r : admin.get("/api/telegram/parents?classId=" + classId + "&status=LINKED").json().get("rows")) {
            assertTrue(r.get("linked").asBoolean());
        }
        JsonNode notLinked = admin.get("/api/telegram/parents?classId=" + classId + "&status=NOT_LINKED").json().get("rows");
        for (JsonNode r : notLinked) {
            assertFalse(r.get("linked").asBoolean());
            assertTrue(r.get("inviteLink").asString().startsWith("https://t.me/maktab_test_bot?start="), r.toString());
        }

        // a viewer sees statuses, but neither chat ids nor invite links
        JsonNode viewer = as(Role.VIEWER, null).get("/api/telegram/parents?classId=" + classId).json();
        for (JsonNode r : viewer.get("rows")) {
            assertTrue(r.get("inviteLink").isNull());
            for (JsonNode p : r.get("parents")) assertTrue(p.get("chatId").isNull());
        }
    }

    @Test
    void broadcastWithAlbumAndPdf_toChosenParents_uploadsEachFileOnce() throws Exception {
        ApiClient admin = as(Role.ADMIN, null);
        List<Long> linkIds = created.stream().map(ParentTelegramLink::getId).toList();
        Map<String, Object> data = Map.of("schoolId", schoolId, "audience", "PARENTS", "linkIds", linkIds,
                "text", "Ertaga ochiq dars. Rasmlar ilova qilindi.");

        // preview: two chosen parents, text rendered as in Telegram
        JsonNode preview = admin.post("/api/broadcasts/preview", data).json();
        assertEquals(2, preview.get("recipientCount").asInt());
        assertTrue(preview.get("html").asString().contains("Ertaga ochiq dars"));

        byte[] png = Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");
        List<Part> parts = new ArrayList<>(List.of(Part.json("data", data)));
        for (int i = 0; i < 3; i++) parts.add(Part.file("files", "rasm" + i + ".png", "image/png", png));
        parts.add(Part.file("files", "reja.pdf", "application/pdf", "%PDF-1.4 reja".getBytes()));
        ApiClient.Response sent = admin.multipart("/api/broadcasts", parts);
        assertEquals(201, sent.status(), sent.body());
        long id = sent.json().get("id").asLong();
        assertEquals(4, sent.json().get("attachments").size());
        assertEquals(2, sent.json().get("recipientCount").asInt());
        assertEquals(2, sent.json().get("pending").asInt(), "queued in the outbox, not sent inline");

        int before = mock.size();
        for (int i = 0; i < 200 && outbox.findAll().stream().anyMatch(n -> (n.getChatId() == chatA || n.getChatId() == chatB)
                && n.getStatus() == NotificationStatus.PENDING); i++) {
            sender.sendDue();
        }
        List<MockTelegramClient.SentMessage> toA = mock.since(before, chatA);
        List<MockTelegramClient.SentMessage> toB = mock.since(before, chatB);
        List<String> filesA = toA.stream().filter(o -> o.photoId() != null).map(MockTelegramClient.SentMessage::photoId).toList();
        List<String> filesB = toB.stream().filter(o -> o.photoId() != null).map(MockTelegramClient.SentMessage::photoId).toList();
        assertEquals(4, filesA.size(), toA.toString());
        assertEquals(filesA, filesB, "the second parent gets the same Telegram file ids — nothing uploaded twice");
        assertEquals(3, toA.stream().filter(o -> "album".equals(o.op())).count(), "3 photos as one album");
        assertTrue(toA.get(toA.size() - 1).text().contains("Ertaga ochiq dars"), "the text follows the files");
        Integer remembered = jdbc.queryForObject("select count(*) from broadcast_attachment where broadcast_id = ? and file_id is not null",
                Integer.class, id);
        assertEquals(4, remembered);

        // history: progress counters and who received it
        JsonNode one = admin.get("/api/broadcasts/" + id).json();
        assertEquals(2, one.get("sent").asInt());
        assertEquals(0, one.get("pending").asInt());
        JsonNode recipients = admin.get("/api/broadcasts/" + id + "/recipients").json();
        assertEquals(2, recipients.get("totalElements").asInt());
        JsonNode r0 = recipients.get("content").get(0);
        assertEquals("SENT", r0.get("status").asString());
        assertTrue(r0.get("className").asString().contains("-"));
        assertNotNull(r0.get("parentName").asString());
        assertEquals(0, admin.get("/api/broadcasts/" + id + "/recipients?status=FAILED").json().get("totalElements").asInt());
        assertEquals(2, admin.get("/api/broadcasts/" + id + "/recipients?classId=" + student.getSchoolClass().getId())
                .json().get("totalElements").asInt());
        assertTrue(admin.get("/api/broadcasts?schoolId=" + schoolId + "&q=ochiq%20dars").json().get("totalElements").asInt() >= 1);
        // history filters: class (reached a parent of it) and status (any recipient in it)
        assertTrue(ids(admin.get("/api/broadcasts?schoolId=" + schoolId + "&size=200&classId=" + student.getSchoolClass().getId())).contains(id));
        assertTrue(ids(admin.get("/api/broadcasts?schoolId=" + schoolId + "&size=200&status=SENT")).contains(id));
        assertFalse(ids(admin.get("/api/broadcasts?schoolId=" + schoolId + "&size=200&status=FAILED")).contains(id));
        Long otherClass = jdbc.queryForObject("select c.id from school_class c join academic_year y on y.id = c.academic_year_id "
                + "where y.school_id = ? and c.id <> ? order by c.id limit 1", Long.class, schoolId, student.getSchoolClass().getId());
        assertFalse(ids(admin.get("/api/broadcasts?schoolId=" + schoolId + "&size=200&classId=" + otherClass)).contains(id));

        // another school's admin cannot read it
        School other = schools.findAll().stream().filter(s -> !s.getId().equals(schoolId)).findFirst().orElseThrow();
        otherEmployee = new Employee();
        otherEmployee.setSchool(other);
        otherEmployee.setPosition(positions.findAll().get(0));
        otherEmployee.setFirstName("Boshqa");
        otherEmployee.setLastName("Admin");
        otherEmployee.setPhone("+9989" + (30_000_000 + ThreadLocalRandom.current().nextInt(1_000_000)));
        otherEmployee = employees.save(otherEmployee);
        ApiClient stranger = as(Role.ADMIN, otherEmployee);
        assertEquals(403, stranger.get("/api/broadcasts/" + id).status());
        assertEquals(403, stranger.get("/api/broadcasts/" + id + "/recipients").status());
        assertEquals(403, stranger.get("/api/broadcasts?schoolId=" + schoolId).status());
        assertEquals(403, stranger.get("/api/telegram/parents?classId=" + student.getSchoolClass().getId()).status());
    }

    private static List<Long> ids(ApiClient.Response r) {
        assertEquals(200, r.status(), r.body());
        List<Long> ids = new ArrayList<>();
        for (JsonNode b : r.json().get("content")) ids.add(b.get("id").asLong());
        return ids;
    }

    @Test
    void parentsPage_showsTheGuardiansRelation() throws Exception {
        GuardianRelation before = student.getGuardianRelation();
        student.setGuardianRelation(GuardianRelation.FATHER);
        students.save(student);
        try {
            JsonNode rows = as(Role.ADMIN, null).get("/api/telegram/parents?classId=" + student.getSchoolClass().getId()).json().get("rows");
            JsonNode row = null;
            for (JsonNode r : rows) if (r.get("studentId").asLong() == student.getId()) row = r;
            assertNotNull(row);
            assertEquals("FATHER", row.get("guardianRelation").asString());
        } finally {
            student.setGuardianRelation(before);
            students.save(student);
        }
    }

    @Test
    void broadcastLimits_areChecked() throws Exception {
        ApiClient admin = as(Role.ADMIN, null);
        Map<String, Object> data = Map.of("schoolId", schoolId, "audience", "PARENTS",
                "linkIds", List.of(created.get(0).getId()), "text", "");
        assertEquals(409, admin.multipart("/api/broadcasts", List.of(Part.json("data", data))).status(), "nothing to send");

        List<Part> eleven = new ArrayList<>(List.of(Part.json("data", data)));
        for (int i = 0; i < 11; i++) eleven.add(Part.file("files", "r" + i + ".jpg", "image/jpeg", new byte[]{1, 2, 3}));
        assertEquals(409, admin.multipart("/api/broadcasts", eleven).status(), "at most 10 files");

        ApiClient.Response exe = admin.multipart("/api/broadcasts", List.of(Part.json("data", data),
                Part.file("files", "virus.exe", "application/x-msdownload", new byte[]{'M', 'Z'})));
        assertEquals(409, exe.status());
        ApiClient.Response svg = admin.multipart("/api/broadcasts", List.of(Part.json("data", data),
                Part.file("files", "logo.svg", "image/svg+xml", "<svg/>".getBytes())));
        assertEquals(409, svg.status());

        // an editor may not broadcast
        Employee none = null;
        assertEquals(403, as(Role.EDITOR, none).multipart("/api/broadcasts", List.of(Part.json("data", data))).status());
    }

    @Test
    void stats_haveThirtyDays_appeals_andWeakestClassesFirst() throws Exception {
        JsonNode s = as(Role.ADMIN, null).get("/api/bot/stats?schoolId=" + schoolId).json();
        assertEquals(30, s.get("messagesPerDay").size());
        assertTrue(s.has("appeals30"));
        assertTrue(s.has("avgReplyMinutes"));
        double previous = -1;
        for (JsonNode c : s.get("classes")) {
            double p = c.get("percent").isNull() ? 0 : c.get("percent").asDouble();
            assertTrue(p >= previous, "weakest coverage first");
            previous = p;
        }
    }
}
