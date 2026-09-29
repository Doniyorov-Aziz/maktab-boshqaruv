package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.AnnouncementRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AnnouncementResponseDto;
import uz.azizbek.maktabboshqaruv.service.AnnouncementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    @Autowired
    private AnnouncementService announcementService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<AnnouncementResponseDto> getAllAnnouncements(@RequestParam Long schoolId, Pageable pageable) {
        return announcementService.getAllAnnouncements(schoolId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/latest")
    public List<AnnouncementResponseDto> getLatest(@RequestParam Long schoolId, @RequestParam(defaultValue = "5") int limit) {
        return announcementService.getLatest(schoolId, limit);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> getAnnouncementById(@PathVariable Long id) {
        return ResponseEntity.ok(announcementService.getAnnouncementById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<AnnouncementResponseDto> createAnnouncement(@Valid @RequestBody AnnouncementRequestDto request) {
        AnnouncementResponseDto created = announcementService.createAnnouncement(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponseDto> updateAnnouncement(@PathVariable Long id, @Valid @RequestBody AnnouncementRequestDto request) {
        return ResponseEntity.ok(announcementService.updateAnnouncement(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteAnnouncement(@PathVariable Long id) {
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok("ID " + id + " bilan e'lon muvaffaqiyatli o'chirildi");
    }
}
