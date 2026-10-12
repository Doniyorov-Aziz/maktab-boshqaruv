package uz.azizbek.maktabboshqaruv.service.library;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.*;
import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.entity.BookCategory;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.BookCategoryRepository;
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

/**
 * The site's library: books (with their PDF and cover) and shelves. Only the school's staff
 * upload books here — the only source the bot hands books out from. A book is never saved
 * without its legal basis. "Deleting" a book makes it inactive, so the download history stays.
 */
@Service
public class LibraryAdminService {

    private final BookRepository books;
    private final BookCategoryRepository categories;
    private final SchoolRepository schools;
    private final LibraryFiles files;
    private final LibraryCatalog catalog;
    private final LibraryDefaults defaults;
    private final BookMedia bookMedia;
    private final SchoolAccessService access;
    private final Clock clock;

    public LibraryAdminService(BookRepository books, BookCategoryRepository categories, SchoolRepository schools,
                               LibraryFiles files, LibraryCatalog catalog, LibraryDefaults defaults, BookMedia bookMedia,
                               SchoolAccessService access, Clock clock) {
        this.books = books;
        this.categories = categories;
        this.schools = schools;
        this.files = files;
        this.catalog = catalog;
        this.defaults = defaults;
        this.bookMedia = bookMedia;
        this.access = access;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ books

    @Transactional(readOnly = true)
    public PageDto<BookDto> list(Long schoolId, String q, Long categoryId, String language, AgeGroup ageGroup,
                                 LegalBasis legalBasis, String status, int page, int size) {
        access.requireSchool(schoolId);
        String key = LibraryText.normalize(q);
        String st = status == null || status.isBlank() ? "all" : status;
        if (!Set.of("all", "active", "inactive", "draft").contains(st)) st = "all";
        Page<Book> result = books.catalogue(schoolId, key.isEmpty() ? "" : "%" + key + "%", categoryId,
                language == null || language.isBlank() ? null : language, ageGroup, legalBasis, st,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Order.desc("draft"), Sort.Order.desc("createdAt"), Sort.Order.desc("id"))));
        return new PageDto<>(result.getContent().stream().map(LibraryAdminService::toDto).toList(),
                result.getTotalElements(), result.getNumber(), result.getSize());
    }

    @Transactional(readOnly = true)
    public BookDto get(Long id) {
        return toDto(owned(id));
    }

    @Transactional(readOnly = true)
    public List<DuplicateDto> duplicates(Long schoolId, String sha256) {
        access.requireSchool(schoolId);
        if (sha256 == null || !sha256.matches("^[0-9a-f]{64}$")) return List.of();
        return books.findBySchoolIdAndPdfSha256(schoolId, sha256).stream()
                .map(b -> new DuplicateDto(b.getId(), b.getTitle(), b.getAuthor(), Boolean.TRUE.equals(b.getActive())))
                .toList();
    }

    @Transactional
    public BookDto create(Long schoolId, BookRequest request, MultipartFile pdf, MultipartFile cover, boolean allowDuplicate) {
        access.requireSchool(schoolId);
        School school = schools.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maktab topilmadi"));
        requireLegalBasis(request);
        if (pdf == null || pdf.isEmpty()) throw LibraryFiles.bad("PDF fayl tanlanmagan");
        byte[] pdfBytes = bytes(pdf);
        LibraryFiles.PdfInfo info = files.checkPdf(pdfBytes);
        byte[] coverBytes = cover == null || cover.isEmpty() ? null : bytes(cover);
        String coverExt = coverBytes == null ? null : files.checkCover(coverBytes);
        if (!allowDuplicate) refuseDuplicate(schoolId, info.sha256(), null);

        Book b = new Book();
        b.setSchool(school);
        b.setDraft(false);
        b.setDownloadCount(0L);
        b.setLikes(0);
        b.setDislikes(0);
        b.setCreatedBy(access.caller().username());
        b.setCreatedAt(LocalDateTime.now(clock));
        apply(b, request, schoolId);
        if (b.getPages() == null) b.setPages(info.pages());
        storePdf(b, schoolId, pdfBytes, info);
        if (coverBytes != null) b.setCoverPath(files.saveCover(schoolId, coverExt, coverBytes));
        else drawCover(b, schoolId, pdfBytes);
        Book saved = books.save(b);
        catalog.evict(schoolId);
        return toDto(saved);
    }

    @Transactional
    public BookDto update(Long id, BookRequest request, MultipartFile pdf, MultipartFile cover, boolean removeCover,
                          boolean allowDuplicate) {
        Book b = owned(id);
        Long schoolId = b.getSchool().getId();
        requireLegalBasis(request);
        byte[] pdfBytes = pdf == null || pdf.isEmpty() ? null : bytes(pdf);
        LibraryFiles.PdfInfo info = pdfBytes == null ? null : files.checkPdf(pdfBytes);
        byte[] coverBytes = cover == null || cover.isEmpty() ? null : bytes(cover);
        String coverExt = coverBytes == null ? null : files.checkCover(coverBytes);
        if (info != null && !allowDuplicate) refuseDuplicate(schoolId, info.sha256(), b.getId());

        apply(b, request, schoolId);
        b.setDraft(false); // saving the full form is the review of a bulk-upload draft
        if (info != null) {
            String old = b.getPdfPath();
            storePdf(b, schoolId, pdfBytes, info);
            if (request.pages() == null) b.setPages(info.pages());
            b.setTelegramFileId(null); // the old file_id is the old file
            bookMedia.forget(b.getId());
            files.delete(old);
        }
        if (coverBytes != null) {
            files.delete(b.getCoverPath());
            b.setCoverPath(files.saveCover(schoolId, coverExt, coverBytes));
        } else if (removeCover) {
            files.delete(b.getCoverPath());
            b.setCoverPath(null);
            drawCover(b, schoolId, pdfBytes != null ? pdfBytes : files.read(b.getPdfPath()));
        }
        b.setUpdatedAt(LocalDateTime.now(clock));
        catalog.evict(schoolId);
        return toDto(books.save(b));
    }

    /**
     * Bulk upload: one PDF becomes a draft card titled after its file name, with the shelf and
     * legal basis chosen once for the whole batch. Drafts are invisible in the bot until saved.
     */
    @Transactional
    public BookDto createDraft(Long schoolId, MultipartFile pdf, Long categoryId, LegalBasis legalBasis, String legalNote,
                               String language, AgeGroup ageGroup) {
        String name = pdf == null ? null : pdf.getOriginalFilename();
        String title = titleFromFileName(name);
        BookDto created = create(schoolId, new BookRequest(title, null, categoryId, null,
                language == null || language.isBlank() ? "uz" : language, ageGroup, null, null, legalBasis, legalNote,
                false, false), pdf, null, true);
        Book b = books.findById(created.id()).orElseThrow();
        b.setDraft(true);
        b.setActive(false);
        return toDto(books.save(b));
    }

    /** "O'chirish": the book disappears from the bot, its download history stays. */
    @Transactional
    public void deactivate(Long id) {
        Book b = owned(id);
        b.setActive(false);
        b.setUpdatedAt(LocalDateTime.now(clock));
        books.save(b);
        catalog.evict(b.getSchool().getId());
    }

    public record FileBody(byte[] bytes, String contentType, String fileName) {
    }

    @Transactional(readOnly = true)
    public FileBody pdf(Long id) {
        Book b = owned(id);
        return new FileBody(files.read(b.getPdfPath()), "application/pdf", LibraryFiles.downloadName(b.getAuthor(), b.getTitle()));
    }

    @Transactional(readOnly = true)
    public FileBody cover(Long id) {
        Book b = owned(id);
        if (b.getCoverPath() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Muqova yo'q");
        String type = b.getCoverPath().endsWith(".png") ? "image/png" : "image/jpeg";
        return new FileBody(files.read(b.getCoverPath()), type, null);
    }

    // ------------------------------------------------------------------ shelves

    @Transactional
    public List<CategoryDto> categories(Long schoolId) {
        access.requireSchool(schoolId);
        defaults.ensureCategories(schoolId);
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : books.countsByCategory(schoolId)) counts.put((Long) row[0], (Long) row[1]);
        return categories.findBySchoolIdOrderByPositionAscIdAsc(schoolId).stream()
                .map(c -> toDto(c, counts.getOrDefault(c.getId(), 0L))).toList();
    }

    @Transactional
    public CategoryDto createCategory(Long schoolId, CategoryRequest request) {
        access.requireSchool(schoolId);
        School school = schools.findById(schoolId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Maktab topilmadi"));
        BookCategory c = new BookCategory();
        c.setSchool(school);
        c.setPosition(categories.maxPosition(schoolId) + 1);
        applyCategory(c, request);
        BookCategory saved = categories.save(c);
        catalog.evict(schoolId);
        return toDto(saved, 0);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        BookCategory c = ownedCategory(id);
        applyCategory(c, request);
        catalog.evict(c.getSchool().getId());
        return toDto(categories.save(c), books.countByCategoryId(id));
    }

    /** A shelf with books is hidden instead of removed (its books keep their shelf). */
    @Transactional
    public boolean deleteCategory(Long id) {
        BookCategory c = ownedCategory(id);
        Long schoolId = c.getSchool().getId();
        catalog.evict(schoolId);
        if (books.countByCategoryId(id) > 0) {
            c.setActive(false);
            categories.save(c);
            return false;
        }
        categories.delete(c);
        return true;
    }

    @Transactional
    public List<CategoryDto> reorder(Long schoolId, List<Long> ids) {
        access.requireSchool(schoolId);
        Map<Long, BookCategory> byId = new HashMap<>();
        for (BookCategory c : categories.findBySchoolIdOrderByPositionAscIdAsc(schoolId)) byId.put(c.getId(), c);
        int position = 0;
        for (Long id : ids) {
            BookCategory c = byId.remove(id);
            if (c != null) c.setPosition(++position);
        }
        for (BookCategory rest : byId.values()) rest.setPosition(++position); // not in the list: keep them last
        catalog.evict(schoolId);
        return categories(schoolId);
    }

    // ------------------------------------------------------------------ helpers

    /** The book, if the caller's school owns it (404 for an unknown id, 403 for another school's). */
    private Book owned(Long id) {
        Book b = books.findWithCategory(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Kitob topilmadi"));
        access.requireSchool(b.getSchool().getId());
        return b;
    }

    private BookCategory ownedCategory(Long id) {
        BookCategory c = categories.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bo'lim topilmadi"));
        access.requireSchool(c.getSchool().getId());
        return c;
    }

    private static void requireLegalBasis(BookRequest request) {
        if (request == null || request.legalBasis() == null) {
            throw LibraryFiles.bad("Huquqiy asos tanlanishi shart: faqat tarqatishga huquqingiz bor kitoblarni yuklang");
        }
        if (request.title() == null || request.title().isBlank()) throw LibraryFiles.bad("Kitob nomi kiritilishi shart");
    }

    private void apply(Book b, BookRequest r, Long schoolId) {
        b.setTitle(r.title().strip());
        b.setAuthor(blankToNull(r.author()));
        b.setCategory(r.categoryId() == null ? null : shelfOf(schoolId, r.categoryId()));
        b.setDescription(blankToNull(r.description()));
        b.setLanguage(r.language() == null || r.language().isBlank() ? "uz" : r.language());
        b.setAgeGroup(r.ageGroup() == null ? AgeGroup.KATTALAR : r.ageGroup());
        b.setPages(r.pages());
        b.setYear(r.year());
        b.setLegalBasis(r.legalBasis());
        b.setLegalNote(blankToNull(r.legalNote()));
        b.setActive(r.active() == null || r.active());
        b.setFeatured(Boolean.TRUE.equals(r.featured()));
        b.setSearchKey(LibraryText.key(b.getTitle(), b.getAuthor()));
    }

    private BookCategory shelfOf(Long schoolId, Long categoryId) {
        BookCategory c = categories.findById(categoryId)
                .orElseThrow(() -> LibraryFiles.bad("Bo'lim topilmadi"));
        if (!c.getSchool().getId().equals(schoolId)) throw LibraryFiles.bad("Bo'lim boshqa maktabga tegishli");
        return c;
    }

    private void storePdf(Book b, Long schoolId, byte[] bytes, LibraryFiles.PdfInfo info) {
        b.setPdfPath(files.savePdf(schoolId, bytes));
        b.setPdfSize(info.size());
        b.setPdfSha256(info.sha256());
    }

    private void drawCover(Book b, Long schoolId, byte[] pdfBytes) {
        byte[] drawn = files.coverFromPdf(pdfBytes);
        if (drawn != null) b.setCoverPath(files.saveCover(schoolId, "jpg", drawn));
    }

    private void refuseDuplicate(Long schoolId, String sha256, Long self) {
        for (Book other : books.findBySchoolIdAndPdfSha256(schoolId, sha256)) {
            if (!other.getId().equals(self)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Bu PDF allaqachon yuklangan: «" + other.getTitle() + "» (#" + other.getId() + ")");
            }
        }
    }

    private static void applyCategory(BookCategory c, CategoryRequest r) {
        c.setNameUz(r.nameUz().strip());
        c.setNameRu(blankToNull(r.nameRu()));
        c.setEmoji(blankToNull(r.emoji()));
        c.setActive(r.active() == null || r.active());
    }

    static String titleFromFileName(String name) {
        if (name == null || name.isBlank()) return "Nomsiz kitob";
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replaceAll("[_]+", " ").replaceAll("\\s+", " ").strip();
        if (base.length() > 255) base = base.substring(0, 255);
        return base.isEmpty() ? "Nomsiz kitob" : base;
    }

    private static byte[] bytes(MultipartFile f) {
        try {
            return f.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Faylni o'qib bo'lmadi", e);
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.strip();
    }

    static BookDto toDto(Book b) {
        BookCategory c = b.getCategory();
        return new BookDto(b.getId(), b.getTitle(), b.getAuthor(), c == null ? null : c.getId(),
                c == null ? null : c.getNameUz(), c == null ? null : c.getEmoji(), b.getDescription(), b.getLanguage(),
                b.getAgeGroup(), b.getPages(), b.getYear(), b.getCoverPath() != null, b.getPdfSize(), b.getPdfSha256(),
                b.getLegalBasis(), b.getLegalNote(), Boolean.TRUE.equals(b.getActive()), Boolean.TRUE.equals(b.getFeatured()),
                Boolean.TRUE.equals(b.getDraft()), b.getDownloadCount() == null ? 0 : b.getDownloadCount(),
                b.getLikes() == null ? 0 : b.getLikes(), b.getDislikes() == null ? 0 : b.getDislikes(),
                b.getCreatedBy(), b.getCreatedAt());
    }

    private static CategoryDto toDto(BookCategory c, long count) {
        return new CategoryDto(c.getId(), c.getNameUz(), c.getNameRu(), c.getEmoji(),
                c.getPosition() == null ? 0 : c.getPosition(), Boolean.TRUE.equals(c.getActive()), count);
    }
}
