package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.CalendarEvent;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.Room;
import uz.azizbek.maktabboshqaruv.entity.RoomBooking;
import uz.azizbek.maktabboshqaruv.entity.RoomStatus;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomBookingRepository;
import uz.azizbek.maktabboshqaruv.repository.RoomRepository;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * One-off room bookings ("Band qilish") and the one rule for them: a room is free at a time when it
 * is not under repair, has no lesson then (on a school day) and no other booking. A calendar event
 * with a room books it for each of its days; moving the event moves (and re-checks) the bookings.
 */
@Service
public class RoomBookingService {

    /** A whole-day event with a room books it for the school day. */
    public static final LocalTime DAY_START = LocalTime.of(8, 0);
    public static final LocalTime DAY_END = LocalTime.of(17, 0);
    /** An event longer than this books only its first days (a vacation never takes a room). */
    static final int MAX_EVENT_DAYS = 31;

    public record BookingRequest(Long schoolId, Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime,
                                 String purpose, String responsible) {
    }

    public record BookingView(long id, long roomId, String roomNumber, String buildingName, LocalDate date,
                              LocalTime startTime, LocalTime endTime, String purpose, String responsible,
                              Long calendarEventId, String createdBy) {
    }

    private final RoomBookingRepository bookings;
    private final RoomRepository rooms;
    private final LessonSlotRepository lessons;
    private final SchoolDays schoolDays;
    private final Clock clock;

    public RoomBookingService(RoomBookingRepository bookings, RoomRepository rooms, LessonSlotRepository lessons,
                              SchoolDays schoolDays, Clock clock) {
        this.bookings = bookings;
        this.rooms = rooms;
        this.lessons = lessons;
        this.schoolDays = schoolDays;
        this.clock = clock;
    }

