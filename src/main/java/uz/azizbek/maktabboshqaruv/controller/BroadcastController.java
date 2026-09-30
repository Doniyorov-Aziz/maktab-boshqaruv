package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.BroadcastDto;
import uz.azizbek.maktabboshqaruv.dto.BroadcastRequestDto;
import uz.azizbek.maktabboshqaruv.service.BroadcastService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.security.Principal;

/** "Ota-onalarga xabar" — messaging all parents of the school or of chosen classes. ADMIN only. */
@RestController
@RequestMapping("/api/broadcasts")
@PreAuthorize("hasRole('ADMIN')")
public class BroadcastController {

    @Autowired
    private BroadcastService broadcastService;

    @GetMapping
    public Page<BroadcastDto> list(@RequestParam Long schoolId, Pageable pageable) {
        return broadcastService.list(schoolId, pageable);
    }

    @PostMapping("/preview")
    public BroadcastDto preview(@Valid @RequestBody BroadcastRequestDto request) {
        return broadcastService.preview(request);
    }

    @PostMapping
    public ResponseEntity<BroadcastDto> send(@Valid @RequestBody BroadcastRequestDto request, Principal principal) {
        return ResponseEntity.status(201).body(broadcastService.send(request, principal == null ? null : principal.getName()));
    }
}
