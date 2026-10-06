package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.CalendarEventRequestDto;
import uz.azizbek.maktabboshqaruv.dto.CalendarEventResponseDto;
import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CalendarEventService {

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolRepository schoolRepository;

    public Page<CalendarEventResponseDto> getAllEvents(Long schoolId, Pageable pageable) {
        return calendarEventRepository.findBySchoolId(schoolId, pageable).map(this::toResponseDto);
    }

    public List<CalendarEventResponseDto> getInRange(Long schoolId, LocalDate from, LocalDate to) {
        return calendarEventRepository.findInRange(schoolId, from, to).stream()
                .map(this::toResponseDto).collect(Collectors.toList());
    }

    public List<CalendarEventResponseDto> getUpcoming(Long schoolId, LocalDate from, int limit) {
        return calendarEventRepository.findUpcoming(schoolId, from, PageRequest.of(0, limit)).stream()
                .map(this::toResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public CalendarEventResponseDto createEvent(CalendarEventRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        validateDateRange(request);

        CalendarEvent event = new CalendarEvent();
        event.setSchool(school);
        applyFields(event, request);

        CalendarEvent saved = calendarEventRepository.save(event);
        botCache.evictEvents();
        return toResponseDto(saved);
    }

    @Transactional
    public CalendarEventResponseDto updateEvent(Long id, CalendarEventRequestDto request) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday tadbir topilmadi: " + id));
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        validateDateRange(request);

        event.setSchool(school);
        applyFields(event, request);

        CalendarEvent updated = calendarEventRepository.save(event);
        botCache.evictEvents();
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteEvent(Long id) {
        if (!calendarEventRepository.existsById(id)) {
            throw new IllegalStateException("Bunday tadbir topilmadi: " + id);
        }
        calendarEventRepository.deleteById(id);
        botCache.evictEvents();
    }

    private void validateDateRange(CalendarEventRequestDto request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalStateException("Tugash sanasi boshlanish sanasidan oldin bo'lishi mumkin emas");
        }
    }

    private void applyFields(CalendarEvent event, CalendarEventRequestDto request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setType(request.getType());
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());
    }

    private CalendarEventResponseDto toResponseDto(CalendarEvent event) {
        CalendarEventResponseDto dto = new CalendarEventResponseDto();
        dto.setId(event.getId());
        dto.setSchoolId(event.getSchool().getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setType(event.getType());
        dto.setStartDate(event.getStartDate());
        dto.setEndDate(event.getEndDate());
        return dto;
    }
}
