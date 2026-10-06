package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.SubjectRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SubjectResponseDto;
import uz.azizbek.maktabboshqaruv.service.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {

    @Autowired
    private SubjectService subjectService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<SubjectResponseDto> getAllSubjects(@RequestParam Long schoolId,
                                                   @RequestParam(defaultValue = "false") boolean includeInactive,
                                                   Pageable pageable) {
        return subjectService.getAllSubjects(schoolId, includeInactive, pageable);
    }

    /** "Qayta faollashtirish". */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}/activate")
    public ResponseEntity<SubjectResponseDto> activate(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.activate(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<SubjectResponseDto> getSubjectById(@PathVariable Long id) {
        return ResponseEntity.ok(subjectService.getSubjectById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<SubjectResponseDto> createSubject(@Valid @RequestBody SubjectRequestDto request) {
        SubjectResponseDto created = subjectService.createSubject(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<SubjectResponseDto> updateSubject(@PathVariable Long id, @Valid @RequestBody SubjectRequestDto request) {
        SubjectResponseDto updated = subjectService.updateSubject(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSubject(@PathVariable Long id) {
        subjectService.deleteSubject(id);
        return ResponseEntity.ok("Fan nofaol qilindi — eski baholar va jadvalda nomi saqlanadi");
    }
}
