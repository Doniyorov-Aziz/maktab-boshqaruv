package uz.azizbek.maktabboshqaruv.service;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.bot.BotI18n;
import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.telegram.MessageFormatter;

import java.sql.Timestamp;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Ota-onalar bilan uchrashuv: teachers (or an administrator for them) open free times, a parent books one in the
 * bot for one of their children, the teacher sees the booking in the list and in the bell. A booking cancelled by
 * the school is told to the parent; a booking the parent cancels is told to the teacher.
 */
@Service
public class MeetingService {

    /** How far ahead parents see free times. */
    public static final int BOOKING_DAYS = 30;
    /** Bookings closer than this cannot be made or cancelled by the parent. */
    public static final int CUTOFF_MINUTES = 60;

    public record BulkRequest(@NotNull(message = "O'qituvchi tanlanishi shart") Long employeeId,
                              @NotNull(message = "Sana kiritilishi shart") LocalDate date,
                              @NotNull(message = "Boshlanish vaqti kiritilishi shart") LocalTime from,
                              @NotNull(message = "Tugash vaqti kiritilishi shart") LocalTime to,
                              @NotNull @Min(value = 5, message = "Uchrashuv kamida 5 daqiqa") @Max(value = 120, message = "Uchrashuv 2 soatdan oshmasin") Integer minutes,
                              @Min(0) @Max(60) Integer gapMinutes,
                              @Size(max = 80, message = "Joy 80 belgidan oshmasin") String place,
                              /** Repeat on the same weekday for this many weeks (1 = only this day). */
                              @Min(1) @Max(12) Integer weeks) {
    }

    public record SlotDto(Long id, Long employeeId, String employeeName, LocalDate date, LocalTime startTime,
                          LocalTime endTime, String place, boolean booked, Long studentId, String studentName,
                          String className, String parentName, String parentPhone, String topic, LocalDateTime bookedAt) {
    }

    /** What a parent's booking attempt came to. */
    public enum BookResult { OK, TAKEN, TOO_LATE, ALREADY_WITH_TEACHER, NOT_ALLOWED }

