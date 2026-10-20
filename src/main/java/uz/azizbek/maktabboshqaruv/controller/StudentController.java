package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.StudentRequestDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private uz.azizbek.maktabboshqaruv.service.pages.ListStatsService listStats;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<StudentResponseDto> getAllStudents(@RequestParam Long schoolId,
                                                   @RequestParam(required = false) Long schoolClassId,
                                                   @RequestParam(required = false) String q,
                                                   @RequestParam(defaultValue = "false") boolean atRisk,
                                                   @RequestParam(required = false) Boolean telegram,
                                                   @RequestParam(defaultValue = "false") boolean birthday,
                                                   Pageable pageable) {
        return studentService.getAllStudents(schoolId, schoolClassId, q, atRisk, telegram, birthday, pageable);
    }

    /** The filter chips' numbers: students per class and per Telegram link. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/facets")
    public java.util.Map<String, java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Facet>> facets(
            @RequestParam Long schoolId) {
        return listStats.studentFacets(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<StudentResponseDto> getStudentById(@PathVariable Long id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<StudentResponseDto> createStudent(@Valid @RequestBody StudentRequestDto request) {
        StudentResponseDto created = studentService.createStudent(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<StudentResponseDto> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentRequestDto request) {
        StudentResponseDto updated = studentService.updateStudent(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteStudent(@PathVariable Long id) {
        studentService.deleteStudent(id);
        return ResponseEntity.ok("ID " + id + " bilan o'quvchi muvaffaqiyatli o'chirildi");
    }
}