    /**
     * 409 with the reason when the room is not free on the date in [start, end): under repair, a
     * lesson ("204-xona bu vaqtda band: 7-B, Fizika (10:20–11:05)") or another booking.
     */
    @Transactional(readOnly = true)
    public void requireFree(Room room, LocalDate date, LocalTime start, LocalTime end, Long exceptEventId, Long exceptBookingId) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new IllegalStateException("Boshlanish vaqti tugash vaqtidan oldin bo'lishi kerak");
        }
        String label = LessonSlotService.roomLabel(room.getRoomNumber());
        if (room.getStatus() == RoomStatus.REPAIR) {
            throw new IllegalStateException(label + " ta'mirda — band qilib bo'lmaydi");
        }
        long schoolId = room.getBuilding().getSchool().getId();
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        if (weekday != null && schoolDays.isSchoolDay(schoolId, date)) {
            for (LessonSlot l : lessons.findByRoomIdAndWeekday(room.getId(), weekday)) {
                if (l.getStartTime().isBefore(end) && l.getEndTime().isAfter(start)) {
                    throw new IllegalStateException(label + " bu vaqtda band: " + l.getSchoolClass().getGradeNumber() + "-"
                            + l.getSchoolClass().getSectionLetter() + ", " + l.getSubject().getName()
                            + " (" + hm(l.getStartTime()) + "–" + hm(l.getEndTime()) + ")");
                }
            }
        }
        for (RoomBooking b : bookings.overlapping(room.getId(), date, start, end, exceptEventId, exceptBookingId)) {
            throw new IllegalStateException(label + " bu vaqtda band qilingan: " + b.getPurpose()
                    + " (" + hm(b.getStartTime()) + "–" + hm(b.getEndTime()) + ")");
        }
    }

    static String hm(LocalTime t) {
        return t.toString().substring(0, 5);
    }

    @Transactional
    public BookingView create(BookingRequest r) {
        if (r.roomId() == null || r.date() == null) throw new IllegalStateException("Xona va sana tanlanishi shart");
        if (r.purpose() == null || r.purpose().isBlank()) throw new IllegalStateException("Maqsad kiritilishi shart");
        Room room = rooms.findById(r.roomId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xona topilmadi"));
        if (r.schoolId() != null && !r.schoolId().equals(room.getBuilding().getSchool().getId())) {
            throw new ForbiddenException("Boshqa maktab ma'lumotiga ruxsat yo'q");
        }
        if (r.date().isBefore(LocalDate.now(clock))) throw new IllegalStateException("O'tgan kunni band qilib bo'lmaydi");
        requireFree(room, r.date(), r.startTime(), r.endTime(), null, null);
        RoomBooking b = new RoomBooking();
        b.setSchool(room.getBuilding().getSchool());
        b.setRoom(room);
        b.setDate(r.date());
        b.setStartTime(r.startTime());
        b.setEndTime(r.endTime());
        b.setPurpose(r.purpose().trim());
        b.setResponsible(r.responsible() == null || r.responsible().isBlank() ? null : r.responsible().trim());
        b.setCreatedBy(currentUser());
        b.setCreatedAt(LocalDateTime.now(clock));
        return view(bookings.save(b));
    }

    /** A booking the calendar made for an event goes with the event, not from here. */
    @Transactional
    public void delete(Long id, Long schoolId) {
        RoomBooking b = bookings.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday band qilish topilmadi"));
        if (schoolId != null && !schoolId.equals(b.getSchool().getId())) {
            throw new ForbiddenException("Boshqa maktab ma'lumotiga ruxsat yo'q");
        }
        if (b.getCalendarEventId() != null) {
            throw new IllegalStateException("Bu xonani taqvimdagi tadbir band qilgan — tadbirni o'zgartiring");
        }
        bookings.delete(b);
    }

    @Transactional(readOnly = true)
    public List<BookingView> inRange(long schoolId, LocalDate from, LocalDate to) {
        return bookings.findInRange(schoolId, from, to).stream().map(RoomBookingService::view).toList();
    }

    /**
     * The event's own bookings, again: dropped and made for each of its days with its times (a
     * whole-day event: 08:00–17:00). Any conflict is a 409 and nothing is saved (the caller's
     * transaction rolls back).
     */
    @Transactional
    public void bookForEvent(CalendarEvent e) {
        bookings.deleteByCalendarEventId(e.getId());
        bookings.flush();
        if (e.getRoom() == null) return;
        Room room = e.getRoom();
        LocalTime start = e.getStartTime() != null ? e.getStartTime() : DAY_START;
        LocalTime end = e.getEndTime() != null ? e.getEndTime() : DAY_END;
        long days = Math.min(ChronoUnit.DAYS.between(e.getStartDate(), e.getEndDate()) + 1, MAX_EVENT_DAYS);
        for (int k = 0; k < days; k++) {
            LocalDate d = e.getStartDate().plusDays(k);
            requireFree(room, d, start, end, e.getId(), null);
            RoomBooking b = new RoomBooking();
            b.setSchool(e.getSchool());
            b.setRoom(room);
            b.setDate(d);
            b.setStartTime(start);
            b.setEndTime(end);
            b.setPurpose(e.getTitle().length() > 200 ? e.getTitle().substring(0, 200) : e.getTitle());
            b.setCalendarEventId(e.getId());
            b.setCreatedBy(currentUser());
            b.setCreatedAt(LocalDateTime.now(clock));
            bookings.save(b);
        }
    }

    @Transactional
    public void dropForEvent(Long eventId) {
        bookings.deleteByCalendarEventId(eventId);
    }

    static BookingView view(RoomBooking b) {
        return new BookingView(b.getId(), b.getRoom().getId(), b.getRoom().getRoomNumber(), b.getRoom().getBuilding().getName(),
                b.getDate(), b.getStartTime(), b.getEndTime(), b.getPurpose(), b.getResponsible(), b.getCalendarEventId(),
                b.getCreatedBy());
    }

    static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
