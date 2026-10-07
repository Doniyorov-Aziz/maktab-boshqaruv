package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.AttendanceRepository;
import uz.azizbek.maktabboshqaruv.repository.CalendarEventRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Monthly attendance calendar of one class: students down, days across, one dot per
 * student per day. A day's dot is the "worst" mark of that day's lessons
 * (kelmadi > kechikdi > sababli > keldi). Changing a dot marks every lesson of that
 * day for the student — the same rows the per-lesson attendance page writes, so parents
 * get the usual notifications.
 */
@Service
public class AttendanceCalendarService {

    private static final Map<DayOfWeek, String> WEEKDAY = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");
    private static final Map<AttendanceStatus, Integer> SEVERITY = Map.of(
            AttendanceStatus.PRESENT, 1, AttendanceStatus.EXCUSED, 2, AttendanceStatus.LATE, 3, AttendanceStatus.ABSENT, 4);

    public record Day(LocalDate date, String weekday, boolean sunday, String holiday, int lessons) {
    }

    public record Cell(String status, String comment) {
    }

    public record Row(Long studentId, String fullName, String guardianName, String guardianPhone, String guardianRelation,
                      Map<LocalDate, Cell> cells) {
    }

    public record Month(Long classId, String className, String month, List<Day> days, List<Row> students,
                       int marked, int present, int late, int absent, int excused, Double rate) {
    }

    private final SchoolClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final AttendanceRepository attendanceRepository;
    private final LessonSlotRepository lessonSlotRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final AttendanceService attendanceService;
    private final Clock clock;

    public AttendanceCalendarService(SchoolClassRepository classRepository, StudentRepository studentRepository,
                                     AttendanceRepository attendanceRepository, LessonSlotRepository lessonSlotRepository,
                                     CalendarEventRepository calendarEventRepository, AttendanceService attendanceService,
                                     Clock clock) {
        this.classRepository = classRepository;
        this.studentRepository = studentRepository;
        this.attendanceRepository = attendanceRepository;
        this.lessonSlotRepository = lessonSlotRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.attendanceService = attendanceService;
        this.clock = clock;
    }

    /** 4 queries for a whole month, whatever the class size. */
    @Transactional(readOnly = true)
    public Month month(Long classId, YearMonth ym) {
        SchoolClass c = classRepository.findById(classId)
                .orElseThrow(() -> new IllegalStateException("Bunday sinf topilmadi"));
        LocalDate from = ym.atDay(1);
        LocalDate to = ym.atEndOfMonth();

        Map<String, Integer> lessonsPerWeekday = new HashMap<>();
        for (LessonSlot l : lessonSlotRepository.findBySchoolClassId(classId)) {
            lessonsPerWeekday.merge(l.getWeekday(), 1, Integer::sum);
        }
        Map<LocalDate, String> holidays = holidays(c.getAcademicYear().getSchool().getId(), from, to);
        List<Day> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            boolean sunday = d.getDayOfWeek() == DayOfWeek.SUNDAY;
            String wd = WEEKDAY.get(d.getDayOfWeek());
            days.add(new Day(d, wd == null ? "Yakshanba" : wd, sunday, holidays.get(d),
                    sunday ? 0 : lessonsPerWeekday.getOrDefault(wd, 0)));
        }

        // worst mark per (student, day), with the comment of that mark
        Map<Long, Map<LocalDate, Cell>> cells = new HashMap<>();
        int present = 0, late = 0, absent = 0, excused = 0;
        for (Object[] r : attendanceRepository.monthOfClass(classId, from, to)) {
            Long sid = (Long) r[0];
            LocalDate date = (LocalDate) r[1];
            AttendanceStatus status = (AttendanceStatus) r[2];
            String comment = (String) r[3];
            switch (status) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
            }
            cells.computeIfAbsent(sid, k -> new TreeMap<>()).merge(date, new Cell(status.name(), blankToNull(comment)),
                    (a, b) -> SEVERITY.get(AttendanceStatus.valueOf(b.status())) > SEVERITY.get(AttendanceStatus.valueOf(a.status()))
                            ? b : (a.comment() == null && b.status().equals(a.status()) ? b : a));
        }
        int marked = present + late + absent + excused;
        Double rate = marked == 0 ? null : Math.round((present + late) * 1000.0 / marked) / 10.0;

        List<Row> rows = studentRepository.findBySchoolClassIdOrderByLastNameAscFirstNameAsc(classId).stream()
                .map(s -> new Row(s.getId(), s.getFirstName() + " " + s.getLastName(), s.getGuardianName(),
                        s.getGuardianPhone(), s.getGuardianRelation() == null ? null : s.getGuardianRelation().name(), cells.getOrDefault(s.getId(), Map.of())))
                .toList();
        return new Month(classId, c.getGradeNumber() + "-" + c.getSectionLetter(), ym.toString(), days, rows,
                marked, present, late, absent, excused, rate);
    }

    /**
     * Sets one student's whole day: every lesson of that weekday gets the status
     * (status null = "belgilanmagan": the day's marks are removed).
     */
    @Transactional
    public Cell setDay(Long studentId, LocalDate date, AttendanceStatus status, String comment) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalStateException("Bunday o'quvchi topilmadi"));
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) throw new IllegalStateException("Yakshanba — dars yo'q");
        if (date.isAfter(LocalDate.now(clock))) throw new IllegalStateException("Kelajak kunga davomat qo'yilmaydi");
        SchoolClass c = student.getSchoolClass();
        if (holidays(c.getAcademicYear().getSchool().getId(), date, date).containsKey(date)) {
            throw new IllegalStateException("Bu kun bayram/ta'til — dars yo'q");
        }
        String wd = WEEKDAY.get(date.getDayOfWeek());
        List<LessonSlot> lessons = lessonSlotRepository.findBySchoolClassId(c.getId()).stream()
                .filter(l -> wd.equals(l.getWeekday())).toList();
        if (lessons.isEmpty()) throw new IllegalStateException("Bu kunda sinfning darsi yo'q");

        Map<Long, Attendance> existing = new HashMap<>();
        for (Attendance a : attendanceRepository.findByStudentIdAndRecordDate(studentId, date)) {
            existing.put(a.getLessonSlot().getId(), a);
        }
        if (status == null) {
            attendanceRepository.deleteAll(existing.values());
            return new Cell(null, null);
        }
        String note = blankToNull(comment);
        for (LessonSlot lesson : lessons) {
            Attendance a = existing.get(lesson.getId());
            if (a == null) {
                a = new Attendance();
                a.setLessonSlot(lesson);
                a.setStudent(student);
                a.setRecordDate(date);
            }
            AttendanceStatus previous = a.getStatus();
            a.setStatus(status);
            a.setComment(note);
            Attendance saved = attendanceRepository.save(a);
            attendanceService.publishIfNotifiable(saved, previous);
        }
        return new Cell(status.name(), note);
    }

    private Map<LocalDate, String> holidays(Long schoolId, LocalDate from, LocalDate to) {
        Map<LocalDate, String> map = new HashMap<>();
        for (CalendarEvent e : calendarEventRepository.findInRange(schoolId, from, to)) {
            if (e.getType() != CalendarEventType.HOLIDAY && e.getType() != CalendarEventType.VACATION) continue;
            for (LocalDate d = e.getStartDate().isBefore(from) ? from : e.getStartDate();
                 !d.isAfter(e.getEndDate()) && !d.isAfter(to); d = d.plusDays(1)) {
                map.putIfAbsent(d, e.getTitle());
            }
        }
        return map;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
