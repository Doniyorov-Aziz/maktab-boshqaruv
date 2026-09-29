package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.CalendarEventRequestDto;
import uz.azizbek.maktabboshqaruv.dto.CalendarEventResponseDto;
import uz.azizbek.maktabboshqaruv.service.CalendarEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar-events")
public class CalendarEventController {

    @Autowired
    private CalendarEventService calendarEventService;

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping
    public Page<CalendarEventResponseDto> getAllEvents(@RequestParam Long schoolId, Pageable pageable) {
        return calendarEventService.getAllEvents(schoolId, pageable);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/range")
    public List<CalendarEventResponseDto> getInRange(@RequestParam Long schoolId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return calendarEventService.getInRange(schoolId, from, to);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/upcoming")
    public List<CalendarEventResponseDto> getUpcoming(@RequestParam Long schoolId,
                                                        @RequestParam(defaultValue = "5") int limit) {
        return calendarEventService.getUpcoming(schoolId, LocalDate.now(), limit);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping
    public ResponseEntity<CalendarEventResponseDto> createEvent(@Valid @RequestBody CalendarEventRequestDto request) {
        CalendarEventResponseDto created = calendarEventService.createEvent(request);
        return ResponseEntity.status(201).body(created);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PutMapping("/{id}")
    public ResponseEntity<CalendarEventResponseDto> updateEvent(@PathVariable Long id, @Valid @RequestBody CalendarEventRequestDto request) {
        return ResponseEntity.ok(calendarEventService.updateEvent(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteEvent(@PathVariable Long id) {
        calendarEventService.deleteEvent(id);
        return ResponseEntity.ok("ID " + id + " bilan tadbir muvaffaqiyatli o'chirildi");
    }
}
