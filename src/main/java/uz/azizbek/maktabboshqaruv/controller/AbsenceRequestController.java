package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.AbsenceRequestDto;
import uz.azizbek.maktabboshqaruv.dto.TextRequestDto;
import uz.azizbek.maktabboshqaruv.entity.AbsenceStatus;
import uz.azizbek.maktabboshqaruv.service.AbsenceRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;

/** "Sababli arizalar": requests from parents; approving marks the days EXCUSED. */
@RestController
@RequestMapping("/api/absence-requests")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
public class AbsenceRequestController {

    @Autowired
    private AbsenceRequestService absenceRequestService;

    @GetMapping
    public Page<AbsenceRequestDto> list(@RequestParam Long schoolId,
                                        @RequestParam(required = false) AbsenceStatus status,
                                        Pageable pageable) {
        return absenceRequestService.list(schoolId, status, pageable);
    }

    @GetMapping("/count-pending")
    public long countPending(@RequestParam Long schoolId) {
        return absenceRequestService.countPending(schoolId);
    }

    @PostMapping("/{id}/approve")
    public AbsenceRequestDto approve(@PathVariable Long id, Principal principal) {
        return absenceRequestService.approve(id, principal == null ? null : principal.getName());
    }

    @PostMapping("/{id}/reject")
    public AbsenceRequestDto reject(@PathVariable Long id, @Valid @RequestBody TextRequestDto request, Principal principal) {
        return absenceRequestService.reject(id, request.getText(), principal == null ? null : principal.getName());
    }
}
