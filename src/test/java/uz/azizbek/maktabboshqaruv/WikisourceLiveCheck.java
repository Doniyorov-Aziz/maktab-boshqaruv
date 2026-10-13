package uz.azizbek.maktabboshqaruv;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.BookDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.WikisourceAuthorDto;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.WikisourceImportRequest;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.WikisourceWorkDto;
import uz.azizbek.maktabboshqaruv.entity.Author;
import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.AuthorRepository;
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.service.library.wikisource.WikisourceImport;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Not part of the build (tag "wikisource", `gradlew wikisourceCheck`): the real wikisource.org for
 * five classics — which works are listed, which import and why the others do not. Imports into the
 * test database (DB_URL), as the first admin of school 1. Writes WIKISOURCE_REPORT (JSON) and
 * renders pages of the made PDFs into WIKISOURCE_SHOTS.
 *
 * Only works whose year (from the author's page) is within the author's lifetime are imported here:
 * for the others "published in the author's lifetime" is the admin's decision in the dialog, and
 * this check does not make it for them.
 */
@Tag("wikisource")
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000"
})
@ActiveProfiles("local")
class WikisourceLiveCheck {

    private static final long SCHOOL = 1L;
    private static final Map<String, List<String>> PREFERRED = Map.of(
            "Abdulla Qodiriy", List.of("O'tkan kunlar"),
            "Abdulhamid Cho'lpon", List.of("Kecha va kunduz"));

    @Autowired private WikisourceImport wikisource;
    @Autowired private AuthorRepository authors;
    @Autowired private BookRepository books;
    @Autowired private UserRepository users;
    @Autowired private FileStorage storage;

    @Test
    void fiveClassics() throws Exception {
        User admin = users.findAll().stream().filter(u -> u.getRole() == Role.ADMIN).findFirst().orElseThrow();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(admin.getUsername(),
                null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        List<Map<String, Object>> report = new ArrayList<>();
        List<Long> imported = new ArrayList<>();
        for (String name : List.of("Abdulla Qodiriy", "Abdulhamid Cho'lpon", "Abdurauf Fitrat", "Alisher Navoiy",
                "Zahiriddin Muhammad Bobur")) {
            Author author = authors.findByFullNameIgnoreCase(name).orElseThrow();
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("author", name);
            WikisourceAuthorDto found = wikisource.works(SCHOOL, author.getId());
            row.put("page", found.pageUrl());
            row.put("worksListed", found.works().size());
            row.put("worksWithYearInLifetime", found.works().stream()
                    .filter(w -> w.year() != null && w.year() <= author.getDeathYear()).count());
            row.put("sample", found.works().stream().limit(12).map(w -> w.label() + (w.year() == null ? "" : " (" + w.year() + ")")).toList());
            List<Map<String, Object>> attempts = new ArrayList<>();
            int ok = 0;
            for (WikisourceWorkDto w : order(found.works(), PREFERRED.getOrDefault(name, List.of()))) {
                if (ok >= 2 || attempts.size() >= 6) break;
                Map<String, Object> a = new LinkedHashMap<>();
                a.put("work", w.label());
                a.put("year", w.year());
                if (w.imported()) {
                    Book b = books.findAll().stream().filter(x -> w.url().equals(x.getSourceUrl())).findFirst().orElseThrow();
                    a.put("result", "import qilingan (avvalgi urinishda)");
                    a.put("bookId", b.getId());
                    a.put("pages", b.getPages());
                    a.put("pdfBytes", b.getPdfSize());
                    imported.add(b.getId());
                    attempts.add(a);
                    ok++;
                    continue;
                }
                boolean yearInLifetime = w.year() != null && w.year() <= author.getDeathYear();
                boolean noYearAtAll = found.works().stream().allMatch(x -> x.year() == null);
                // with no year anywhere, one work is tried as if the admin confirmed "tirikligida chop etilgan"
                boolean adminConfirmed = !yearInLifetime && noYearAtAll
                        && attempts.stream().noneMatch(x -> Boolean.TRUE.equals(x.get("adminConfirmed")));
                if (!yearInLifetime && !adminConfirmed) {
                    a.put("result", "o'tkazildi: nashr yili noma'lum — «tirikligida chop etilgan»ni admin tasdiqlashi kerak");
                    attempts.add(a);
                    continue;
                }
                if (adminConfirmed) a.put("adminConfirmed", true);
                try {
                    BookDto b = wikisource.importWork(SCHOOL, new WikisourceImportRequest(author.getId(), w.title(), null,
                            true, null, true));
                    a.put("result", adminConfirmed ? "import qilindi (admin tasdig'i bilan, sinov)" : "import qilindi");
                    a.put("bookId", b.id());
                    a.put("pages", b.pages());
                    a.put("pdfBytes", b.pdfSize());
                    imported.add(b.id());
                    ok++;
                } catch (ResponseStatusException e) {
                    a.put("result", "import qilinmadi: " + e.getReason());
                }
                attempts.add(a);
            }
            row.put("attempts", attempts);
            report.add(row);
            System.out.println("WIKISOURCE " + new ObjectMapper().writeValueAsString(row));
        }
        String out = System.getenv("WIKISOURCE_REPORT");
        if (out != null) Files.writeString(Path.of(out), new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(report));
        String shots = System.getenv("WIKISOURCE_SHOTS");
        if (shots != null) render(imported, Path.of(shots));
        SecurityContextHolder.clearContext();
    }

    /** Preferred titles first, then works with a year in the author's lifetime, then the rest. */
    private static List<WikisourceWorkDto> order(List<WikisourceWorkDto> works, List<String> preferred) {
        List<WikisourceWorkDto> list = new ArrayList<>(works);
        list.sort(Comparator.comparingInt((WikisourceWorkDto w) -> preferred.contains(w.title()) ? 0 : 1)
                .thenComparingInt(w -> w.year() == null ? 1 : 0));
        return list;
    }

    /** The cover, the contents, page 5 and the middle page of each imported PDF, as PNG. */
    private void render(List<Long> ids, Path dir) throws Exception {
        Files.createDirectories(dir);
        int n = 0;
        for (Long id : ids) {
            Book b = books.findById(id).orElseThrow();
            n++;
            try (PDDocument d = Loader.loadPDF(storage.read(b.getPdfPath()))) {
                PDFRenderer r = new PDFRenderer(d);
                int last = d.getNumberOfPages() - 1;
                for (int page : new LinkedHashSet<>(List.of(0, 1, Math.min(4, last), last / 2))) {
                    BufferedImage img = r.renderImageWithDPI(page, 110, ImageType.RGB);
                    ImageIO.write(img, "png", dir.resolve("wikisource-" + n + "-p" + (page + 1) + ".png").toFile());
                }
            }
            System.out.println("WIKISOURCE PDF " + n + ": " + b.getTitle() + " — " + b.getPages() + " bet");
        }
    }
}
