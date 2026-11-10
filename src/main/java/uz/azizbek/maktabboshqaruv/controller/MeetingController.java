package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.service.MeetingService;

import java.time.LocalDate;
import java.util.List;

/** Ota-onalar bilan uchrashuvlar: the teachers' free times and the parents' bookings. */
@RestController
@RequestMapping("/api/meeting-slots")
public class MeetingController {

    private final MeetingService service;

    public MeetingController(MeetingService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public List<MeetingService.SlotDto> list(@RequestParam Long schoolId,
                                             @RequestParam(required = false) Long employeeId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.list(schoolId, employeeId, from, to);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/bulk")
    public ResponseEntity<List<MeetingService.SlotDto>> bulk(@Valid @RequestBody MeetingService.BulkRequest request) {
        return ResponseEntity.status(201).body(service.createBulk(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{id}/cancel")
    public MeetingService.SlotDto cancel(@PathVariable Long id) {
        return service.cancelBooking(id);
    }
}
