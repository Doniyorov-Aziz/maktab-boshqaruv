package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.service.ExamService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    private final ExamService service;

    public ExamController(ExamService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<ExamService.Response> list(@RequestParam Long schoolId,
                                           @RequestParam(required = false) Long schoolClassId,
                                           @RequestParam(required = false) Long subjectId,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                           @PageableDefault(size = 50, sort = {"examDate", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(schoolId, schoolClassId, subjectId, from, to, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ExamService.Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<ExamService.Response> create(@Valid @RequestBody ExamService.Request request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ExamService.Response update(@PathVariable Long id, @Valid @RequestBody ExamService.Request request) {
        return service.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}/results")
    public ExamService.Results results(@PathVariable Long id) {
        return service.results(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}/results")
    public ExamService.Results saveResults(@PathVariable Long id, @RequestBody List<ExamService.ResultInput> results) {
        return service.saveResults(id, results);
    }
}
