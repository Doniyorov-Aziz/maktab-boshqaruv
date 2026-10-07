package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.GradeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.GradeResponseDto;
import uz.azizbek.maktabboshqaruv.dto.GradebookResponseDto;
import uz.azizbek.maktabboshqaruv.service.GradeService;
import uz.azizbek.maktabboshqaruv.service.JournalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/grades")
public class GradeController {

    @Autowired
    private GradeService gradeService;

    @Autowired
    private JournalService journalService;

    @Autowired
    private java.time.Clock clock;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<GradeResponseDto> getAllGrades(@RequestParam Long schoolId, Pageable pageable) {
        return gradeService.getAllGrades(schoolId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/gradebook")
    public GradebookResponseDto getGradebook(@RequestParam Long schoolClassId,
                                              @RequestParam Long subjectId,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return gradeService.getGradebook(schoolClassId, subjectId, from, to);
    }

    /** The class journal: a month of one subject — days, students (by surname), grades, subject teacher. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/journal")
    public JournalService.Journal journal(@RequestParam Long classId, @RequestParam Long subjectId,
                                          @RequestParam(required = false) String month) {
        java.time.YearMonth ym = month == null || month.isBlank()
                ? java.time.YearMonth.now(clock) : java.time.YearMonth.parse(month);
        return journalService.journal(classId, subjectId, ym);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<GradeResponseDto> createGrade(@Valid @RequestBody GradeRequestDto request) {
        GradeResponseDto created = gradeService.createGrade(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<GradeResponseDto> updateGrade(@PathVariable Long id, @Valid @RequestBody GradeRequestDto request) {
        return ResponseEntity.ok(gradeService.updateGrade(id, request));
    }

    // teachers fix their own mistakes from the journal (with an undo), so EDITOR may delete too
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteGrade(@PathVariable Long id) {
        gradeService.deleteGrade(id);
        return ResponseEntity.ok("ID " + id + " bilan baho yozuvi muvaffaqiyatli o'chirildi");
    }
}
