package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.BehaviorRecordRequestDto;
import uz.azizbek.maktabboshqaruv.dto.BehaviorRecordResponseDto;
import uz.azizbek.maktabboshqaruv.service.BehaviorRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/behavior-records")
public class BehaviorRecordController {

    @Autowired
    private BehaviorRecordService behaviorRecordService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<BehaviorRecordResponseDto> getAllRecords(@RequestParam Long schoolId,
                                                         @RequestParam(required = false) Long schoolClassId,
                                                         Pageable pageable) {
        return behaviorRecordService.getAllRecords(schoolId, schoolClassId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<BehaviorRecordResponseDto> createRecord(@Valid @RequestBody BehaviorRecordRequestDto request) {
        BehaviorRecordResponseDto created = behaviorRecordService.createRecord(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<BehaviorRecordResponseDto> updateRecord(@PathVariable Long id, @Valid @RequestBody BehaviorRecordRequestDto request) {
        return ResponseEntity.ok(behaviorRecordService.updateRecord(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteRecord(@PathVariable Long id) {
        behaviorRecordService.deleteRecord(id);
        return ResponseEntity.ok("ID " + id + " bilan xulq yozuvi muvaffaqiyatli o'chirildi");
    }
}
