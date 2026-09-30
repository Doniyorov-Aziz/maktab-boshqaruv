package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.NotificationLogDto;
import uz.azizbek.maktabboshqaruv.dto.NotificationSettingsDto;
import uz.azizbek.maktabboshqaruv.dto.NotificationStatsDto;
import uz.azizbek.maktabboshqaruv.entity.NotificationStatus;
import uz.azizbek.maktabboshqaruv.entity.NotificationType;
import uz.azizbek.maktabboshqaruv.service.NotificationQueryService;
import uz.azizbek.maktabboshqaruv.service.NotificationSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationQueryService notificationQueryService;

    @Autowired
    private NotificationSettingsService notificationSettingsService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping
    public Page<NotificationLogDto> getNotifications(
            @RequestParam Long schoolId,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        return notificationQueryService.search(schoolId, type, status, from, to, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/stats")
    public NotificationStatsDto getStats(@RequestParam Long schoolId) {
        return notificationQueryService.stats(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{id}/retry")
    public NotificationLogDto retry(@PathVariable Long id) {
        return notificationQueryService.retry(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @GetMapping("/settings")
    public NotificationSettingsDto getSettings(@RequestParam Long schoolId) {
        return notificationSettingsService.getSettings(schoolId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/settings")
    public NotificationSettingsDto updateSettings(@RequestParam Long schoolId,
                                                  @Valid @RequestBody NotificationSettingsDto request) {
        return notificationSettingsService.updateSettings(schoolId, request);
    }
}
