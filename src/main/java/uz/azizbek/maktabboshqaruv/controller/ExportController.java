package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.service.pages.TableExportService;

import java.time.LocalDate;

/**
 * POST /api/export/table?format=xlsx|pdf — a list page's table (what it shows) as a file. Anyone
 * who may see the list may export it; the rows come from the page, nothing is read here.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final TableExportService export;

    public ExportController(TableExportService export) {
        this.export = export;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @PostMapping("/table")
    public ResponseEntity<byte[]> table(@RequestParam(defaultValue = "xlsx") String format,
                                        @RequestBody TableExportService.Table table) {
        if (table.columns() == null || table.columns().isEmpty() || table.rows() == null) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "Eksport uchun ustunlar va qatorlar kerak");
        }
        if (table.rows().size() > TableExportService.MAX_ROWS) {
            throw new IllegalStateException("Bir eksportda ko'pi bilan " + TableExportService.MAX_ROWS + " ta qator");
        }
        String name = (table.title() == null || table.title().isBlank() ? "eksport" : table.title().strip())
                .replaceAll("[^\\p{L}\\p{N}]+", "-").toLowerCase() + "-" + LocalDate.now();
        boolean pdf = "pdf".equalsIgnoreCase(format);
        byte[] body = pdf ? export.pdf(table) : export.xlsx(table);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pdf ? "application/pdf"
                        : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name + (pdf ? ".pdf" : ".xlsx"), java.nio.charset.StandardCharsets.UTF_8).build().toString())
                .body(body);
    }
}
