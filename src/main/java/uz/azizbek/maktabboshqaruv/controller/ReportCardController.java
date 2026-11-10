package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import uz.azizbek.maktabboshqaruv.service.ReportCardMedia;
import uz.azizbek.maktabboshqaruv.service.ReportCardPdf;
import uz.azizbek.maktabboshqaruv.service.ReportCardService;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Chorak / yillik tabel of a class. */
@RestController
@RequestMapping("/api/report-cards")
public class ReportCardController {

    public record CellRequest(Long classId, Long termId, Long studentId, Long subjectId, Integer score) {
    }

    public record FinalizeRequest(Long classId, Long termId, Boolean overwrite) {
    }

    private final ReportCardService cards;
    private final ReportCardPdf pdf;
    private final ReportCardMedia sender;
    private final StudentRepository students;

    public ReportCardController(ReportCardService cards, ReportCardPdf pdf, ReportCardMedia sender, StudentRepository students) {
        this.cards = cards;
        this.pdf = pdf;
        this.sender = sender;
        this.students = students;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/terms")
    public List<ReportCardService.Term> terms(@RequestParam Long classId) {
        return cards.terms(classId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public ReportCardService.Card card(@RequestParam Long classId, @RequestParam(required = false) Long termId) {
        return cards.card(classId, termId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/student")
    public ReportCardService.StudentYear student(@RequestParam Long studentId, @RequestParam(required = false) Long termId) {
        return cards.studentYear(studentId, termId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/finalize")
    public Map<String, Integer> finalizeQuarter(@RequestBody FinalizeRequest r) {
        return Map.of("saved", cards.finalizeQuarter(r.classId(), r.termId(), Boolean.TRUE.equals(r.overwrite())));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/cell")
    public ReportCardService.Card cell(@RequestBody CellRequest r) {
        return cards.setCell(r.classId(), r.termId(), r.studentId(), r.subjectId(), r.score());
    }

    /** The printed tabel: every pupil of the class (one page each), or one pupil (studentId). */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> pdf(@RequestParam Long classId, @RequestParam(required = false) Long termId,
                                      @RequestParam(required = false) Long studentId) {
        List<Long> ids = studentId != null ? List.of(studentId)
                : students.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId).stream().map(s -> s.getId()).toList();
        byte[] bytes = pdf.render(ids.stream()
                .map(id -> cards.studentYear(id, termId))
                .filter(y -> studentId == null || y != null).toList());
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename("tabel.pdf", StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/send")
    public Map<String, Integer> send(@RequestBody FinalizeRequest r) {
        return Map.of("queued", sender.send(r.classId(), r.termId()));
    }
}
