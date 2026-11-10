package uz.azizbek.maktabboshqaruv.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uz.azizbek.maktabboshqaruv.dto.HomeworkDtos.Request;
import uz.azizbek.maktabboshqaruv.dto.HomeworkDtos.Response;
import uz.azizbek.maktabboshqaruv.service.HomeworkService;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/homework")
public class HomeworkController {

    private final HomeworkService service;

    public HomeworkController(HomeworkService service) {
        this.service = service;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<Response> list(@RequestParam Long schoolId,
                               @RequestParam(required = false) Long schoolClassId,
                               @RequestParam(required = false) Long subjectId,
                               @RequestParam(required = false) Long employeeId,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                               @PageableDefault(size = 20, sort = {"dueDate", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(schoolId, schoolClassId, subjectId, employeeId, from, to, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public Response get(@PathVariable Long id) {
        return service.get(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<Response> create(@Valid @RequestBody Request request) {
        return ResponseEntity.status(201).body(service.create(request));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public Response update(@PathVariable Long id, @Valid @RequestBody Request request) {
        return service.update(id, request);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping(value = "/{id}/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response attach(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return service.attach(id, file);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/{id}/file")
    public Response detach(@PathVariable Long id) {
        return service.detach(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        return service.download(id);
    }

    /** "Ota-onalarga yuborish": the bot message to the class's parents (once). */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/{id}/notify")
    public Map<String, Integer> notifyParents(@PathVariable Long id) {
        return Map.of("queued", service.notify(id));
    }
}
