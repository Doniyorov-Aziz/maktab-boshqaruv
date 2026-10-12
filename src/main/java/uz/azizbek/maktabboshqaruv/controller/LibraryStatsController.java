package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.dto.LibraryDtos.PageDto;
import uz.azizbek.maktabboshqaruv.service.library.LibraryStatsService;
import uz.azizbek.maktabboshqaruv.service.library.LibraryStatsService.*;

import java.time.LocalDate;

/**
 * Mutolaa statistikasi — ADMIN only: it shows which parent read what, with their phone.
 * EDITOR and VIEWER get 403. Every query is limited to the given school (school scope guard
 * + the service's own check).
 */
@RestController
@RequestMapping("/api/library/stats")
@PreAuthorize("hasRole('ADMIN')")
public class LibraryStatsController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final LibraryStatsService stats;

    public LibraryStatsController(LibraryStatsService stats) {
        this.stats = stats;
    }

    @GetMapping
    public Overview overview(@RequestParam Long schoolId) {
        return stats.overview(schoolId);
    }

    @GetMapping("/journal")
    public PageDto<JournalRow> journal(@RequestParam Long schoolId,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                       @RequestParam(required = false) Long classId,
                                       @RequestParam(required = false) Long categoryId,
                                       @RequestParam(required = false) Long bookId,
                                       @RequestParam(required = false) String q,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return stats.journal(schoolId, new JournalFilter(from, to, classId, categoryId, bookId, q), page, size);
    }

    /** The journal with the same filters as an Excel file. */
    @GetMapping("/journal/export")
    public ResponseEntity<byte[]> export(@RequestParam Long schoolId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                         @RequestParam(required = false) Long classId,
                                         @RequestParam(required = false) Long categoryId,
                                         @RequestParam(required = false) Long bookId,
                                         @RequestParam(required = false) String q) {
        byte[] file = stats.export(schoolId, new JournalFilter(from, to, classId, categoryId, bookId, q));
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("mutolaa-jurnali.xlsx").build().toString())
                .cacheControl(CacheControl.noStore())
                .body(file);
    }

    /** One parent: every book they downloaded, their favourite shelves and their children. */
    @GetMapping("/parents/{chatId}")
    public ParentDetail parent(@RequestParam Long schoolId, @PathVariable Long chatId) {
        return stats.parent(schoolId, chatId);
    }
}
