package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import uz.azizbek.maktabboshqaruv.service.library.LibraryAdminService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Kutubxona — books and shelves (ADMIN, EDITOR). Book files are streamed only from here, after
 * the JWT and the school check: the PDF and the cover are never public links.
 */
@RestController
@RequestMapping("/api/library")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class LibraryController {

    private final LibraryAdminService library;

    public LibraryController(LibraryAdminService library) {
        this.library = library;
    }

    @GetMapping("/books")
    public PageDto<BookDto> list(@RequestParam Long schoolId,
                                 @RequestParam(required = false) String q,
                                 @RequestParam(required = false) Long categoryId,
                                 @RequestParam(required = false) String language,
                                 @RequestParam(required = false) AgeGroup ageGroup,
                                 @RequestParam(required = false) LegalBasis legalBasis,
                                 @RequestParam(required = false) String status,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "24") int size) {
        return library.list(schoolId, q, categoryId, language, ageGroup, legalBasis, status, page, size);
    }

    @GetMapping("/books/{id}")
    public BookDto get(@PathVariable Long id) {
        return library.get(id);
    }

    /** Same PDF already in this school? (the page checks the SHA-256 before uploading) */
    @GetMapping("/books/duplicates")
    public List<DuplicateDto> duplicates(@RequestParam Long schoolId, @RequestParam String sha256) {
        return library.duplicates(schoolId, sha256);
    }

    @PostMapping(value = "/books", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookDto create(@RequestParam Long schoolId,
                          @Valid @RequestPart("book") BookRequest book,
                          @RequestPart(value = "pdf", required = false) MultipartFile pdf,
                          @RequestPart(value = "cover", required = false) MultipartFile cover,
                          @RequestParam(defaultValue = "false") boolean allowDuplicate) {
        return library.create(schoolId, book, pdf, cover, allowDuplicate);
    }

    @PutMapping(value = "/books/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookDto update(@PathVariable Long id,
                          @Valid @RequestPart("book") BookRequest book,
                          @RequestPart(value = "pdf", required = false) MultipartFile pdf,
                          @RequestPart(value = "cover", required = false) MultipartFile cover,
                          @RequestParam(defaultValue = "false") boolean removeCover,
                          @RequestParam(defaultValue = "false") boolean allowDuplicate) {
        return library.update(id, book, pdf, cover, removeCover, allowDuplicate);
    }

    /** Bulk upload: one PDF → one draft card (shelf and legal basis chosen once for the batch). */
    @PostMapping(value = "/books/drafts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public BookDto draft(@RequestParam Long schoolId,
                         @RequestPart("pdf") MultipartFile pdf,
                         @RequestParam(required = false) Long categoryId,
                         @RequestParam LegalBasis legalBasis,
                         @RequestParam(required = false) String legalNote,
                         @RequestParam(required = false) String language,
                         @RequestParam(required = false) AgeGroup ageGroup) {
        return library.createDraft(schoolId, pdf, categoryId, legalBasis, legalNote, language, ageGroup);
    }

    /** Makes the book inactive (the download history stays). */
    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        library.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/books/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        LibraryAdminService.FileBody f = library.pdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename(f.fileName(), StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore())
                .body(f.bytes());
    }

    @GetMapping("/books/{id}/cover")
    public ResponseEntity<byte[]> cover(@PathVariable Long id) {
        LibraryAdminService.FileBody f = library.cover(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofMinutes(10)).cachePrivate())
                .body(f.bytes());
    }

    // ------------------------------------------------------------------ shelves

    @GetMapping("/categories")
    public List<CategoryDto> categories(@RequestParam Long schoolId) {
        return library.categories(schoolId);
    }

    @PostMapping("/categories")
    public CategoryDto createCategory(@RequestParam Long schoolId, @Valid @RequestBody CategoryRequest request) {
        return library.createCategory(schoolId, request);
    }

    @PutMapping("/categories/{id}")
    public CategoryDto updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return library.updateCategory(id, request);
    }

    /** Removed when empty; a shelf with books is hidden instead ({"deleted": false}). */
    @DeleteMapping("/categories/{id}")
    public Map<String, Boolean> deleteCategory(@PathVariable Long id) {
        return Map.of("deleted", library.deleteCategory(id));
    }

    @PutMapping("/categories/order")
    public List<CategoryDto> reorder(@RequestParam Long schoolId, @Valid @RequestBody ReorderRequest request) {
        return library.reorder(schoolId, request.ids());
    }
}
