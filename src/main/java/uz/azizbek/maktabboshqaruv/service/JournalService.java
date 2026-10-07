package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.GradeType;
import uz.azizbek.maktabboshqaruv.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * The class journal ("Baholar jurnali"): a month of one subject for one class, the class
 * overview panel (homeroom teacher, today's lessons) and a teacher's current lesson.
 * Every method is 1–3 SQL statements: the queries select columns, not entities, so no
 * eager relation is ever loaded one by one (checked by JournalQueryCountTest).
 * Averages and "what is on now" are computed by the page.
 */
@Service
public class JournalService {

    static final Map<DayOfWeek, String> WEEKDAY = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba");
    static final Map<DayOfWeek, String> SHORT = Map.of(
            DayOfWeek.MONDAY, "Du", DayOfWeek.TUESDAY, "Se", DayOfWeek.WEDNESDAY, "Ch", DayOfWeek.THURSDAY, "Pa",
            DayOfWeek.FRIDAY, "Ju", DayOfWeek.SATURDAY, "Sh", DayOfWeek.SUNDAY, "Ya");
    /** A teacher still sees the lesson as "current" this long after it ended (to finish the grades). */
    static final Duration GRACE = Duration.ofMinutes(15);

    public record Person(Long id, String fullName, String phone) {
    }

    public record Day(LocalDate date, String weekday, boolean isSunday, boolean isHoliday, String holidayName,
                      boolean isFuture, boolean hasLesson) {
    }

    public record StudentRow(Long id, String fullName) {
    }

    public record GradeCell(Long id, Long studentId, LocalDate date, int value, String type, String comment,
                            String createdBy, LocalDateTime createdAt) {
    }

    public record Journal(Long classId, Long subjectId, String month, LocalDate today, List<Day> days,
                          List<StudentRow> students, List<GradeCell> grades, Person subjectTeacher) {
    }

    public record Lesson(int lessonNo, LocalTime start, LocalTime end, Long subjectId, String subjectName,
                         Person teacher) {
    }

    public record SubjectOption(Long id, String name, Person teacher) {
    }

    public record Overview(Long classId, String className, Person homeroomTeacher, LocalDate today, String weekday,
                           List<Lesson> todayLessons, boolean isDayOff, String dayOffReason,
                           List<SubjectOption> subjects) {
    }

    public record CurrentLesson(Long classId, String className, Long subjectId, String subjectName, int lessonNo,
                                LocalTime start, LocalTime end) {
    }

    /** One timetable row of a class (from {@link LessonSlotRepository#weekOfClass}). */
    private record Slot(String weekday, LocalTime start, LocalTime end, Long subjectId, String subjectName,
                        boolean subjectActive, Person teacher) {
        static Slot of(Object[] r) {
            return new Slot((String) r[0], (LocalTime) r[1], (LocalTime) r[2], (Long) r[3], (String) r[4],
                    r[5] == null || (Boolean) r[5], person((Long) r[6], (String) r[7], (String) r[8], (String) r[9]));
        }
    }

    private final GradeRepository grades;
    private final LessonSlotRepository slots;
    private final CalendarEventRepository events;
    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final Clock clock;

    public JournalService(GradeRepository grades, LessonSlotRepository slots, CalendarEventRepository events,
                          SchoolClassRepository classes, SubjectRepository subjects, Clock clock) {
        this.grades = grades;
        this.slots = slots;
        this.events = events;
        this.classes = classes;
        this.subjects = subjects;
        this.clock = clock;
    }

    /** 3 statements: the class's week, students + grades, days off. */
    @Transactional(readOnly = true)
    public Journal journal(Long classId, Long subjectId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        LocalDate today = LocalDate.now(clock);

        Set<String> lessonDays = new HashSet<>();
        Person teacher = null;
        for (Object[] r : slots.weekOfClass(classId)) {
            Slot s = Slot.of(r);
            if (!s.subjectId().equals(subjectId)) continue;
            lessonDays.add(s.weekday());
            if (teacher == null) teacher = s.teacher();
        }
        Map<LocalDate, String> daysOff = daysOff(classId, from, to);

        List<Day> days = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            boolean sunday = d.getDayOfWeek() == DayOfWeek.SUNDAY;
            String holiday = daysOff.get(d);
            days.add(new Day(d, SHORT.get(d.getDayOfWeek()), sunday, holiday != null, holiday, d.isAfter(today),
                    !sunday && holiday == null && lessonDays.contains(WEEKDAY.get(d.getDayOfWeek()))));
        }

        List<StudentRow> students = new ArrayList<>();
        List<GradeCell> cells = new ArrayList<>();
        Long last = null;
        for (Object[] r : grades.journal(classId, subjectId, from, to)) {
            Long studentId = (Long) r[0];
            if (!studentId.equals(last)) {
                students.add(new StudentRow(studentId, r[1] + " " + r[2]));
                last = studentId;
            }
            if (r[3] != null) {
                cells.add(new GradeCell((Long) r[3], studentId, (LocalDate) r[4], (Integer) r[5],
                        ((GradeType) r[6]).name(), (String) r[7], (String) r[8], (LocalDateTime) r[9]));
            }
        }
        return new Journal(classId, subjectId, month.toString(), today, days, students, cells, teacher);
    }

    /** 3 statements (4 when the class has no timetable yet): class header, its week, today's day off. */
    @Transactional(readOnly = true)
    public Overview overview(Long classId) {
        List<Object[]> header = classes.header(classId);
        if (header.isEmpty()) throw new IllegalStateException("Sinf topilmadi");
        Object[] h = header.get(0);
        String className = h[1] + "-" + h[2];
        Long schoolId = (Long) h[3];
        Person homeroom = h[4] == null ? null : person((Long) h[4], (String) h[5], (String) h[6], (String) h[7]);

        LocalDate today = LocalDate.now(clock);
        String weekday = WEEKDAY.get(today.getDayOfWeek());
        List<Slot> week = slots.weekOfClass(classId).stream().map(Slot::of).toList();
        List<LocalTime> starts = week.stream().map(Slot::start).distinct().sorted().toList();
        List<Lesson> lessons = week.stream()
                .filter(s -> s.weekday().equals(weekday))
                .sorted(Comparator.comparing(Slot::start))
                .map(s -> new Lesson(starts.indexOf(s.start()) + 1, s.start(), s.end(), s.subjectId(), s.subjectName(),
                        s.teacher()))
                .toList();

        // subjects taught in this class (active ones), each with its teacher; all active subjects otherwise
        Map<Long, SubjectOption> taught = new LinkedHashMap<>();
        week.stream().filter(Slot::subjectActive).sorted(Comparator.comparing(Slot::subjectName))
                .forEach(s -> taught.putIfAbsent(s.subjectId(), new SubjectOption(s.subjectId(), s.subjectName(), s.teacher())));
        List<SubjectOption> subjectOptions = new ArrayList<>(taught.values());
        if (subjectOptions.isEmpty()) {
            subjects.findActiveBySchoolId(schoolId, PageRequest.of(0, 500)).forEach(s ->
                    subjectOptions.add(new SubjectOption(s.getId(), s.getName(), null)));
            subjectOptions.sort(Comparator.comparing(SubjectOption::name));
        }

        String dayOff = today.getDayOfWeek() == DayOfWeek.SUNDAY ? "Yakshanba" : daysOff(classId, today, today).get(today);
        return new Overview(classId, className, homeroom, today, weekday,
                dayOff == null ? lessons : List.of(), dayOff != null, dayOff, subjectOptions);
    }

    /**
     * The lesson a teacher is giving now — or ended at most 15 minutes ago. 2 statements.
     * Empty outside lessons (free periods, before and after school, days off).
     */
    @Transactional(readOnly = true)
    public Optional<CurrentLesson> currentLesson(Long employeeId) {
        if (employeeId == null) return Optional.empty();
        LocalDateTime now = LocalDateTime.now(clock);
        String weekday = WEEKDAY.get(now.getDayOfWeek());
        if (weekday == null) return Optional.empty();
        LocalTime t = now.toLocalTime();
        Object[] current = null;
        for (Object[] r : slots.ofTeacherOnDay(employeeId, weekday)) {
            LocalTime start = (LocalTime) r[0];
            LocalTime end = (LocalTime) r[1];
            if (!t.isBefore(start) && t.isBefore(end)) {
                current = r;
                break;
            }
            if (!t.isBefore(end) && !t.isAfter(end.plus(GRACE))) current = r;
        }
        if (current == null) return Optional.empty();
        Long classId = (Long) current[2];
        LocalTime start = (LocalTime) current[0];
        int no = slots.startTimesOfClass(classId).indexOf(start) + 1;
        return Optional.of(new CurrentLesson(classId, current[3] + "-" + current[4], (Long) current[5],
                (String) current[6], no, start, (LocalTime) current[1]));
    }

    private Map<LocalDate, String> daysOff(Long classId, LocalDate from, LocalDate to) {
        Map<LocalDate, String> map = new HashMap<>();
        for (Object[] e : events.daysOffOfClass(classId, from, to)) {
            LocalDate start = (LocalDate) e[0];
            LocalDate end = (LocalDate) e[1];
            for (LocalDate d = start.isBefore(from) ? from : start; !d.isAfter(end) && !d.isAfter(to); d = d.plusDays(1)) {
                map.putIfAbsent(d, (String) e[2]);
            }
        }
        return map;
    }

    private static Person person(Long id, String lastName, String firstName, String phone) {
        return id == null ? null : new Person(id, lastName + " " + firstName, phone);
    }
}
