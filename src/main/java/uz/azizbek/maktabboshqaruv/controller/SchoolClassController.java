package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.SchoolClassRequestDto;
import uz.azizbek.maktabboshqaruv.dto.SchoolClassResponseDto;
import uz.azizbek.maktabboshqaruv.service.SchoolClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/school-classes")
public class SchoolClassController {

    @Autowired
    private SchoolClassService schoolClassService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<SchoolClassResponseDto> getAllSchoolClasses(Pageable pageable) {
        return schoolClassService.getAllSchoolClasses(pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<SchoolClassResponseDto> getSchoolClassById(@PathVariable Long id) {
        return ResponseEntity.ok(schoolClassService.getSchoolClassById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<SchoolClassResponseDto> createSchoolClass(@Valid @RequestBody SchoolClassRequestDto request) {
        SchoolClassResponseDto created = schoolClassService.createSchoolClass(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<SchoolClassResponseDto> updateSchoolClass(@PathVariable Long id, @Valid @RequestBody SchoolClassRequestDto request) {
        SchoolClassResponseDto updated = schoolClassService.updateSchoolClass(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchoolClass(@PathVariable Long id) {
        schoolClassService.deleteSchoolClass(id);
        return ResponseEntity.ok("ID " + id + " bilan sinf muvaffaqiyatli o'chirildi");
    }
}
