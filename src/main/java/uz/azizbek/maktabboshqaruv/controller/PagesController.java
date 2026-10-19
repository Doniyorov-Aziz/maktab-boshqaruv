package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.service.RoomBookingService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache.Widget;
import uz.azizbek.maktabboshqaruv.service.pages.CalendarPageService;
import uz.azizbek.maktabboshqaruv.service.pages.ClassesPageService;
import uz.azizbek.maktabboshqaruv.service.pages.RoomsPageService;
import uz.azizbek.maktabboshqaruv.service.pages.TimetablePageService;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

/**
 * The four "v2" pages: Dars jadvali, Taqvim, Sinflar (and one class), Xonalar. Reading is for
 * VIEWER and up, changing for EDITOR and up, the calendar subscription links for ADMIN. Everything
 * is limited to ?schoolId (the school-scope guard checks it, and ids of other kinds are checked
 * against it here). Heavy numbers are cached for 90 s per school (DashboardCache) and dropped when
 * the timetable changes.
 */
@RestController
@RequestMapping("/api/pages")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
public class PagesController {

    private final TimetablePageService timetable;
    private final CalendarPageService calendar;
    private final ClassesPageService classes;
    private final RoomsPageService rooms;
    private final RoomBookingService bookings;
    private final DashboardCache cache;
    private final SchoolAccessService access;

    public PagesController(TimetablePageService timetable, CalendarPageService calendar, ClassesPageService classes,
                           RoomsPageService rooms, RoomBookingService bookings, DashboardCache cache,
                           SchoolAccessService access) {
        this.timetable = timetable;
        this.calendar = calendar;
        this.classes = classes;
        this.rooms = rooms;
        this.bookings = bookings;
        this.cache = cache;
        this.access = access;
    }

    /** VIEWERs see no phone numbers. */
    private boolean phones() {
        return access.caller().role() != Role.VIEWER;
    }

    // ------------------------------------------------------------------ Dars jadvali

    @GetMapping("/timetable/week")
    public TimetablePageService.Week week(@RequestParam Long schoolId) {
        return timetable.week(schoolId, phones());
    }

    @GetMapping("/timetable/kpis")
    public Widget<TimetablePageService.Kpis> timetableKpis(@RequestParam Long schoolId) {
        return cache.get(schoolId, "tt:kpis", () -> timetable.kpis(schoolId));
    }

    @GetMapping("/timetable/day")
    public TimetablePageService.Day timetableDay(@RequestParam Long schoolId,
                                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return timetable.day(schoolId, date);
    }

    @GetMapping("/timetable/substitutes")
    public TimetablePageService.Candidates substitutes(@RequestParam Long schoolId, @RequestParam Long lessonSlotId,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TimetablePageService.Candidates c = timetable.candidates(schoolId, lessonSlotId, date);
        if (phones()) return c;
        return new TimetablePageService.Candidates(c.lessonSlotId(), c.date(), c.subject(), c.absentTeacher(), c.start(), c.end(),
                c.candidates().stream().map(x -> new TimetablePageService.Candidate(x.id(), x.name(), null, x.match(), x.hours(),
                        x.lessonsThatDay(), x.subjects())).toList());
    }

