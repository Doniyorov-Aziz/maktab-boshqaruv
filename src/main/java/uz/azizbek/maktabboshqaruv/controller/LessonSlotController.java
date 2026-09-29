package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.LessonSlotRequestDto;
import uz.azizbek.maktabboshqaruv.dto.LessonSlotResponseDto;
import uz.azizbek.maktabboshqaruv.dto.TimetableEntryDto;
import uz.azizbek.maktabboshqaruv.service.LessonSlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/lesson-slots")
public class LessonSlotController {

    @Autowired
    private LessonSlotService lessonSlotService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<LessonSlotResponseDto> getAllLessonSlots(@RequestParam Long schoolId, Pageable pageable) {
        return lessonSlotService.getAllLessonSlots(schoolId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/timetable")
    public List<TimetableEntryDto> getTimetable(@RequestParam Long schoolId,
                                                  @RequestParam(required = false) Long schoolClassId,
                                                  @RequestParam(required = false) Long employeeId,
                                                  @RequestParam(required = false) Long roomId) {
        return lessonSlotService.getTimetable(schoolId, schoolClassId, employeeId, roomId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/{id}")
    public ResponseEntity<LessonSlotResponseDto> getLessonSlotById(@PathVariable Long id) {
        return ResponseEntity.ok(lessonSlotService.getLessonSlotById(id));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<LessonSlotResponseDto> createLessonSlot(@Valid @RequestBody LessonSlotRequestDto request) {
        LessonSlotResponseDto created = lessonSlotService.createLessonSlot(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<LessonSlotResponseDto> updateLessonSlot(@PathVariable Long id, @Valid @RequestBody LessonSlotRequestDto request) {
        LessonSlotResponseDto updated = lessonSlotService.updateLessonSlot(id, request);
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteLessonSlot(@PathVariable Long id) {
        lessonSlotService.deleteLessonSlot(id);
        return ResponseEntity.ok("ID " + id + " bilan dars jadvali muvaffaqiyatli o'chirildi");
    }
}
