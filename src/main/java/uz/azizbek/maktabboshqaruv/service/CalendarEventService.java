package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.CalendarEventRequestDto;
import uz.azizbek.maktabboshqaruv.dto.CalendarEventResponseDto;
import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * School calendar events. Besides title, type and dates an event may have a time, a room (booked
 * automatically — a busy room is a 409, and moving the event re-checks it), a place, an audience
 * (classes, else the whole school), "tell the parents" (Telegram when created and the day before)
 * and, when created, a repetition (every week / month up to a date: one event per occurrence,
 * sharing a series id).
 */
@Service
public class CalendarEventService {

    /** A series stops here even if "until" is later. */
    static final int MAX_OCCURRENCES = 60;

    @Autowired
    private CalendarEventRepository calendarEventRepository;

    @Autowired
    private BotCache botCache;

    @Autowired
    private SchoolRepository schoolRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SchoolClassRepository schoolClassRepository;

    @Autowired
    private RoomBookingService roomBookings;

    @Autowired
    private EventNotifier eventNotifier;

    /** The calendar page's numbers are cached per school. */
    @Autowired
    private uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache pageCache;

    @Transactional(readOnly = true)
    public Page<CalendarEventResponseDto> getAllEvents(Long schoolId, Pageable pageable) {
        return calendarEventRepository.findBySchoolId(schoolId, pageable).map(this::toResponseDto);
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponseDto> getInRange(Long schoolId, LocalDate from, LocalDate to) {
        return calendarEventRepository.findInRange(schoolId, from, to).stream()
                .map(this::toResponseDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CalendarEventResponseDto> getUpcoming(Long schoolId, LocalDate from, int limit) {
        return calendarEventRepository.findUpcoming(schoolId, from, PageRequest.of(0, limit)).stream()
                .map(this::toResponseDto).collect(Collectors.toList());
    }

    @Transactional
    public CalendarEventResponseDto createEvent(CalendarEventRequestDto request) {
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));
        validate(request);

        List<LocalDate[]> occurrences = occurrences(request);
        String seriesId = occurrences.size() > 1 ? UUID.randomUUID().toString() : null;
        CalendarEvent first = null;
        for (LocalDate[] dates : occurrences) {
            CalendarEvent event = new CalendarEvent();
            event.setSchool(school);
            applyFields(event, request);
            event.setStartDate(dates[0]);
            event.setEndDate(dates[1]);
            event.setSeriesId(seriesId);
            CalendarEvent saved = calendarEventRepository.save(event);
            roomBookings.bookForEvent(saved);
            if (first == null) first = saved;
        }
        botCache.evictEvents();
        pageCache.evictSchool(school.getId());
        // one message for a whole series: about its first date
        eventNotifier.notifyCreated(first);
        return toResponseDto(first);
    }

    @Transactional
    public CalendarEventResponseDto updateEvent(Long id, CalendarEventRequestDto request) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday tadbir topilmadi: " + id));
        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday maktab mavjud emas"));
        validate(request);

        event.setSchool(school);
        applyFields(event, request);
        event.setStartDate(request.getStartDate());
        event.setEndDate(request.getEndDate());

        CalendarEvent updated = calendarEventRepository.save(event);
        // moved or its room / time changed: the bookings follow, checked again
        roomBookings.bookForEvent(updated);
        botCache.evictEvents();
        pageCache.evictSchool(school.getId());
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteEvent(Long id) {
        CalendarEvent event = calendarEventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday tadbir topilmadi: " + id));
        roomBookings.dropForEvent(id);
        calendarEventRepository.delete(event);
        botCache.evictEvents();
        pageCache.evictSchool(event.getSchool().getId());
    }

    private void validate(CalendarEventRequestDto request) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new IllegalStateException("Tugash sanasi boshlanish sanasidan oldin bo'lishi mumkin emas");
        }
        if ((request.getStartTime() == null) != (request.getEndTime() == null)) {
            throw new IllegalStateException("Boshlanish va tugash vaqtini birga kiriting (yoki ikkalasini ham bo'sh qoldiring)");
        }
        if (request.getStartTime() != null && !request.getStartTime().isBefore(request.getEndTime())) {
            throw new IllegalStateException("Boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak");
        }
    }

    /** [start, end] of each occurrence: one, or every week / month up to recurrenceUntil. */
    static List<LocalDate[]> occurrences(CalendarEventRequestDto r) {
        List<LocalDate[]> out = new ArrayList<>();
        out.add(new LocalDate[]{r.getStartDate(), r.getEndDate()});
        String rec = r.getRecurrence() == null ? "NONE" : r.getRecurrence().toUpperCase();
        if (rec.equals("NONE")) return out;
        if (!rec.equals("WEEKLY") && !rec.equals("MONTHLY")) {
            throw new IllegalStateException("Takrorlanish: NONE, WEEKLY yoki MONTHLY");
        }
        if (r.getRecurrenceUntil() == null || r.getRecurrenceUntil().isBefore(r.getStartDate())) {
            throw new IllegalStateException("Takrorlanish qachongacha ekanini kiriting");
        }
        long length = ChronoUnit.DAYS.between(r.getStartDate(), r.getEndDate());
        for (int k = 1; out.size() < MAX_OCCURRENCES; k++) {
            LocalDate start = rec.equals("WEEKLY") ? r.getStartDate().plusWeeks(k) : r.getStartDate().plusMonths(k);
            if (start.isAfter(r.getRecurrenceUntil())) break;
            out.add(new LocalDate[]{start, start.plusDays(length)});
        }
        return out;
    }

    private void applyFields(CalendarEvent event, CalendarEventRequestDto request) {
        event.setTitle(request.getTitle());
        event.setDescription(request.getDescription());
        event.setType(request.getType());
        event.setStartTime(request.getStartTime());
        event.setEndTime(request.getEndTime());
        event.setLocation(request.getLocation() == null || request.getLocation().isBlank() ? null : request.getLocation().trim());
        long schoolId = event.getSchool().getId();
        if (request.getRoomId() != null) {
            Room room = roomRepository.findById(request.getRoomId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xona topilmadi"));
            if (!room.getBuilding().getSchool().getId().equals(schoolId)) {
                throw new ForbiddenException("Boshqa maktab xonasi");
            }
            event.setRoom(room);
        } else {
            event.setRoom(null);
        }
        List<Long> classIds = request.getClassIds() == null ? List.of() : request.getClassIds().stream().distinct().toList();
        for (Long classId : classIds) {
            boolean own = schoolClassRepository.findById(classId)
                    .map(c -> c.getAcademicYear().getSchool().getId().equals(schoolId)).orElse(false);
            if (!own) throw new ForbiddenException("Boshqa maktab sinfi");
        }
        event.setClassIds(classIds.isEmpty() ? null : classIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        // an older client sends nothing: keep what the event had
        if (request.getNotifyParents() != null) event.setNotifyParents(request.getNotifyParents());
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
        dto.setStartTime(event.getStartTime());
        dto.setEndTime(event.getEndTime());
        if (event.getRoom() != null) {
            dto.setRoomId(event.getRoom().getId());
            dto.setRoomNumber(event.getRoom().getRoomNumber());
        }
        dto.setLocation(event.getLocation());
        dto.setClassIds(event.classIdList());
        dto.setNotifyParents(event.getNotifyParents());
        dto.setSeriesId(event.getSeriesId());
        return dto;
    }
}
