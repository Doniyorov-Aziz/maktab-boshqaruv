package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.azizbek.maktabboshqaruv.entity.Achievement;
import uz.azizbek.maktabboshqaruv.service.AchievementService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService service;

    public AchievementController(AchievementService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<AchievementService.Response> list(@RequestParam Long schoolId,
                                                  @RequestParam(required = false) Long schoolClassId,
                                                  @RequestParam(required = false) Long studentId,
                                                  @RequestParam(required = false) Achievement.Category category,
                                                  @RequestParam(required = false) Achievement.Level level,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                                  @PageableDefault(size = 20, sort = {"achievedOn", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(schoolId, schoolClassId, studentId, category, level, from, to, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/student/{studentId}")
    public List<AchievementService.Response> ofStudent(@PathVariable Long studentId) {
        return service.ofStudent(studentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/stats")
    public AchievementService.Stats stats(@RequestParam Long schoolId,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.stats(schoolId, from, to);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<AchievementService.Response> create(@Valid @RequestBody AchievementService.Request r) {
        return ResponseEntity.status(201).body(service.create(r));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public AchievementService.Response update(@PathVariable Long id, @Valid @RequestBody AchievementService.Request r) {
        return service.update(id, r);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AchievementService.Response attach(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.attach(id, file);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}/file")
    public AchievementService.Response detach(@PathVariable Long id) {
        return service.detach(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return service.download(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{id}/notify")
    public Map<String, Integer> notifyParents(@PathVariable Long id) {
        return Map.of("queued", service.notifyParents(id));
    }
}