    public record SubstitutionRequest(Long lessonSlotId, LocalDate date, Long substituteId, String reason) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/timetable/substitutions")
    public TimetablePageService.Substitution substitute(@RequestParam Long schoolId, @RequestBody SubstitutionRequest r) {
        if (r.lessonSlotId() == null || r.date() == null || r.substituteId() == null) {
            throw new IllegalStateException("Dars, sana va o'rinbosar tanlanishi shart");
        }
        return timetable.substitute(schoolId, r.lessonSlotId(), r.date(), r.substituteId(), r.reason(), access.caller().username());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/timetable/substitutions/{id}")
    public Map<String, Object> removeSubstitution(@RequestParam Long schoolId, @PathVariable Long id) {
        timetable.removeSubstitution(schoolId, id);
        return Map.of("ok", true);
    }

    // ------------------------------------------------------------------ Taqvim

    @GetMapping("/calendar/overview")
    public Widget<CalendarPageService.Overview> calendarOverview(@RequestParam Long schoolId) {
        return cache.get(schoolId, "cal:overview", () -> calendar.overview(schoolId));
    }

    @GetMapping("/calendar/attendance")
    public List<CalendarPageService.DayAttendance> calendarAttendance(
            @RequestParam Long schoolId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        if (to.isBefore(from) || from.plusDays(400).isBefore(to)) throw new IllegalStateException("Sana oralig'i noto'g'ri");
        return cache.get(schoolId, "cal:att:" + from + ":" + to, () -> calendar.attendance(schoolId, from, to)).data();
    }

    @GetMapping("/calendar/day")
    public CalendarPageService.DaySummary calendarDay(@RequestParam Long schoolId,
                                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return calendar.day(schoolId, date);
    }

    @GetMapping("/calendar/export.ics")
    public ResponseEntity<byte[]> calendarIcs(@RequestParam Long schoolId, @RequestHeader(value = "Host", required = false) String host) {
        return ics(calendar.ics(schoolId, hostOf(host)), "taqvim.ics");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/calendar/feeds")
    public List<CalendarPageService.Feed> feeds(@RequestParam Long schoolId) {
        return calendar.feeds(schoolId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/calendar/feeds")
    public CalendarPageService.Feed createFeed(@RequestParam Long schoolId) {
        return calendar.createFeed(schoolId, access.caller().username());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/calendar/feeds/{id}")
    public Map<String, Object> revokeFeed(@RequestParam Long schoolId, @PathVariable Long id) {
        calendar.revokeFeed(schoolId, id);
        return Map.of("ok", true);
    }

    static String hostOf(String host) {
        if (host == null || host.isBlank()) return "maktab.local";
        return host.replaceAll("[^A-Za-z0-9.\\-]", "").replaceAll(":\\d+$", "");
    }

    static ResponseEntity<byte[]> ics(byte[] body, String name) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/calendar; charset=utf-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(name).build().toString())
                .body(body);
    }

    // ------------------------------------------------------------------ Sinflar

    @GetMapping("/classes/overview")
    public Widget<ClassesPageService.Overview> classesOverview(@RequestParam Long schoolId) {
        return cache.get(schoolId, "classes:overview", () -> classes.overview(schoolId));
    }

    @GetMapping("/classes/compare")
    public List<ClassesPageService.Compared> compare(@RequestParam Long schoolId, @RequestParam List<Long> ids) {
        return classes.compare(schoolId, ids);
    }

    @GetMapping("/classes/{id}")
    public ClassesPageService.Detail classDetail(@RequestParam Long schoolId, @PathVariable Long id) {
        boolean phones = phones();
        return cache.get(schoolId, "class:" + id + ":" + phones, () -> classes.detail(schoolId, id, phones)).data();
    }

    @GetMapping("/classes/{id}/month")
    public List<ClassesPageService.Point> classMonth(@RequestParam Long schoolId, @PathVariable Long id,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return classes.month(schoolId, id, date);
    }

    // ------------------------------------------------------------------ Xonalar

    @GetMapping("/rooms/live")
    public RoomsPageService.Live live(@RequestParam Long schoolId,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                      @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime time) {
        return rooms.live(schoolId, date, time);
    }

    @GetMapping("/rooms/find")
    public RoomsPageService.Finder find(@RequestParam Long schoolId,
                                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                        @RequestParam(required = false) List<Integer> periods,
                                        @RequestParam(required = false) Integer minCapacity,
                                        @RequestParam(required = false) String type,
                                        @RequestParam(required = false) List<String> equipment) {
        return rooms.find(schoolId, date, periods, minCapacity, type, equipment);
    }

    @GetMapping("/rooms/{roomId}/detail")
    public RoomsPageService.Detail roomDetail(@RequestParam Long schoolId, @PathVariable Long roomId) {
        return rooms.detail(schoolId, roomId);
    }

    @GetMapping("/rooms/analytics")
    public Widget<RoomsPageService.Analytics> roomAnalytics(@RequestParam Long schoolId) {
        return cache.get(schoolId, "rooms:analytics", () -> rooms.analytics(schoolId));
    }

    @GetMapping("/rooms/bookings")
    public List<RoomBookingService.BookingView> roomBookings(@RequestParam Long schoolId,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                             @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bookings.inRange(schoolId, from, to);
    }

    public record BookingBody(Long roomId, LocalDate date, LocalTime startTime, LocalTime endTime, String purpose,
                              String responsible) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/rooms/bookings")
    public ResponseEntity<RoomBookingService.BookingView> book(@RequestParam Long schoolId, @RequestBody BookingBody b) {
        RoomBookingService.BookingView v = bookings.create(new RoomBookingService.BookingRequest(schoolId, b.roomId(), b.date(),
                b.startTime(), b.endTime(), b.purpose(), b.responsible()));
        cache.evictSchool(schoolId);
        return ResponseEntity.status(201).body(v);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @DeleteMapping("/rooms/bookings/{id}")
    public Map<String, Object> unbook(@RequestParam Long schoolId, @PathVariable Long id) {
        bookings.delete(id, schoolId);
        cache.evictSchool(schoolId);
        return Map.of("ok", true);
    }
}
