package uz.azizbek.maktabboshqaruv.service.library.wikisource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.*;
import uz.azizbek.maktabboshqaruv.entity.Author;
import uz.azizbek.maktabboshqaruv.entity.BookCategory;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.repository.BookCategoryRepository;
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.library.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * "📜 Wikisource dan qo'shish" — Uzbek classics whose authors' works are in the public domain:
 * the author's page on Wikisource (wikisource.org, which hosts Uzbek, and ru.wikisource.org), the
 * list of works, and importing one: its text (with its chapters) is fetched, cleaned and laid out
 * as a PDF once (WikisourcePdf), then saved as an ordinary book — JAMOAT_MULKI with the page as the
 * legal note — through the same checks as any upload. A page that is unfinished, too short or has
 * missing chapters is not imported, and the reason is shown.
 */
@Service
public class WikisourceImport {

    private static final Logger log = LoggerFactory.getLogger(WikisourceImport.class);

    /** where to look, in order: host, author namespace id, author namespace prefix, book language */
    record Site(String host, int authorNs, String prefix, String language) {
    }

    static final List<Site> SITES = List.of(
            new Site("wikisource.org", 108, "Author:", "uz"),
            new Site("ru.wikisource.org", 102, "Автор:", "ru"));

    static final int MIN_TEXT_CHARS = 1000;
    static final int MAX_CHAPTERS = 150;

    private final WikisourceApi api;
    private final WikisourcePdf pdf;
    private final LibraryAuthors authors;
    private final LibraryLegal legal;
    private final LibraryAdminService admin;
    private final BookRepository books;
    private final BookCategoryRepository categories;
    private final SchoolRepository schools;
    private final SchoolAccessService access;

    public WikisourceImport(WikisourceApi api, WikisourcePdf pdf, LibraryAuthors authors, LibraryLegal legal,
                            LibraryAdminService admin, BookRepository books, BookCategoryRepository categories,
                            SchoolRepository schools, SchoolAccessService access) {
        this.api = api;
        this.pdf = pdf;
        this.authors = authors;
        this.legal = legal;
        this.admin = admin;
        this.books = books;
        this.categories = categories;
        this.schools = schools;
        this.access = access;
    }

    /** The same service talking to another API (tests use a fake Wikisource). */
    public WikisourceImport withApi(WikisourceApi other) {
        return new WikisourceImport(other, pdf, authors, legal, admin, books, categories, schools, access);
    }

    // ------------------------------------------------------------------ browsing

    public record AuthorPage(Site site, String title, String wikitext) {
    }

    /** The author's works on Wikisource (only for authors whose works are in the public domain). */
    public WikisourceAuthorDto works(Long schoolId, Long authorId) {
        access.requireSchool(schoolId);
        Author author = authors.require(authorId);
        requireFree(author);
        AuthorPage page = findAuthorPage(author);
        if (page == null) {
            return new WikisourceAuthorDto(author.getId(), author.getFullName(), null, null, author.getBirthYear(),
                    author.getDeathYear(), List.of(), "Wikisource'da bu muallif sahifasi topilmadi");
        }
        Set<String> imported = new HashSet<>(books.sourceUrls(schoolId));
        List<WikisourceWorkDto> works = new ArrayList<>();
        for (WikiText.WorkLink w : WikiText.works(page.wikitext())) {
            String url = url(page.site(), w.title());
            works.add(new WikisourceWorkDto(w.title(), w.label(), w.year(), w.section(), url, imported.contains(url)));
        }
        return new WikisourceAuthorDto(author.getId(), author.getFullName(), page.title(), url(page.site(), page.title()),
                author.getBirthYear(), author.getDeathYear(), works,
                works.isEmpty() ? "Muallif sahifasida asarlar ro'yxati yo'q" : null);
    }