    private final MeetingSlotRepository slots;
    private final EmployeeRepository employees;
    private final StudentRepository students;
    private final LessonSlotRepository lessons;
    private final NotificationService notifications;
    private final SchoolAccessService access;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public MeetingService(MeetingSlotRepository slots, EmployeeRepository employees, StudentRepository students,
                          LessonSlotRepository lessons, NotificationService notifications, SchoolAccessService access,
                          JdbcTemplate jdbc, Clock clock) {
        this.slots = slots;
        this.employees = employees;
        this.students = students;
        this.lessons = lessons;
        this.notifications = notifications;
        this.access = access;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    // ------------------------------------------------------------------ staff

    @Transactional(readOnly = true)
    public List<SlotDto> list(Long schoolId, Long employeeId, LocalDate from, LocalDate to) {
        access.requireSchool(schoolId);
        LocalDate f = from != null ? from : LocalDate.now(clock);
        LocalDate t = to != null ? to : f.plusDays(BOOKING_DAYS);
        if (ChronoUnit.DAYS.between(f, t) > 400) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Davr juda uzun");
        return slots.search(schoolId, employeeId, f, t).stream().map(MeetingService::toDto).toList();
    }

    /** "Bo'sh vaqtlar qo'shish": cuts the time range into meetings; a time that overlaps one already there is skipped. */
    @Transactional
    public List<SlotDto> createBulk(BulkRequest r) {
        SchoolAccessService.Caller caller = access.caller();
        Employee e = employees.findById(r.employeeId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday xodim topilmadi"));
        requireTeacherOrAdmin(caller, e.getId());
        if (!r.to().isAfter(r.from())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tugash vaqti boshlanishdan keyin bo'lsin");
        if (r.date().isBefore(LocalDate.now(clock))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O'tgan kunga vaqt qo'shib bo'lmaydi");
        int gap = r.gapMinutes() == null ? 0 : r.gapMinutes();
        int weeks = r.weeks() == null ? 1 : r.weeks();
        List<SlotDto> created = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now(clock);
        for (int w = 0; w < weeks; w++) {
            LocalDate day = r.date().plusWeeks(w);
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) continue;
            List<MeetingSlot> existing = slots.ofTeacherOn(e.getId(), day);
            int fromMin = r.from().toSecondOfDay() / 60;
            int toMin = r.to().toSecondOfDay() / 60;
            for (int at = fromMin; at + r.minutes() <= toMin; at += r.minutes() + gap) {
                LocalTime s = LocalTime.of(at / 60, at % 60);
                LocalTime end = s.plusMinutes(r.minutes());
                LocalTime start = s;
                boolean overlaps = existing.stream().anyMatch(m -> start.isBefore(m.getEndTime()) && end.isAfter(m.getStartTime()));
                if (overlaps) continue;
                MeetingSlot m = new MeetingSlot();
                m.setSchool(e.getSchool());
                m.setEmployee(e);
                m.setDate(day);
                m.setStartTime(start);
                m.setEndTime(end);
                m.setPlace(r.place() == null || r.place().isBlank() ? null : r.place().strip());
                m.setCreatedBy(caller.username());
                m.setCreatedAt(now);
                slots.save(m);
                existing.add(m);
                created.add(toDto(m));
            }
        }
        if (created.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Yangi vaqt qo'shilmadi — bu vaqtlarda uchrashuvlar bor yoki oraliq juda qisqa");
        }
        return created;
    }

    /** Removes a time; a booked one is cancelled first and the parent told. */
    @Transactional
    public void delete(Long id) {
        MeetingSlot m = find(id);
        requireTeacherOrAdmin(access.caller(), m.getEmployee().getId());
        if (m.isBooked()) tellParentCancelled(m);
        slots.delete(m);
    }

    /** "Bekor qilish" of a booking by the school: the time is free again, the parent is told. */
    @Transactional
    public SlotDto cancelBooking(Long id) {
        MeetingSlot m = find(id);
        requireTeacherOrAdmin(access.caller(), m.getEmployee().getId());
        if (!m.isBooked()) return toDto(m);
        tellParentCancelled(m);
        clear(m);
        return toDto(m);
    }

    // ------------------------------------------------------------------ parents (bot)

    /** The child's teachers (class teacher first, then by name) who have free times ahead, with how many. */
    @Transactional(readOnly = true)
    public List<Map.Entry<Employee, Integer>> teachersWithFreeTimes(Student child) {
        List<MeetingSlot> free = freeFor(child, null);
        Map<Long, Employee> byId = new LinkedHashMap<>();
        Map<Long, Integer> counts = new HashMap<>();
        for (MeetingSlot m : free) {
            byId.putIfAbsent(m.getEmployee().getId(), m.getEmployee());
            counts.merge(m.getEmployee().getId(), 1, Integer::sum);
        }
        Long classTeacher = child.getSchoolClass().getClassTeacher() == null ? null : child.getSchoolClass().getClassTeacher().getId();
        List<Map.Entry<Employee, Integer>> out = new ArrayList<>();
        byId.values().stream()
                .sorted(Comparator.comparing((Employee e) -> !e.getId().equals(classTeacher)).thenComparing(Employee::getLastName))
                .forEach(e -> out.add(Map.entry(e, counts.get(e.getId()))));
        return out;
    }

    /** Free times of the child's teachers ({@code teacherId} = one of them), from an hour from now. */
    @Transactional(readOnly = true)
    public List<MeetingSlot> freeFor(Student child, Long teacherId) {
        Set<Long> teachers = teachersOf(child);
        if (teacherId != null) teachers = teachers.contains(teacherId) ? Set.of(teacherId) : Set.of();
        if (teachers.isEmpty()) return List.of();
        LocalDateTime earliest = LocalDateTime.now(clock).plusMinutes(CUTOFF_MINUTES);
        return slots.freeOf(teachers, earliest.toLocalDate(), earliest.toLocalDate().plusDays(BOOKING_DAYS)).stream()
                .filter(m -> m.getDate().atTime(m.getStartTime()).isAfter(earliest)).toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingSlot> bookingsOf(long chatId) {
        return slots.bookedBy(chatId, LocalDate.now(clock));
    }

    @Transactional(readOnly = true)
    public Optional<MeetingSlot> slot(Long id) {
        return slots.findById(id);
    }

    @Transactional
    public BookResult book(long chatId, Student child, Long slotId, String parentName, String topic) {
        MeetingSlot m = slots.findById(slotId).orElse(null);
        if (m == null || m.isBooked()) return BookResult.TAKEN;
        if (!teachersOf(child).contains(m.getEmployee().getId())) return BookResult.NOT_ALLOWED;
        if (m.getDate().atTime(m.getStartTime()).isBefore(LocalDateTime.now(clock).plusMinutes(CUTOFF_MINUTES))) {
            return BookResult.TOO_LATE;
        }
        boolean already = slots.bookedBy(chatId, LocalDate.now(clock)).stream()
                .anyMatch(b -> b.getEmployee().getId().equals(m.getEmployee().getId())
                        && b.getStudent() != null && b.getStudent().getId().equals(child.getId()));
        if (already) return BookResult.ALREADY_WITH_TEACHER;
        String phone = child.getGuardianPhone();
        int taken = slots.book(slotId, chatId, students.getReferenceById(child.getId()), trim(parentName, 120), trim(phone, 32),
                trim(topic, 300), LocalDateTime.now(clock));
        if (taken == 0) return BookResult.TAKEN;
        bell(m, "📅 " + NotificationService.fullName(child) + " (" + NotificationService.className(child) + ") ota-onasi "
                + m.getDate() + " " + m.getStartTime() + " ga uchrashuvga yozildi");
        return BookResult.OK;
    }

    /** The parent's own cancellation (until an hour before). */
    @Transactional
    public boolean cancelByParent(long chatId, Long slotId) {
        MeetingSlot m = slots.findById(slotId).orElse(null);
        if (m == null || !Objects.equals(m.getChatId(), chatId)) return false;
        if (m.getDate().atTime(m.getStartTime()).isBefore(LocalDateTime.now(clock).plusMinutes(CUTOFF_MINUTES))) return false;
        String who = m.getStudent() == null ? "Ota-ona" : NotificationService.fullName(m.getStudent()) + " ota-onasi";
        clear(m);
        bell(m, "↩️ " + who + " " + m.getDate() + " " + m.getStartTime() + " dagi uchrashuvni bekor qildi");
        return true;
    }

    // ------------------------------------------------------------------ helpers

    /** Class teacher and every teacher of the class's timetable. */
    Set<Long> teachersOf(Student child) {
        Set<Long> ids = new LinkedHashSet<>();
        SchoolClass c = child.getSchoolClass();
        if (c.getClassTeacher() != null) ids.add(c.getClassTeacher().getId());
        for (LessonSlot l : lessons.findBySchoolClassId(c.getId())) ids.add(l.getEmployee().getId());
        return ids;
    }

    private void tellParentCancelled(MeetingSlot m) {
        if (m.getChatId() == null || m.getStudent() == null) return;
        Student s = m.getStudent();
        String teacher = (m.getEmployee().getLastName() + " " + m.getEmployee().getFirstName()).strip();
        LocalDate date = m.getDate();
        String time = m.getStartTime().toString();
        notifications.enqueueDirect(NotificationService.schoolOf(s), s, m.getChatId(), NotificationType.MEETING, m.getId(),
                LocalDate.now(clock), lang -> BotI18n.get().t(lang, "notif.meeting_cancelled",
                        "teacher", MessageFormatter.escape(teacher), "date", BotI18n.get().dateFull(lang, date), "time", time)
                        + MessageFormatter.footer(lang, NotificationService.schoolOf(s).getName()),
                null);
    }

    private void clear(MeetingSlot m) {
        m.setChatId(null);
        m.setStudent(null);
        m.setParentName(null);
        m.setParentPhone(null);
        m.setTopic(null);
        m.setBookedAt(null);
    }

    /** A line in the teacher's bell (top bar), opening the meetings page. */
    private void bell(MeetingSlot m, String text) {
        jdbc.update("""
                insert into staff_notification (school_id, employee_id, lesson_slot_id, record_date, kind, text, link, created_at)
                values (?, ?, null, ?, 'MEETING', ?, '/meetings', ?)""",
                m.getSchool().getId(), m.getEmployee().getId(), java.sql.Date.valueOf(m.getDate()),
                text.length() > 300 ? text.substring(0, 300) : text, Timestamp.valueOf(LocalDateTime.now(clock)));
    }

    private void requireTeacherOrAdmin(SchoolAccessService.Caller caller, Long employeeId) {
        if (caller.isAdmin() || Objects.equals(caller.employeeId(), employeeId)) return;
        throw new ForbiddenException("Faqat o'z uchrashuv vaqtlaringizni o'zgartira olasiz");
    }

    private MeetingSlot find(Long id) {
        return slots.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday uchrashuv vaqti topilmadi"));
    }

    private static String trim(String s, int max) {
        if (s == null || s.isBlank()) return null;
        s = s.strip();
        return s.length() > max ? s.substring(0, max) : s;
    }

    static SlotDto toDto(MeetingSlot m) {
        Employee e = m.getEmployee();
        Student s = m.getStudent();
        return new SlotDto(m.getId(), e.getId(), (e.getLastName() + " " + e.getFirstName()).strip(), m.getDate(),
                m.getStartTime(), m.getEndTime(), m.getPlace(), m.isBooked(),
                s == null ? null : s.getId(), s == null ? null : NotificationService.fullName(s),
                s == null ? null : NotificationService.className(s), m.getParentName(), m.getParentPhone(), m.getTopic(),
                m.getBookedAt());
    }
}
