package uz.azizbek.maktabboshqaruv.config;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.DownloadSource;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.NotificationService;
import uz.azizbek.maktabboshqaruv.service.library.LibraryDefaults;
import uz.azizbek.maktabboshqaruv.service.library.LibraryFiles;
import uz.azizbek.maktabboshqaruv.service.library.LibraryText;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Library sample data — ONLY with the "dev" profile, never in prod. The books are PDFs generated
 * right here ("Namuna kitob 1" … "Namuna kitob 12": a coloured title page and a few pages of
 * placeholder text) — no real, copyrighted book is ever part of the code or the seed. Each is
 * marked MAKTAB_MATERIALI with a note that it is a generated sample. A few weeks of downloads by
 * the school's linked parents make the statistics page show something. Runs once per school.
 */
@Component
@Profile("dev")
@Order(20)
public class LibraryDemoSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LibraryDemoSeeder.class);
    private static final int BOOKS = 12;
    private static final Color[] COLORS = {new Color(0x4F46E5), new Color(0x0EA5E9), new Color(0x10B981),
            new Color(0xF59E0B), new Color(0xEF4444), new Color(0x8B5CF6), new Color(0xEC4899), new Color(0x14B8A6)};
    private static final String[] LANGS = {"uz", "uz", "ru", "uz", "en", "uz"};
    private static final AgeGroup[] AGES = {AgeGroup.KATTALAR, AgeGroup.BOLALAR, AgeGroup.OSMIRLAR};
    private static final DownloadSource[] SOURCES = {DownloadSource.SEARCH, DownloadSource.CATEGORY,
            DownloadSource.CATEGORY, DownloadSource.RECOMMENDATION, DownloadSource.TOP, DownloadSource.NEW};

    private final SchoolRepository schools;
    private final BookRepository books;
    private final BookCategoryRepository categories;
    private final BookDownloadRepository downloads;
    private final ParentTelegramLinkRepository links;
    private final ParentSessionRepository sessions;
    private final LibraryDefaults defaults;
    private final LibraryFiles files;
    private final Clock clock;

    public LibraryDemoSeeder(SchoolRepository schools, BookRepository books, BookCategoryRepository categories,
                             BookDownloadRepository downloads, ParentTelegramLinkRepository links,
                             ParentSessionRepository sessions, LibraryDefaults defaults, LibraryFiles files, Clock clock) {
        this.schools = schools;
        this.books = books;
        this.categories = categories;
        this.downloads = downloads;
        this.links = links;
        this.sessions = sessions;
        this.defaults = defaults;
        this.files = files;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Map<Long, List<ParentTelegramLink>> linksBySchool = links.findAllActive().stream()
                .collect(Collectors.groupingBy(l -> NotificationService.schoolOf(l.getStudent()).getId()));
        for (School school : schools.findAll()) {
            if (books.countBySchoolIdAndActiveTrueAndDraftFalse(school.getId()) > 0) continue;
            defaults.ensureCategories(school.getId());
            List<BookCategory> shelves = categories.findBySchoolIdOrderByPositionAscIdAsc(school.getId());
            List<Book> created = new ArrayList<>();
            for (int i = 1; i <= BOOKS; i++) {
                try {
                    created.add(book(school, shelves.get((i - 1) % shelves.size()), i));
                } catch (IOException e) {
                    log.warn("Namuna kitob {} yaratilmadi: {}", i, e.getMessage());
                }
            }
            int history = history(school, created, linksBySchool.getOrDefault(school.getId(), List.of()));
            log.info("Kutubxona (dev): {} — {} ta namunaviy kitob, {} ta yuklash tarixi", school.getName(), created.size(), history);
        }
    }

    private Book book(School school, BookCategory shelf, int n) throws IOException {
        String title = "Namuna kitob " + n;
        String author = "Namuna muallif " + (char) ('A' + (n - 1) % 6);
        byte[] pdf = samplePdf(title, author, COLORS[(n - 1) % COLORS.length], 3 + n % 4);
        LibraryFiles.PdfInfo info = files.checkPdf(pdf);
        Book b = new Book();
        b.setSchool(school);
        b.setTitle(title);
        b.setAuthor(author);
        b.setCategory(shelf);
        b.setDescription("Ishlab chiqish (dev) muhiti uchun generatsiya qilingan namunaviy PDF — haqiqiy kitob emas. "
                + "Kutubxona, qidiruv, tavsiyalar va yuklab olishni sinash uchun.");
        b.setLanguage(LANGS[(n - 1) % LANGS.length]);
        b.setAgeGroup(AGES[(n - 1) % AGES.length]);
        b.setPages(info.pages());
        b.setYear(2020 + n % 6);
        b.setLegalBasis(LegalBasis.MAKTAB_MATERIALI);
        b.setLegalNote("Namunaviy fayl (dev seed), generatsiya qilingan");
        b.setActive(true);
        b.setFeatured(n % 4 == 1);
        b.setDraft(false);
        b.setSearchKey(LibraryText.key(title, author));
        b.setDownloadCount(0L);
        b.setLikes(0);
        b.setDislikes(0);
        b.setCreatedBy("seed");
        b.setCreatedAt(LocalDateTime.now(clock).minusDays(BOOKS - n));
        b.setPdfPath(files.savePdf(school.getId(), pdf));
        b.setPdfSize(info.size());
        b.setPdfSha256(info.sha256());
        byte[] cover = files.coverFromPdf(pdf);
        if (cover != null) b.setCoverPath(files.saveCover(school.getId(), "jpg", cover));
        return books.save(b);
    }

    /** A few weeks of downloads by the linked parents (deterministic), with the counters kept in step. */
    private int history(School school, List<Book> created, List<ParentTelegramLink> schoolLinks) {
        if (created.isEmpty() || schoolLinks.isEmpty()) return 0;
        Map<Long, ParentSession> byChat = new HashMap<>();
        for (ParentSession s : sessions.findAll()) byChat.put(s.getChatId(), s);
        Random random = new Random(42);
        LocalDateTime now = LocalDateTime.now(clock);
        Set<Long> chats = new LinkedHashSet<>();
        for (ParentTelegramLink l : schoolLinks) chats.add(l.getChatId());
        int count = 0;
        int i = 0;
        for (Long chatId : chats) {
            if (i++ % 3 == 2) continue; // not every parent reads
            int n = 1 + random.nextInt(4);
            for (int k = 0; k < n; k++) {
                Book b = created.get(random.nextInt(created.size()));
                BookDownload d = new BookDownload();
                d.setSchool(school);
                d.setBook(b);
                d.setParent(byChat.get(chatId));
                d.setChatId(chatId);
                d.setDownloadedAt(now.minusDays(random.nextInt(30)).minusMinutes(random.nextInt(600)));
                d.setSource(SOURCES[random.nextInt(SOURCES.length)]);
                downloads.save(d);
                b.setDownloadCount(b.getDownloadCount() + 1);
                if (random.nextInt(3) == 0) b.setLikes(b.getLikes() + 1);
                else if (random.nextInt(8) == 0) b.setDislikes(b.getDislikes() + 1);
                count++;
            }
            if (count > 400) break;
        }
        books.saveAll(created);
        return count;
    }

    /** A small A5 PDF: a coloured title page, then pages of placeholder text. */
    public static byte[] samplePdf(String title, String author, Color color, int pages) throws IOException {
        try (PDDocument doc = new PDDocument(); InputStream boldIn = font("Inter-Bold"); InputStream regIn = font("Inter-Regular")) {
            PDType0Font bold = PDType0Font.load(doc, boldIn);
            PDType0Font regular = PDType0Font.load(doc, regIn);
            PDRectangle size = PDRectangle.A5;
            for (int p = 0; p < pages; p++) {
                PDPage page = new PDPage(size);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    if (p == 0) {
                        cs.setNonStrokingColor(color);
                        cs.addRect(0, 0, size.getWidth(), size.getHeight());
                        cs.fill();
                        cs.setNonStrokingColor(new Color(255, 255, 255, 60));
                        cs.addRect(28, size.getHeight() * 0.35f, size.getWidth() - 56, 4);
                        cs.fill();
                        text(cs, bold, 26, Color.WHITE, 32, size.getHeight() * 0.62f, title);
                        text(cs, regular, 14, Color.WHITE, 32, size.getHeight() * 0.62f - 30, author);
                        text(cs, regular, 10, Color.WHITE, 32, 40, "Namunaviy PDF · dev seed · haqiqiy kitob emas");
                    } else {
                        text(cs, bold, 13, Color.DARK_GRAY, 40, size.getHeight() - 60, title + " — " + p + "-bet");
                        float y = size.getHeight() - 95;
                        for (int line = 0; line < 18; line++) {
                            text(cs, regular, 10, Color.GRAY, 40, y, "Bu sahifa sinov uchun yozilgan namunaviy matn, " + (line + 1) + "-qator.");
                            y -= 16;
                        }
                    }
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static void text(PDPageContentStream cs, PDType0Font font, float size, Color color, float x, float y, String s)
            throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(color);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(s);
        cs.endText();
    }

    private static InputStream font(String name) throws IOException {
        InputStream in = LibraryDemoSeeder.class.getResourceAsStream("/fonts/" + name + ".ttf");
        if (in == null) throw new IOException("Shrift topilmadi: " + name);
        return in;
    }
}