    /** The author's page: searched by the name and every alias on each site, the best match first. */
    AuthorPage findAuthorPage(Author author) {
        for (Site site : SITES) {
            for (String variant : LibraryAuthors.variants(author)) {
                String wanted = LibraryText.normalize(variant);
                if (wanted.isEmpty()) continue;
                for (String title : api.search(site.host(), variant, site.authorNs(), 5)) {
                    String name = title.startsWith(site.prefix()) ? title.substring(site.prefix().length()) : title;
                    if (LibraryText.normalize(name).equals(wanted)) {
                        String text = api.wikitext(site.host(), title);
                        if (text != null) return new AuthorPage(site, title, text);
                    }
                }
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ importing

    /** Imports one work; a 400 with the reason when it cannot be (unfinished, too short, protected, …). */
    public BookDto importWork(Long schoolId, WikisourceImportRequest r) {
        access.requireSchool(schoolId);
        Author author = authors.require(r.authorId());
        requireFree(author);
        // the legal confirmations first — nothing is fetched for a work that may not be added
        legal.checkPublicDomain(List.of(author), r.publishedInLifetime(), r.firstPublishedYear(), r.noModernCommentary());

        AuthorPage page = findAuthorPage(author);
        Site site = page != null ? page.site() : SITES.get(0);
        String url = url(site, r.title());
        if (books.existsBySchoolIdAndSourceUrl(schoolId, url)) {
            throw LibraryFiles.bad("Bu asar allaqachon import qilingan");
        }
        String main = api.wikitext(site.host(), r.title());
        if (main == null) throw LibraryFiles.bad("Wikisource'da «" + r.title() + "» sahifasi topilmadi");
        if (WikiText.unfinished(main)) throw LibraryFiles.bad("Sahifa hali to'liq emas (tayyorlanmoqda) — import qilinmadi");

        List<WikisourcePdf.Chapter> chapters = new ArrayList<>();
        List<WikiText.ChapterLink> links = WikiText.chapters(r.title(), main);
        if (links.size() > MAX_CHAPTERS) throw LibraryFiles.bad("Asar juda katta (" + links.size() + " bob) — qo'lda yuklang");
        if (links.isEmpty()) {
            chapters.add(new WikisourcePdf.Chapter(displayTitle(r.title()), WikiText.clean(main)));
        } else {
            String intro = WikiText.clean(stripChapterList(main, r.title()));
            if (intro.length() > 200) chapters.add(new WikisourcePdf.Chapter("Kirish", intro));
            int missing = 0;
            for (WikiText.ChapterLink link : links) {
                String text = api.wikitext(site.host(), link.title());
                if (text == null) {
                    missing++;
                    continue;
                }
                if (WikiText.unfinished(text)) {
                    throw LibraryFiles.bad("«" + link.heading() + "» bobi hali to'liq emas (tayyorlanmoqda) — import qilinmadi");
                }
                chapters.add(new WikisourcePdf.Chapter(link.heading(), WikiText.clean(text)));
            }
            if (missing > 0) {
                throw LibraryFiles.bad(missing + " ta bob sahifasi Wikisource'da yo'q — asar to'liq emas, import qilinmadi");
            }
        }
        int chars = chapters.stream().mapToInt(c -> c.text().length()).sum();
        if (chars < MIN_TEXT_CHARS) {
            throw LibraryFiles.bad("Matn juda qisqa (" + chars + " belgi) — sahifa to'liq emas, import qilinmadi");
        }

        String title = displayTitle(r.title());
        String school = schools.findById(schoolId).map(s -> s.getName()).orElse(null);
        byte[] bytes = pdf.render(new WikisourcePdf.BookInfo(title, author.getFullName(), author.lifeYears(), url, school),
                chapters);
        Long categoryId = r.categoryId() != null ? r.categoryId() : classicsShelf(schoolId);
        BookRequest request = new BookRequest(title, null, List.of(author.getId()), categoryId,
                "Wikisource'dan olingan to'liq matn (" + chapters.size() + " qism). Jamoat mulki.",
                site.language(), AgeGroup.KATTALAR, null, null, LegalBasis.JAMOAT_MULKI, "Wikisource: " + url, null,
                r.publishedInLifetime(), r.firstPublishedYear(), r.noModernCommentary(), true, false);
        BookDto saved = admin.createFromBytes(schoolId, request, bytes, null, false, url);
        log.info("Wikisource import: «{}» ({}) — {} qism, {} belgi, {} bayt PDF", title, author.getFullName(),
                chapters.size(), chars, bytes.length);
        return saved;
    }

    private void requireFree(Author author) {
        CopyrightRules.Status s = authors.status(author);
        if (!s.free()) {
            throw LibraryFiles.bad(s.year() == null
                    ? "«" + author.getFullName() + "» muallifining vafot yili kiritilmagan — Wikisource importi faqat jamoat mulki bo'lgan mualliflar uchun"
                    : "Bu muallif asarlari " + s.year() + "-yilgacha mualliflik huquqi bilan himoyalangan. "
                    + "Muallif yoki merosxo'rlar ruxsati kerak.");
        }
    }

    /** "O'tkan kunlar", without a "(Abdulla Qodiriy)"-style disambiguation. */
    static String displayTitle(String pageTitle) {
        String t = pageTitle.replace('_', ' ').strip();
        return t.replaceFirst("\\s*\\([^)]*\\)$", "").strip();
    }

    /** The work page without its list of chapter links (what is left is a foreword of the author, if any). */
    private static String stripChapterList(String wikitext, String title) {
        StringBuilder sb = new StringBuilder();
        for (String line : wikitext.split("\n")) {
            if (line.contains("[[" + title + "/") || line.contains("[[/")) continue;
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private Long classicsShelf(Long schoolId) {
        String wanted = LibraryText.normalize("O'zbek klassikasi");
        return categories.findBySchoolIdOrderByPositionAscIdAsc(schoolId).stream()
                .filter(c -> LibraryText.normalize(c.getNameUz()).equals(wanted))
                .map(BookCategory::getId).findFirst().orElse(null);
    }

    static String url(Site site, String title) {
        return "https://" + site.host() + "/wiki/" + URLEncoder.encode(title.replace(' ', '_'), StandardCharsets.UTF_8)
                .replace("+", "_").replace("%2F", "/").replace("%3A", ":");
    }
}
