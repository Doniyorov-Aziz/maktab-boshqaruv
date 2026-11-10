package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.service.PaymentService;

import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/** To'lovlar (a private school's fees). Viewers do not see them; only an administrator removes a payment. */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    public record RemindRequest(Long schoolId, List<Long> studentIds) {
    }

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/settings")
    public PaymentService.SettingsDto settings(@RequestParam Long schoolId) {
        return service.settings(schoolId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/settings")
    public PaymentService.SettingsDto saveSettings(@RequestParam Long schoolId, @Valid @RequestBody PaymentService.SettingsRequest r) {
        return service.saveSettings(schoolId, r);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping
    public Page<PaymentService.PaymentDto> list(@RequestParam Long schoolId,
                                                @RequestParam(required = false) Long schoolClassId,
                                                @RequestParam(required = false) Long studentId,
                                                @RequestParam(required = false) YearMonth month,
                                                @PageableDefault(size = 20, sort = {"paidOn", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(schoolId, schoolClassId, studentId, month, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/summary")
    public PaymentService.Summary summary(@RequestParam Long schoolId, @RequestParam(required = false) Long schoolClassId,
                                          @RequestParam(required = false) YearMonth month) {
        return service.summary(schoolId, schoolClassId, month);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/student/{studentId}")
    public PaymentService.StudentAccount account(@PathVariable Long studentId) {
        return service.account(studentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<PaymentService.PaymentDto> create(@Valid @RequestBody PaymentService.PaymentRequest r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public PaymentService.PaymentDto update(@PathVariable Long id, @Valid @RequestBody PaymentService.PaymentRequest r) {
        return service.update(id, r);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/fee")
    public PaymentService.StudentAccount fee(@Valid @RequestBody PaymentService.FeeRequest r) {
        return service.setFee(r);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/remind")
    public Map<String, Integer> remind(@RequestBody RemindRequest r) {
        return Map.of("queued", service.remind(r.schoolId(), r.studentIds() == null || r.studentIds().isEmpty() ? null : r.studentIds()));
    }
}
