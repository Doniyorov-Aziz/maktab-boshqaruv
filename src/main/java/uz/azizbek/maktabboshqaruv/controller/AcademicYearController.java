package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.AcademicYearRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearResponseDto;
import uz.azizbek.maktabboshqaruv.service.AcademicYearService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/academic-years")
public class AcademicYearController {

    @Autowired
    private AcademicYearService academicYearService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{id}/current")
    public AcademicYearResponseDto makeCurrent(@PathVariable Long id) {
        return academicYearService.makeCurrent(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}/terms")
    public java.util.List<AcademicYearService.Term> terms(@PathVariable Long id) {
        return academicYearService.terms(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}/terms")
    public java.util.List<AcademicYearService.Term> saveTerms(@PathVariable Long id,
                                                              @RequestBody java.util.List<AcademicYearService.Term> terms) {
        return academicYearService.saveTerms(id, terms);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/next")
    public AcademicYearService.NextYear nextTemplate(@RequestParam Long schoolId) {
        return academicYearService.nextTemplate(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/next")
    public AcademicYearResponseDto createNext(@RequestParam Long schoolId, @RequestBody AcademicYearService.NextYear next) {
        return academicYearService.createNext(schoolId, next);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<AcademicYearResponseDto> getAllAcademicYears(@RequestParam Long schoolId,
                                                             @RequestParam(required = false) String q,
                                                             Pageable pageable) {
        return academicYearService.getAllAcademicYears(schoolId, q, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<AcademicYearResponseDto> getAcademicYearById(@PathVariable Long id) {
        return ResponseEntity.ok(academicYearService.getAcademicYearById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<AcademicYearResponseDto> createAcademicYear(@Valid @RequestBody AcademicYearRequestDto request) {
        AcademicYearResponseDto created = academicYearService.createAcademicYear(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<AcademicYearResponseDto> updateAcademicYear(@PathVariable Long id, @Valid @RequestBody AcademicYearRequestDto request) {
        AcademicYearResponseDto updated = academicYearService.updateAcademicYear(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAcademicYear(@PathVariable Long id) {
        academicYearService.deleteAcademicYear(id);
        return ResponseEntity.ok("ID " + id + " bilan o'quv yili muvaffaqiyatli o'chirildi");
    }
}
