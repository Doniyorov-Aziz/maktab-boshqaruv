package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.service.WorkloadService;

import java.time.LocalDate;

/** O'qituvchilar ish yuklamasi (the "Eksport" button sends the table to /api/export, like every list page). */
@RestController
@RequestMapping("/api/workload")
public class WorkloadController {

    private final WorkloadService service;

    public WorkloadController(WorkloadService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public WorkloadService.Report report(@RequestParam Long schoolId,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.report(schoolId, from, to);
    }
}
