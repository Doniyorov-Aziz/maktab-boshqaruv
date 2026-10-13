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
import uz.azizbek.maktabboshqaruv.repository.BookRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.library.LibraryAuthors;
import uz.azizbek.maktabboshqaruv.service.library.LibraryPermissions;
import uz.azizbek.maktabboshqaruv.service.library.PermissionTemplatePdf;
import uz.azizbek.maktabboshqaruv.service.library.wikisource.WikisourceImport;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The library's legal side: the authors reference list (staff read it, admins edit it), the rights
 * holders' permissions with their signed scans and the letter template, and the Wikisource import
 * of public-domain classics — the last three for admins only.
 */
@RestController
@RequestMapping("/api/library")
public class LibraryLegalController {

    private final LibraryAuthors authors;
    private final LibraryPermissions permissions;
    private final PermissionTemplatePdf template;
    private final WikisourceImport wikisource;
    private final BookRepository books;
    private final SchoolRepository schools;
    private final SchoolAccessService access;

    public LibraryLegalController(LibraryAuthors authors, LibraryPermissions permissions, PermissionTemplatePdf template,
                                  WikisourceImport wikisource, BookRepository books, SchoolRepository schools,
                                  SchoolAccessService access) {
        this.authors = authors;
        this.permissions = permissions;
        this.template = template;
        this.wikisource = wikisource;
        this.books = books;
        this.schools = schools;
        this.access = access;
    }

    // ------------------------------------------------------------------ authors

    @GetMapping("/authors")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public List<AuthorDto> authors() {
        return authors.list();
    }

    @PostMapping("/authors")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorDto createAuthor(@Valid @RequestBody AuthorRequest request) {
        return authors.create(request);
    }

    @PutMapping("/authors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public AuthorDto updateAuthor(@PathVariable Long id, @Valid @RequestBody AuthorRequest request) {
        return authors.update(id, request);
    }

    @DeleteMapping("/authors/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAuthor(@PathVariable Long id) {
        authors.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ permissions

    @GetMapping("/permissions")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PermissionDto> permissions(@RequestParam Long schoolId, @RequestParam(required = false) String status) {
        return permissions.list(schoolId, status);
    }

    @PostMapping(value = "/permissions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public PermissionDto createPermission(@RequestParam Long schoolId,
                                          @Valid @RequestPart("permission") PermissionRequest request,
                                          @RequestPart(value = "document", required = false) MultipartFile document) {
        return permissions.create(schoolId, request, document);
    }

    @PutMapping(value = "/permissions/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public PermissionDto updatePermission(@PathVariable Long id,
                                          @Valid @RequestPart("permission") PermissionRequest request,
                                          @RequestPart(value = "document", required = false) MultipartFile document) {
        return permissions.update(id, request, document);
    }

    /** "Bekor qilish": its books leave the bot at once. */
    @PostMapping("/permissions/{id}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    public PermissionDto revoke(@PathVariable Long id) {
        return permissions.revoke(id);
    }

    @GetMapping("/permissions/{id}/document")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> document(@PathVariable Long id) {
        LibraryPermissions.Document d = permissions.document(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
                .header("X-Content-Type-Options", "nosniff").cacheControl(CacheControl.noStore()).body(d.bytes());
    }

    /** The letter to print and sign, with the school and the book titles filled in. */
    @GetMapping("/permissions/template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> template(@RequestParam Long schoolId,
                                           @RequestParam(required = false) List<Long> bookIds,
                                           @RequestParam(required = false) String title,
                                           @RequestParam(required = false) String grantor) {
        access.requireSchool(schoolId);
        String school = schools.findById(schoolId).map(s -> s.getName()).orElse(null);
        List<String> titles = new java.util.ArrayList<>();
        if (bookIds != null) {
            for (var b : books.findAllById(bookIds)) {
                if (b.getSchool().getId().equals(schoolId)) titles.add(b.getTitle());
            }
        }
        if (title != null && !title.isBlank()) titles.add(title.strip());
        byte[] pdf = template.render(school, titles, grantor);
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("ruxsat-xati-shablon.pdf", StandardCharsets.UTF_8).build().toString())
                .cacheControl(CacheControl.noStore()).body(pdf);
    }

    // ------------------------------------------------------------------ Wikisource

    @GetMapping("/wikisource/works")
    @PreAuthorize("hasRole('ADMIN')")
    public WikisourceAuthorDto wikisourceWorks(@RequestParam Long schoolId, @RequestParam Long authorId) {
        return wikisource.works(schoolId, authorId);
    }

    @PostMapping("/wikisource/import")
    @PreAuthorize("hasRole('ADMIN')")
    public BookDto wikisourceImport(@RequestParam Long schoolId, @Valid @RequestBody WikisourceImportRequest request) {
        return wikisource.importWork(schoolId, request);
    }
}
