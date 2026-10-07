package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.BroadcastDto;
import uz.azizbek.maktabboshqaruv.dto.BroadcastRecipientDto;
import uz.azizbek.maktabboshqaruv.dto.BroadcastRequestDto;
import uz.azizbek.maktabboshqaruv.service.BroadcastService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * "Ota-onalarga xabar" — messaging all parents of the school, chosen classes or chosen
 * parents, with photos / videos / files. ADMIN only (mass messaging is sensitive).
 */
@RestController
@RequestMapping("/api/broadcasts")
@PreAuthorize("hasRole('ADMIN')")
public class BroadcastController {

    @Autowired
    private BroadcastService broadcastService;

    @Autowired
    private SchoolAccessService access;

    @GetMapping
    public Page<BroadcastDto> list(@RequestParam Long schoolId,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                   @RequestParam(required = false) String q,
                                   Pageable pageable) {
        access.requireSchool(schoolId);
        return broadcastService.list(schoolId, from, to, q, pageable);
    }

    @GetMapping("/{id}")
    public BroadcastDto get(@PathVariable Long id) {
        access.requireSchool(broadcastService.schoolOf(id));
        return broadcastService.get(id);
    }

    /** Who it went to: name, class, status (SENT / PENDING / FAILED / SKIPPED), time. */
    @GetMapping("/{id}/recipients")
    public Page<BroadcastRecipientDto> recipients(@PathVariable Long id,
                                                  @RequestParam(required = false) String status,
                                                  @RequestParam(required = false) Long classId,
                                                  @RequestParam(required = false) String q,
                                                  Pageable pageable) {
        access.requireSchool(broadcastService.schoolOf(id));
        return broadcastService.recipients(id, status, classId, q, pageable);
    }

    @PostMapping("/preview")
    public BroadcastDto preview(@Valid @RequestBody BroadcastRequestDto request) {
        access.requireSchool(request.getSchoolId());
        return broadcastService.preview(request);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BroadcastDto> send(@Valid @RequestBody BroadcastRequestDto request, Principal principal) {
        access.requireSchool(request.getSchoolId());
        return ResponseEntity.status(201).body(broadcastService.send(request, principal == null ? null : principal.getName()));
    }

    /** With files: part "data" (the same JSON) + up to 10 parts "files". */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<BroadcastDto> sendWithFiles(@Valid @RequestPart("data") BroadcastRequestDto request,
                                                      @RequestPart(value = "files", required = false) List<MultipartFile> files,
                                                      Principal principal) {
        access.requireSchool(request.getSchoolId());
        return ResponseEntity.status(201).body(broadcastService.send(request, files, principal == null ? null : principal.getName()));
    }
}
