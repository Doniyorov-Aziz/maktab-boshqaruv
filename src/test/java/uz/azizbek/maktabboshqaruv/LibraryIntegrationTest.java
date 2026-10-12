package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.bot.BotRouter;
import uz.azizbek.maktabboshqaruv.config.LibraryDemoSeeder;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.DownloadSource;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationSender;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog.DownloadResult;
import uz.azizbek.maktabboshqaruv.support.ApiClient;
import uz.azizbek.maktabboshqaruv.telegram.MockTelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramModels;

import java.awt.Color;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 📚 Kitob mutolaasi — the rules that must hold: the legal basis is required, only real PDFs
 * within 50 MB are accepted, search ignores case / apostrophes / script, inactive and foreign
 * books never reach a parent, the daily limit, the "Siz uchun" order, the Telegram file_id
 * reuse, and the reading statistics being admin-only.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "telegram.mock=true",
        "telegram.bot-username=maktab_test_bot",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class LibraryIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final String TAG = "T" + ThreadLocalRandom.current().nextInt(100_000, 999_999);

    @LocalServerPort
    private int port;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private PositionRepository positions;
    @Autowired private PasswordEncoder encoder;
    @Autowired private BookRepository books;
    @Autowired private BookCategoryRepository categories;
    @Autowired private BookDownloadRepository downloads;
    @Autowired private BookFavoriteRepository favorites;
    @Autowired private BookRatingRepository ratings;
    @Autowired private NotificationLogRepository notificationLog;
    @Autowired private ParentTelegramLinkRepository links;
    @Autowired private ParentSessionRepository sessions;
    @Autowired private LibraryCatalog catalog;
    @Autowired private FileStorage storage;
    @Autowired private BotRouter router;
    @Autowired private NotificationSender sender;
    @Autowired private TelegramClient telegram;
    @Autowired private Clock clock;

    private Student a;
    private Student b;
    private long chatId;
    private ApiClient adminA;
    private final List<User> createdUsers = new ArrayList<>();
    private final List<Employee> createdEmployees = new ArrayList<>();
    private final List<Long> createdBooks = new ArrayList<>();
    private final List<Long> createdCategories = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        List<Student> all = students.findAll();
        a = all.stream().filter(s -> school(s) == 1L && s.getTelegramLinkCode() != null).findFirst().orElseThrow();
        b = all.stream().filter(s -> school(s) != 1L).findFirst().orElseThrow();
        chatId = 9_100_000_000L + ThreadLocalRandom.current().nextInt(1_000_000);
        adminA = userOf(school(a), Role.ADMIN);
    }

    @AfterEach
    void cleanUp() {
        List<Book> mine = books.findAllById(createdBooks);
        downloads.deleteAll(downloads.findAll().stream().filter(d -> createdBooks.contains(d.getBook().getId()) || d.getChatId() == chatId).toList());
        favorites.deleteAll(favorites.findAll().stream().filter(f -> createdBooks.contains(f.getBook().getId())).toList());
        ratings.deleteAll(ratings.findAll().stream().filter(r -> createdBooks.contains(r.getBook().getId())).toList());
        notificationLog.deleteAll(notificationLog.findAll().stream().filter(n -> n.getChatId() == chatId).toList());
        for (Book book : mine) {
            storage.delete(book.getPdfPath());
            storage.delete(book.getCoverPath());
        }
        books.deleteAll(mine);
        categories.deleteAllById(createdCategories);
        links.findByChatIdAndActiveTrue(chatId).forEach(links::delete);
        links.findAll().stream().filter(l -> l.getChatId() == chatId).forEach(links::delete);
        sessions.findByChatId(chatId).ifPresent(sessions::delete);
        users.deleteAll(createdUsers);
        employees.deleteAll(createdEmployees);
        catalog.evict(school(a));
        catalog.evict(school(b));
    }

    // ------------------------------------------------------------------ helpers

    private static long school(Student s) {
        return s.getSchoolClass().getAcademicYear().getSchool().getId();
    }

    private ApiClient userOf(Long schoolId, Role role) throws Exception {
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        Employee e = new Employee();
        e.setSchool(schoolId.equals(school(a)) ? a.getSchoolClass().getAcademicYear().getSchool()
                : b.getSchoolClass().getAcademicYear().getSchool());
        e.setPosition(positions.findAll().get(0));
        e.setFirstName("Kutubxona");
        e.setLastName("Test");
        e.setPhone("+9989" + (40_000_000 + n));
        createdEmployees.add(e = employees.save(e));
        User u = new User();
        u.setUsername("lib_" + role + "_" + n);
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(role);
        u.setEmployee(e);
        createdUsers.add(users.save(u));
        return new ApiClient(port).login(u.getUsername(), PASSWORD);
    }

    private static Map<String, Object> bookJson(String title, String author, Long categoryId, boolean withBasis) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", title);
        m.put("author", author);
        m.put("categoryId", categoryId);
        m.put("language", "uz");
        m.put("ageGroup", "KATTALAR");
        if (withBasis) m.put("legalBasis", "MAKTAB_MATERIALI");
        m.put("legalNote", "test");
        return m;
    }

    private Long createBook(ApiClient api, long schoolId, String title, String author, Long categoryId) throws Exception {
        byte[] pdf = LibraryDemoSeeder.samplePdf(title, author == null ? "" : author, Color.BLUE, 2);
        ApiClient.Response r = api.multipart("/api/library/books?schoolId=" + schoolId + "&allowDuplicate=true", List.of(
                ApiClient.Part.json("book", bookJson(title, author, categoryId, true)),
                ApiClient.Part.file("pdf", "kitob.pdf", "application/pdf", pdf)));
        assertEquals(200, r.status(), r.body());
        Long id = r.json().get("id").asLong();
        createdBooks.add(id);
        return id;
    }

    private Long createCategory(long schoolId, String name) throws Exception {
        ApiClient.Response r = adminA.post("/api/library/categories?schoolId=" + schoolId,
                Map.of("nameUz", name + " " + TAG, "emoji", "🕵️"));
        assertEquals(200, r.status(), r.body());
        Long id = r.json().get("id").asLong();
        createdCategories.add(id);
        return id;
    }

    private ParentSession linkParent() {
        router.handle(new TelegramModels.Update(1, new TelegramModels.Message(1L,
                new TelegramModels.User(chatId, false, "Kitobxon", "kitobxon_test"),
                new TelegramModels.Chat(chatId, "private"), "/start " + a.getTelegramLinkCode(), null)));
        return sessions.findByChatId(chatId).orElseThrow();
    }

    private void addDownload(Long bookId, LocalDateTime at) {
        BookDownload d = new BookDownload();
        d.setSchool(a.getSchoolClass().getAcademicYear().getSchool());
        d.setBook(books.getReferenceById(bookId));
        d.setChatId(chatId);
        d.setDownloadedAt(at);
        d.setSource(DownloadSource.CATEGORY);
        downloads.save(d);
    }

    // ------------------------------------------------------------------ rules

    @Test
    void bookWithoutLegalBasis_isNotSaved_400() throws Exception {
        String title = "Asossiz kitob " + TAG;
        byte[] pdf = LibraryDemoSeeder.samplePdf(title, "", Color.RED, 1);
        ApiClient.Response r = adminA.multipart("/api/library/books?schoolId=" + school(a), List.of(
                ApiClient.Part.json("book", bookJson(title, "Muallif", null, false)),
                ApiClient.Part.file("pdf", "kitob.pdf", "application/pdf", pdf)));
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("Huquqiy asos"), r.body());
        assertTrue(books.findAll().stream().noneMatch(x -> x.getTitle().equals(title)));
    }

    @Test
    void exeRenamedToPdf_isRefused() throws Exception {
        byte[] exe = new byte[4096];
        exe[0] = 'M';
        exe[1] = 'Z';
        ApiClient.Response r = adminA.multipart("/api/library/books?schoolId=" + school(a), List.of(
                ApiClient.Part.json("book", bookJson("Virus " + TAG, "X", null, true)),
                ApiClient.Part.file("pdf", "kitob.pdf", "application/pdf", exe)));
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("PDF emas"), r.body());
    }

    @Test
    void pdfOver50Mb_isRefused() throws Exception {
        byte[] big = new byte[50 * 1024 * 1024 + 1];
        big[0] = '%';
        big[1] = 'P';
        big[2] = 'D';
        big[3] = 'F';
        ApiClient.Response r = adminA.multipart("/api/library/books?schoolId=" + school(a), List.of(
                ApiClient.Part.json("book", bookJson("Katta " + TAG, "X", null, true)),
                ApiClient.Part.file("pdf", "katta.pdf", "application/pdf", big)));
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("juda katta"), r.body());
    }

    @Test
    void search_ignoresCase_apostropheKind_andCyrillic() throws Exception {
        Long id = createBook(adminA, school(a), "O‘tkan kunlar " + TAG, "Abdulla Qodiriy", null);
        List<Long> plain = catalog.search(school(a), "o'tkan kunlar", 0).map(Book::getId).getContent();
        List<Long> upper = catalog.search(school(a), "O‘TKAN KUNLAR", 0).map(Book::getId).getContent();
        List<Long> cyrillic = catalog.search(school(a), "ўткан кунлар", 0).map(Book::getId).getContent();
        assertTrue(plain.contains(id), plain.toString());
        assertEquals(plain, upper);
        assertEquals(plain, cyrillic);
        // by author, in Cyrillic too
        assertTrue(catalog.search(school(a), "Абдулла Қодирий", 0).map(Book::getId).getContent().contains(id));
    }

    @Test
    void inactiveBook_isHiddenInTheBot_andCannotBeDownloaded() throws Exception {
        ParentSession parent = linkParent();
        Long id = createBook(adminA, school(a), "Nofaol kitob " + TAG, "Muallif", null);
        assertNotNull(catalog.book(school(a), id));
        assertEquals(204, adminA.delete("/api/library/books/" + id).status());

        assertNull(catalog.book(school(a), id));
        assertFalse(catalog.search(school(a), "Nofaol kitob " + TAG, 0).map(Book::getId).getContent().contains(id));
        assertEquals(DownloadResult.NOT_FOUND, catalog.download(parent, a, id, DownloadSource.SEARCH));
        assertTrue(downloads.findAll().stream().noneMatch(d -> d.getBook().getId().equals(id)));
        // the history is kept: the book still exists, only inactive
        assertFalse(books.findById(id).orElseThrow().getActive());
    }

    @Test
    void anotherSchoolsBook_byId_is403_andNeverReachesTheBot() throws Exception {
        Long id = createBook(adminA, school(a), "Begona kitob " + TAG, "Muallif", null);
        ApiClient adminB = userOf(school(b), Role.ADMIN);
        assertEquals(403, adminB.get("/api/library/books/" + id).status());
        assertEquals(403, adminB.get("/api/library/books/" + id + "/pdf").status());
        assertEquals(403, adminB.delete("/api/library/books/" + id).status());
        assertEquals(200, adminA.get("/api/library/books/" + id).status());
        assertNull(catalog.book(school(b), id), "a parent of school B never sees school A's book");
    }

    @Test
    void dailyLimit_isTwentyBooks() throws Exception {
        ParentSession parent = linkParent();
        Long id = createBook(adminA, school(a), "Limit kitob " + TAG, "Muallif", null);
        LocalDateTime earlier = LocalDateTime.now(clock).withHour(0).withMinute(5);
        for (int i = 0; i < 19; i++) addDownload(id, earlier);
        assertEquals(DownloadResult.QUEUED, catalog.download(parent, a, id, DownloadSource.CATEGORY), "the 20th is allowed");
        Long other = createBook(adminA, school(a), "Limit kitob 2 " + TAG, "Muallif", null);
        assertEquals(DownloadResult.LIMIT, catalog.download(parent, a, other, DownloadSource.CATEGORY), "the 21st is not");
    }

    @Test
    void recommendations_detectiveReader_getsUnreadDetectivesFirst() throws Exception {
        ParentSession parent = linkParent();
        Long detective = createCategory(school(a), "Detektiv");
        Long other = createCategory(school(a), "Boshqa");
        List<Long> d = new ArrayList<>();
        for (int i = 1; i <= 5; i++) d.add(createBook(adminA, school(a), "Detektiv " + i + " " + TAG, "Muallif", detective));
        Long popularOther = createBook(adminA, school(a), "Mashhur boshqa " + TAG, "Muallif", other);
        // other parents made the non-detective book very popular
        Book p = books.findById(popularOther).orElseThrow();
        p.setDownloadCount(500L);
        books.save(p);
        for (int i = 0; i < 3; i++) addDownload(d.get(i), LocalDateTime.now(clock).minusDays(1 + i));

        LibraryCatalog.Recommendations r = catalog.recommendations(school(a), parent.getChatId(), List.of(a));
        assertTrue(r.personal());
        assertTrue(r.forYou().size() >= 2, r.forYou().toString());
        Set<Long> unread = Set.of(d.get(3), d.get(4));
        assertTrue(unread.contains(r.forYou().get(0).getId()), "1st: " + r.forYou().get(0).getTitle());
        assertTrue(unread.contains(r.forYou().get(1).getId()), "2nd: " + r.forYou().get(1).getTitle());
        assertTrue(r.forYou().stream().noneMatch(x -> d.subList(0, 3).contains(x.getId())), "already read books are not suggested");
    }

    @Test
    void secondSending_usesTheTelegramFileId_notANewUpload() throws Exception {
        ParentSession parent = linkParent();
        Long id = createBook(adminA, school(a), "Fayl kitob " + TAG, "Muallif", null);
        MockTelegramClient mock = (MockTelegramClient) telegram;

        int before = mock.size();
        assertEquals(DownloadResult.QUEUED, catalog.download(parent, a, id, DownloadSource.SEARCH));
        sender.sendDue();
        // second request later the same day (the double-tap guard is 15 s)
        BookDownload first = downloads.findAll().stream().filter(x -> x.getBook().getId().equals(id)).findFirst().orElseThrow();
        first.setDownloadedAt(first.getDownloadedAt().minusMinutes(5));
        downloads.save(first);
        assertEquals(DownloadResult.QUEUED, catalog.download(parent, a, id, DownloadSource.SEARCH));
        for (int i = 0; i < 3; i++) {
            Thread.sleep(1100); // the outbox sends at most one message per chat per second
            sender.sendDue();
        }

        List<MockTelegramClient.SentMessage> docs = mock.since(before, chatId).stream()
                .filter(m -> "document".equals(m.op())).toList();
        assertEquals(2, docs.size(), docs.toString());
        assertNotNull(docs.get(0).photoId());
        assertEquals(docs.get(0).photoId(), docs.get(1).photoId(), "the second parent copy is sent by file_id");
        assertTrue(docs.get(0).text().contains("Fayl kitob"), docs.get(0).text());
        assertTrue(docs.get(0).text().contains("Yoqimli mutolaa"), docs.get(0).text());
        assertEquals(2, books.findById(id).orElseThrow().getDownloadCount());
        assertTrue(notificationLog.findAll().stream().filter(n -> n.getChatId() == chatId && n.getType() == NotificationType.BOOK_DOWNLOAD)
                .allMatch(n -> n.getStatus() == NotificationStatus.SENT));
    }

    @Test
    void readingStatistics_areAdminOnly() throws Exception {
        ApiClient editorA = userOf(school(a), Role.EDITOR);
        long s = school(a);
        assertEquals(403, editorA.get("/api/library/stats?schoolId=" + s).status());
        assertEquals(403, editorA.get("/api/library/stats/journal?schoolId=" + s).status());
        assertEquals(403, editorA.get("/api/library/stats/journal/export?schoolId=" + s).status());
        // the editor still manages the books
        assertEquals(200, editorA.get("/api/library/books?schoolId=" + s).status());

        assertEquals(200, adminA.get("/api/library/stats?schoolId=" + s).status());
        assertEquals(200, adminA.get("/api/library/stats/journal?schoolId=" + s).status());
        ApiClient.Response xlsx = adminA.get("/api/library/stats/journal/export?schoolId=" + s);
        assertEquals(200, xlsx.status());
        byte[] body = xlsx.raw().body();
        assertTrue(body.length > 0 && body[0] == 'P' && body[1] == 'K', "an .xlsx is a zip");
        // another school's statistics
        assertEquals(403, adminA.get("/api/library/stats?schoolId=" + school(b)).status());
    }
}
