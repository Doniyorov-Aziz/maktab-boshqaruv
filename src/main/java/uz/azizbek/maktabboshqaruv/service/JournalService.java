package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.*;
import uz.azizbek.maktabboshqaruv.repository.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

/**
 * The class journal ("Baholar jurnali"): a month of one subject for one class, the class
 * overview panel (homeroom teacher, today's lessons) and a teacher's current lesson.
 * Every method is 1–3 SQL queries; averages and "what is on now" are computed by the page.
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

    /** 3 queries: the class's lessons, students + grades, days off. */
    @Transactional(readOnly = true)
    public Journal journal(Long classId, Long subjectId, YearMonth month) {
        LocalDate from = month.atDay(1);
        LocalDate to = month.atEndOfMonth();
        LocalDate today = LocalDate.now(clock);

        Set<String> lessonDays = new HashSet<>();
        Person teacher = null;
        for (LessonSlot l : slots.findWeekWithTeachers(classId)) {
            if (!l.getSubject().getId().equals(subjectId)) continue;
            lessonDays.add(l.getWeekday());
            if (teacher == null) teacher = person(l.getEmployee());
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
        for (Object[] row : grades.journal(classId, subjectId, from, to)) {
            Student s = (Student) row[0];
            if (!s.getId().equals(last)) {
                students.add(new StudentRow(s.getId(), s.getLastName() + " " + s.getFirstName()));
                last = s.getId();
            }
            if (row[1] instanceof Grade g) {
                cells.add(new GradeCell(g.getId(), s.getId(), g.getGradeDate(), g.getScore(), g.getType().name(),
                        g.getComment(), g.getCreatedBy(), g.getCreatedDate()));
            }
        }
        return new Journal(classId, subjectId, month.toString(), today, days, students, cells, teacher);
    }

    /** 3 queries (4 when the class has no timetable yet): class + teacher, its week, today's day off. */
    @Transactional(readOnly = true)
    public Overview overview(Long classId) {
        SchoolClass c = classes.findWithTeacher(classId).orElseThrow(() -> new IllegalStateException("Sinf topilmadi"));
        LocalDate today = LocalDate.now(clock);
        String weekday = WEEKDAY.get(today.getDayOfWeek());

        List<LessonSlot> week = slots.findWeekWithTeachers(classId);
        List<LocalTime> starts = week.stream().map(LessonSlot::getStartTime).distinct().sorted().toList();
        List<Lesson> lessons = week.stream()
                .filter(l -> l.getWeekday().equals(weekday))
                .sorted(Comparator.comparing(LessonSlot::getStartTime))
                .map(l -> new Lesson(starts.indexOf(l.getStartTime()) + 1, l.getStartTime(), l.getEndTime(),
                        l.getSubject().getId(), l.getSubject().getName(), person(l.getEmployee())))
                .toList();

        // subjects taught in this class (active ones), each with its teacher; all active subjects otherwise
        Map<Long, SubjectOption> taught = new LinkedHashMap<>();
        week.stream().sorted(Comparator.comparing((LessonSlot l) -> l.getSubject().getName()))
                .filter(l -> l.getSubject().isActive())
                .forEach(l -> taught.putIfAbsent(l.getSubject().getId(),
                        new SubjectOption(l.getSubject().getId(), l.getSubject().getName(), person(l.getEmployee()))));
        List<SubjectOption> subjectOptions = new ArrayList<>(taught.values());
        if (subjectOptions.isEmpty()) {
            subjects.findActiveBySchoolId(c.getAcademicYear().getSchool().getId(), PageRequest.of(0, 500)).forEach(s ->
                    subjectOptions.add(new SubjectOption(s.getId(), s.getName(), null)));
            subjectOptions.sort(Comparator.comparing(SubjectOption::name));
        }

        String dayOff = null;
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dayOff = "Yakshanba";
        } else {
            dayOff = daysOff(classId, today, today).get(today);
        }
        return new Overview(c.getId(), c.getGradeNumber() + "-" + c.getSectionLetter(),
                c.getClassTeacher() == null ? null : person(c.getClassTeacher()), today, weekday,
                dayOff == null ? lessons : List.of(), dayOff != null, dayOff, subjectOptions);
    }

    /**
     * The lesson a teacher is giving now — or ended at most 15 minutes ago. 2 queries.
     * Empty outside lessons (breaks, before and after school, days off).
     */
    @Transactional(readOnly = true)
    public Optional<CurrentLesson> currentLesson(Long employeeId) {
        if (employeeId == null) return Optional.empty();
        LocalDateTime now = LocalDateTime.now(clock);
        String weekday = WEEKDAY.get(now.getDayOfWeek());
        if (weekday == null) return Optional.empty();
        LocalTime t = now.toLocalTime();
        LessonSlot current = null;
        for (LessonSlot l : slots.findOfTeacherOnDay(employeeId, weekday)) {
            boolean during = !t.isBefore(l.getStartTime()) && t.isBefore(l.getEndTime());
            boolean justEnded = !t.isBefore(l.getEndTime()) && !t.isAfter(l.getEndTime().plus(GRACE));
            if (during) {
                current = l;
                break;
            }
            if (justEnded) current = l;
        }
        if (current == null) return Optional.empty();
        SchoolClass c = current.getSchoolClass();
        int no = slots.startTimesOfClass(c.getId()).indexOf(current.getStartTime()) + 1;
        return Optional.of(new CurrentLesson(c.getId(), c.getGradeNumber() + "-" + c.getSectionLetter(),
                current.getSubject().getId(), current.getSubject().getName(), no,
                current.getStartTime(), current.getEndTime()));
    }

    private Map<LocalDate, String> daysOff(Long classId, LocalDate from, LocalDate to) {
        Map<LocalDate, String> map = new HashMap<>();
        for (CalendarEvent e : events.daysOffOfClass(classId, from, to)) {
            for (LocalDate d = e.getStartDate().isBefore(from) ? from : e.getStartDate();
                 !d.isAfter(e.getEndDate()) && !d.isAfter(to); d = d.plusDays(1)) {
                map.putIfAbsent(d, e.getTitle());
            }
        }
        return map;
    }

    static Person person(Employee e) {
        return e == null ? null : new Person(e.getId(), e.getLastName() + " " + e.getFirstName(), e.getPhone());
    }
}
