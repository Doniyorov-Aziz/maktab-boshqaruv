package uz.azizbek.maktabboshqaruv;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.config.LibraryDemoSeeder;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.BookDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.WikisourceImportRequest;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.service.library.LibraryCatalog;
import uz.azizbek.maktabboshqaruv.service.library.LibraryPermissions;
import uz.azizbek.maktabboshqaruv.service.library.LibraryText;
import uz.azizbek.maktabboshqaruv.service.library.wikisource.WikisourceApi;
import uz.azizbek.maktabboshqaruv.service.library.wikisource.WikisourceImport;
import uz.azizbek.maktabboshqaruv.support.ApiClient;

import java.awt.Color;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The library's legal rules: "Jamoat mulki" only when the author died more than 70 years ago (qonun,
 * 35-modda) and the work was published in their lifetime and has no modern commentary; MUALLIF_RUXSATI
 * only with a permission in force, whose end makes its books inactive; and the Wikisource import
 * producing a PDF in which Uzbek Latin and Cyrillic letters survive (read back with PDFBox).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        // the same properties as SchoolScopeIntegrationTest: one shared Spring context (and DB pool)
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class LibraryLegalIntegrationTest {

    private static final String PASSWORD = "pw-" + ThreadLocalRandom.current().nextInt(1_000_000);
    private static final String TAG = "L" + ThreadLocalRandom.current().nextInt(100_000, 999_999);

    @LocalServerPort
    private int port;
    @Autowired private StudentRepository students;
    @Autowired private UserRepository users;
    @Autowired private EmployeeRepository employees;
    @Autowired private PositionRepository positions;
    @Autowired private PasswordEncoder encoder;
    @Autowired private BookRepository books;
    @Autowired private AuthorRepository authors;
    @Autowired private LibraryPermissionRepository permissions;
    @Autowired private LibraryPermissions permissionService;
    @Autowired private LibraryCatalog catalog;
    @Autowired private WikisourceImport wikisource;
    @Autowired private FileStorage storage;
    @Autowired private Clock clock;

    private long schoolId;
    private ApiClient admin;
    private String adminName;
    private Long qodiriy;
    private final List<User> createdUsers = new ArrayList<>();
    private final List<Employee> createdEmployees = new ArrayList<>();
    private final List<Long> createdAuthors = new ArrayList<>();

    @BeforeEach
    void setUp() throws Exception {
        Student s = students.findAll().stream().filter(x -> x.getSchoolClass().getAcademicYear().getSchool().getId() == 1L)
                .findFirst().orElseThrow();
        School school = s.getSchoolClass().getAcademicYear().getSchool();
        schoolId = school.getId();
        int n = ThreadLocalRandom.current().nextInt(1_000_000);
        Employee e = new Employee();
        e.setSchool(school);
        e.setPosition(positions.findAll().get(0));
        e.setFirstName("Huquq");
        e.setLastName("Test");
        e.setPhone("+9989" + (50_000_000 + n));
        createdEmployees.add(e = employees.save(e));
        User u = new User();
        adminName = "legal_admin_" + n;
        u.setUsername(adminName);
        u.setPassword(encoder.encode(PASSWORD));
        u.setRole(Role.ADMIN);
        u.setEmployee(e);
        createdUsers.add(users.save(u));
        admin = new ApiClient(port).login(adminName, PASSWORD);
        qodiriy = authors.findByFullNameIgnoreCase("Abdulla Qodiriy").orElseThrow(
                () -> new IllegalStateException("AuthorSeeder must seed the classics")).getId();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        List<Book> mine = books.findAll().stream().filter(b -> b.getTitle().contains(TAG)).toList();
        for (Book b : mine) {
            storage.delete(b.getPdfPath());
            storage.delete(b.getCoverPath());
        }
        books.deleteAll(mine);
        List<LibraryPermission> perms = permissions.findAll().stream().filter(p -> p.getGrantorFullName().contains(TAG)).toList();
        perms.forEach(p -> storage.delete(p.getDocumentPath()));
        permissions.deleteAll(perms);
        authors.deleteAllById(createdAuthors);
        users.deleteAll(createdUsers);
        employees.deleteAll(createdEmployees);
        catalog.evict(schoolId);
    }

    // ------------------------------------------------------------------ helpers

    private Long author(String name, Integer died) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("fullName", name + " " + TAG);
        body.put("deathYear", died);
        ApiClient.Response r = admin.post("/api/library/authors", body);
        assertEquals(200, r.status(), r.body());
        Long id = r.json().get("id").asLong();
        createdAuthors.add(id);
        return id;
    }

    private Map<String, Object> book(String title, Long authorId, String basis) {
        Map<String, Object> m = new HashMap<>();
        m.put("title", title + " " + TAG);
        m.put("authorIds", List.of(authorId));
        m.put("language", "uz");
        m.put("ageGroup", "KATTALAR");
        m.put("legalBasis", basis);
        m.put("publishedInLifetime", true);
        m.put("noModernCommentary", true);
        return m;
    }

    private ApiClient.Response save(Map<String, Object> book) throws Exception {
        byte[] pdf = LibraryDemoSeeder.samplePdf(String.valueOf(book.get("title")), "x", Color.GRAY, 1);
        return admin.multipart("/api/library/books?schoolId=" + schoolId + "&allowDuplicate=true", List.of(
                ApiClient.Part.json("book", book), ApiClient.Part.file("pdf", "k.pdf", "application/pdf", pdf)));
    }

    private Long permission(LocalDate from, LocalDate until) throws Exception {
        Map<String, Object> p = new HashMap<>();
        p.put("grantor", "MUALLIF");
        p.put("grantorFullName", "Ruxsat beruvchi " + TAG);
        p.put("validFrom", from.toString());
        if (until != null) p.put("validUntil", until.toString());
        byte[] scan = LibraryDemoSeeder.samplePdf("Ruxsatnoma", "imzo", Color.WHITE, 1);
        ApiClient.Response r = admin.multipart("/api/library/permissions?schoolId=" + schoolId, List.of(
                ApiClient.Part.json("permission", p), ApiClient.Part.file("document", "xat.pdf", "application/pdf", scan)));
        assertEquals(200, r.status(), r.body());
        return r.json().get("id").asLong();
    }

    // ------------------------------------------------------------------ Jamoat mulki

    @Test
    void qodiriy_1938_publicDomain_isSaved() throws Exception {
        ApiClient.Response r = save(book("O'tkan kunlar", qodiriy, "JAMOAT_MULKI"));
        assertEquals(200, r.status(), r.body());
        assertEquals("FREE", r.json().get("authors").get(0).get("status").asString());
        assertEquals(2009, r.json().get("authors").get(0).get("statusYear").asInt(), "free from 1938 + 71");
        assertEquals("Abdulla Qodiriy", r.json().get("author").asString());
    }

    @Test
    void oybek_1968_isProtectedUntil2038() throws Exception {
        Long oybek = author("Oybek", 1968);
        ApiClient.Response r = save(book("Navoiy", oybek, "JAMOAT_MULKI"));
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("2038-yilgacha"), r.body());
        assertTrue(r.body().contains("ruxsati kerak"), r.body());
    }

    @Test
    void authorWithoutDeathYear_isRefused() throws Exception {
        Long unknown = author("Nomalum muallif", null);
        ApiClient.Response r = save(book("Kitob", unknown, "JAMOAT_MULKI"));
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("vafot yili"), r.body());
    }

    @Test
    void publishedAfterDeath_in1990_isRefused() throws Exception {
        Map<String, Object> b = book("Vafotdan keyingi nashr", qodiriy, "JAMOAT_MULKI");
        b.put("publishedInLifetime", false);
        b.put("firstPublishedYear", 1990);
        ApiClient.Response r = save(b);
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("2060-yilgacha"), r.body());

        b.remove("firstPublishedYear");
        ApiClient.Response noYear = save(b);
        assertEquals(400, noYear.status(), noYear.body());
        assertTrue(noYear.body().contains("birinchi nashr yili"), noYear.body());
    }

    @Test
    void modernCommentaryConfirmation_isRequired() throws Exception {
        Map<String, Object> b = book("Izohli nashr", qodiriy, "JAMOAT_MULKI");
        b.put("noModernCommentary", false);
        ApiClient.Response r = save(b);
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("so'zboshi"), r.body());
    }

    // ------------------------------------------------------------------ permissions

    @Test
    void authorPermission_withoutPermission_isRefused() throws Exception {
        Long oybek = author("Oybek", 1968);
        ApiClient.Response r = save(book("Qutlug' qon", oybek, "MUALLIF_RUXSATI"));
        assertEquals(400, r.status(), r.body());
        assertTrue(r.body().contains("ruxsatnoma"), r.body());
    }

    @Test
    void expiredPermission_job_makesTheBookInactive_andTheBotStopsShowingIt() throws Exception {
        LocalDate today = LocalDate.now(clock);
        Long oybek = author("Oybek", 1968);
        Long permissionId = permission(today.minusMonths(6), today.plusDays(10));
        Map<String, Object> b = book("Ruxsatli kitob", oybek, "MUALLIF_RUXSATI");
        b.put("permissionId", permissionId);
        ApiClient.Response saved = save(b);
        assertEquals(200, saved.status(), saved.body());
        Long bookId = saved.json().get("id").asLong();
        assertNotNull(catalog.book(schoolId, bookId), "in force: parents see it");

        // the permission ran out yesterday
        LibraryPermission p = permissions.findById(permissionId).orElseThrow();
        p.setValidUntil(today.minusDays(1));
        permissions.save(p);
        assertEquals(1, permissionService.runDaily(today));

        assertFalse(books.findById(bookId).orElseThrow().getActive());
        assertNull(catalog.book(schoolId, bookId), "the bot no longer shows it");
        assertTrue(catalog.search(schoolId, "Ruxsatli kitob " + TAG, 0).isEmpty(), "nor finds it");
        assertEquals("EXPIRED", admin.get("/api/library/permissions?schoolId=" + schoolId).json().findValues("status")
                .stream().filter(n -> n.asString().equals("EXPIRED")).findFirst().orElseThrow().asString());
        // saving the book again under the expired permission is refused
        assertEquals(400, save(b).status());
    }

    @Test
    void revokedPermission_stopsItsBooksAtOnce() throws Exception {
        LocalDate today = LocalDate.now(clock);
        Long oybek = author("Oybek", 1968);
        Long permissionId = permission(today.minusDays(1), null);
        Map<String, Object> b = book("Bekor qilinadigan", oybek, "MUALLIF_RUXSATI");
        b.put("permissionId", permissionId);
        Long bookId = save(b).json().get("id").asLong();
        assertTrue(books.findById(bookId).orElseThrow().getActive());

        assertEquals(200, admin.post("/api/library/permissions/" + permissionId + "/revoke", "{}").status());
        assertFalse(books.findById(bookId).orElseThrow().getActive());
        assertNull(catalog.book(schoolId, bookId));
    }

    @Test
    void letterTemplate_isAPdf_withTheSchoolTextFilledIn() throws Exception {
        ApiClient.Response pdf = admin.get("/api/library/permissions/template?schoolId=" + schoolId + "&title=Sinov");
        assertEquals(200, pdf.status());
        byte[] bytes = pdf.raw().body();
        try (PDDocument d = Loader.loadPDF(bytes)) {
            // lines wrap and justified lines have wider gaps — compare words, not the layout
            String text = new PDFTextStripper().getText(d).replaceAll("\\s+", " ");
            assertTrue(text.contains("RUXSATNOMA"), text);
            assertTrue(text.contains("«Sinov»"), text);
            assertTrue(text.contains("BEPUL elektron shaklda (PDF)"), text);
        }
    }

    // ------------------------------------------------------------------ Wikisource (mock API)

    /** A fake Wikisource: an author page with two works; one work in two chapters. */
    static class FakeWikisource implements WikisourceApi {
        final Map<String, String> pages = new HashMap<>();
        int calls;

        @Override
        public String wikitext(String host, String title) {
            calls++;
            return pages.get(host + "|" + title);
        }

        @Override
        public List<String> search(String host, String query, int namespace, int limit) {
            calls++;
            String q = LibraryText.normalize(query);
            return pages.keySet().stream().filter(k -> k.startsWith(host + "|"))
                    .map(k -> k.substring(k.indexOf('|') + 1))
                    .filter(t -> LibraryText.normalize(t).contains(q)).limit(limit).toList();
        }

        @Override
        public List<String> pagesWithPrefix(String host, String prefix, int limit) {
            return List.of();
        }
    }

    private static final String PROSE = "Oʻtkan zamonda Margʻilonda shoʻx bir yigit yashardi; u choʻl va togʻ "
            + "oshib Toshkandga yoʻl oldi. Ўткан кунлар — Абдулла Қодирий асари: «Ҳаёт ғалваси, қишлоқ ҳовлиси». ";

    private FakeWikisource fake(String work) {
        FakeWikisource f = new FakeWikisource();
        f.pages.put("wikisource.org|Author:Abdulla Qodiriy", "{{Author\n | firstname = Abdulla Qodiriy\n | deathyear = 1938\n}}\n"
                + "==Asarlari==\n===Romanlari===\n*[[" + work + "]] (1926)\n*[[Qisqa asar " + TAG + "]] (1920)\n"
                + "*[[Chala asar " + TAG + "]] (1922)\n");
        f.pages.put("wikisource.org|" + work, "{{header\n | title = " + work + "\n | author = Abdulla Qodiriy\n}}\n"
                + "*[[" + work + "/01|01]]. Otabek\n*[[" + work + "/02|02]]. Kumush<ref>zamonaviy izoh</ref>\n");
        f.pages.put("wikisource.org|" + work + "/01", "{{header|title=x}}\n" + PROSE.repeat(6) + "\n\n'''Ikkinchi''' xatboshi [[Toshkand|Toshkandda]].");
        f.pages.put("wikisource.org|" + work + "/02", "{{header|title=x}}\n<poem>\nChoʻlpon yulduz,\nShoʻx shamol.\n</poem>\n\n"
                + PROSE.repeat(4));
        f.pages.put("wikisource.org|Qisqa asar " + TAG, "{{header|title=q}}\nBir-ikki satr.");
        f.pages.put("wikisource.org|Chala asar " + TAG, "{{tayyorlanmoqda}}\n" + PROSE.repeat(10));
        return f;
    }

    private void signedIn() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(adminName, null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @Test
    void wikisourceImport_makesAPdf_withUzbekLatinAndCyrillicLetters() throws Exception {
        signedIn();
        String work = "Sinov roman " + TAG;
        FakeWikisource api = fake(work);
        WikisourceImport importer = wikisource.withApi(api);

        var works = importer.works(schoolId, qodiriy);
        assertEquals("Author:Abdulla Qodiriy", works.page());
        assertTrue(works.works().stream().anyMatch(w -> w.title().equals(work) && w.year() == 1926), works.toString());

        BookDto book = importer.importWork(schoolId, new WikisourceImportRequest(qodiriy, work, null, true, null, true));
        assertEquals("JAMOAT_MULKI", book.legalBasis().name());
        assertTrue(book.legalNote().startsWith("Wikisource: https://wikisource.org/wiki/"), book.legalNote());
        assertEquals("Abdulla Qodiriy", book.author());
        assertTrue(book.hasCover(), "cover drawn from the PDF's first page");

        Book saved = books.findById(book.id()).orElseThrow();
        byte[] pdf = storage.read(saved.getPdfPath());
        try (PDDocument d = Loader.loadPDF(pdf)) {
            assertTrue(d.getNumberOfPages() >= 4, "cover, contents, two chapters");
            String text = new PDFTextStripper().getText(d).replaceAll("\\s+", " ");
            for (String s : List.of("Oʻtkan", "Margʻilon", "shoʻx", "choʻl", "togʻ", "Ўткан кунлар", "Абдулла Қодирий",
                    "Ҳаёт ғалваси", "Choʻlpon yulduz", "Mundarija", "Jamoat mulki · Manba: Wikisource", "(1894–1938)",
                    "Toshkandda")) {
                assertTrue(text.contains(s), "PDF da yo'q: " + s);
            }
            assertFalse(text.contains("zamonaviy izoh"), "<ref> notes are removed");
            assertFalse(text.contains("'''"), "wiki markup is removed");
        }

        // the same work again is refused; the list marks it imported
        ResponseStatusException again = assertThrows(ResponseStatusException.class,
                () -> importer.importWork(schoolId, new WikisourceImportRequest(qodiriy, work, null, true, null, true)));
        assertTrue(again.getReason().contains("allaqachon"), again.getReason());
        assertTrue(importer.works(schoolId, qodiriy).works().stream().filter(w -> w.title().equals(work)).findFirst()
                .orElseThrow().imported());
    }

    @Test
    void wikisourceImport_refusesShortAndUnfinishedPages_andProtectedAuthors() throws Exception {
        signedIn();
        FakeWikisource api = fake("Boshqa " + TAG);
        WikisourceImport importer = wikisource.withApi(api);

        ResponseStatusException shortPage = assertThrows(ResponseStatusException.class, () -> importer.importWork(schoolId,
                new WikisourceImportRequest(qodiriy, "Qisqa asar " + TAG, null, true, null, true)));
        assertTrue(shortPage.getReason().contains("juda qisqa"), shortPage.getReason());

        ResponseStatusException unfinished = assertThrows(ResponseStatusException.class, () -> importer.importWork(schoolId,
                new WikisourceImportRequest(qodiriy, "Chala asar " + TAG, null, true, null, true)));
        assertTrue(unfinished.getReason().contains("tayyorlanmoqda"), unfinished.getReason());

        Long oybek = author("Oybek", 1968);
        int before = api.calls;
        ResponseStatusException prot = assertThrows(ResponseStatusException.class, () -> importer.importWork(schoolId,
                new WikisourceImportRequest(oybek, "Qutlug' qon", null, true, null, true)));
        assertTrue(prot.getReason().contains("2038-yilgacha"), prot.getReason());
        assertEquals(before, api.calls, "nothing is fetched for a protected author");
        assertTrue(books.findAll().stream().noneMatch(b -> b.getTitle().contains("Qisqa asar " + TAG)));
    }
}
