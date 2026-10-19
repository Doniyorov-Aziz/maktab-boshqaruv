package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.entity.LessonSubstitution;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.repository.LessonSubstitutionRepository;
import uz.azizbek.maktabboshqaruv.service.BotCache;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * "Dars jadvali" page: the school's whole week in one answer (the page draws the class, teacher and
 * room views, the teachers' load and the live drag-and-drop check from it), the hero numbers, one
 * day's attendance state per lesson and the one-day substitutions ("o'rinbosar").
 */
@Service
public class TimetablePageService {

    public record Lesson(long id, long classId, String className, int grade, long subjectId, String subject,
                         String subjectColor, long teacherId, String teacher, String teacherShort, long roomId,
                         String room, String weekday, LocalTime start, LocalTime end) {
    }

    public record Period(int n, LocalTime start, LocalTime end) {
    }

    public record SubjectRef(long id, String name, String color, boolean active) {
    }

    public record ClassRef(long id, String name, int grade, String letter, int students) {
    }

    public record TeacherRef(long id, String name, String shortName, String phone) {
    }

    public record RoomRef(long id, String number, String building, String type, int capacity, Integer floor, String status) {
    }

    public record Week(List<Lesson> lessons, List<Period> periods, List<SubjectRef> subjects, List<ClassRef> classes,
                       List<TeacherRef> teachers, List<RoomRef> rooms) {
    }

    public record Conflict(String kind, String weekday, LocalTime start, LocalTime end, List<Long> lessonIds, String text) {
    }

    public record Kpis(int lessons, List<Integer> lessonsByDay, Double teacherHours, List<Double> teacherHoursByDay,
                       int teachers, Double roomUse, List<Double> roomUseByDay, int rooms, List<Conflict> conflicts) {
    }

    /** One lesson's state on a date: MANUAL (✓), AUTO (🤖), NONE (⏳). */
    public record Substitution(long id, long lessonSlotId, LocalDate date, long substituteId, String substitute,
                               long absentTeacherId, String absentTeacher, String reason) {
    }

    public record Day(LocalDate date, String weekday, boolean schoolDay, LocalDateTime now, Map<Long, String> states,
                      List<Substitution> substitutions) {
    }

    public record Candidate(long id, String name, String phone, String match, double hours, int lessonsThatDay,
                            List<String> subjects) {
    }

    public record Candidates(long lessonSlotId, LocalDate date, String subject, String absentTeacher, LocalTime start,
                             LocalTime end, List<Candidate> candidates) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final LessonSlotRepository lessons;
    private final LessonSubstitutionRepository substitutions;
    private final EmployeeRepository employees;
    private final SchoolDays schoolDays;
    private final DashboardCache cache;
    private final BotCache botCache;
    private final Clock clock;

    public TimetablePageService(NamedParameterJdbcTemplate jdbc, LessonSlotRepository lessons,
                                LessonSubstitutionRepository substitutions, EmployeeRepository employees,
                                SchoolDays schoolDays, DashboardCache cache, BotCache botCache, Clock clock) {
        this.jdbc = jdbc;
        this.lessons = lessons;
        this.substitutions = substitutions;
        this.employees = employees;
        this.schoolDays = schoolDays;
        this.cache = cache;
        this.botCache = botCache;
        this.clock = clock;
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    // ------------------------------------------------------------------ the week

    /** Every lesson of the school and everything the page picks from (phones only for staff who may call). */
    @Transactional(readOnly = true)
    public Week week(long schoolId, boolean withPhones) {
        List<Lesson> all = lessonsOf(schoolId);
        List<SubjectRef> subjects = jdbc.query("""
                select s.id, s.name, s.color, coalesce(s.active, true) as active from subject s
                 where s.school_id = :s and (coalesce(s.active, true) or exists (select 1 from lesson_slot ls where ls.subject_id = s.id))
                 order by s.name""", p(schoolId),
                (rs, n) -> new SubjectRef(rs.getLong("id"), rs.getString("name"), rs.getString("color"), rs.getBoolean("active")));
        List<ClassRef> classes = jdbc.query("""
                select c.id, c.grade_number, c.section_letter, (select count(*) from students st where st.class_id = c.id) as students
                  from school_class c join academic_year y on y.id = c.academic_year_id
                 where y.school_id = :s and (exists (select 1 from students st where st.class_id = c.id)
                       or exists (select 1 from lesson_slot ls where ls.class_id = c.id))
                 order by c.grade_number, c.section_letter""", p(schoolId),
                (rs, n) -> new ClassRef(rs.getLong("id"), rs.getInt("grade_number") + "-" + rs.getString("section_letter"),
                        rs.getInt("grade_number"), rs.getString("section_letter"), rs.getInt("students")));
        List<TeacherRef> teachers = jdbc.query("""
                select e.id, e.first_name, e.last_name, e.phone from employee e
                 where e.school_id = :s and coalesce(e.status, 'ACTIVE') <> 'DISMISSED'
                 order by e.last_name, e.first_name""", p(schoolId),
                (rs, n) -> new TeacherRef(rs.getLong("id"),
                        (rs.getString("last_name") + " " + rs.getString("first_name")).trim(),
                        shortName(rs.getString("last_name"), rs.getString("first_name")),
                        withPhones ? rs.getString("phone") : null));
        List<RoomRef> rooms = jdbc.query("""
                select r.id, r.room_number, b.name as building, r.type, r.capacity, r.floor, coalesce(r.status, 'ACTIVE') as status
                  from room r join building b on b.id = r.building_id where b.school_id = :s
                 order by b.name, r.floor nulls last, r.room_number""", p(schoolId),
                (rs, n) -> new RoomRef(rs.getLong("id"), rs.getString("room_number"), rs.getString("building"),
                        rs.getString("type") == null ? "CLASSROOM" : rs.getString("type"), rs.getInt("capacity"),
                        (Integer) rs.getObject("floor"), rs.getString("status")));
        return new Week(all, periods(all), subjects, classes, teachers, rooms);
    }

    List<Lesson> lessonsOf(long schoolId) {
        return jdbc.query(LESSONS, p(schoolId), (rs, n) -> new Lesson(rs.getLong("id"), rs.getLong("class_id"),
                rs.getInt("grade_number") + "-" + rs.getString("section_letter"), rs.getInt("grade_number"),
                rs.getLong("subject_id"), rs.getString("subject"), rs.getString("subject_color"), rs.getLong("employee_id"),
                (rs.getString("last_name") + " " + rs.getString("first_name")).trim(),
                shortName(rs.getString("last_name"), rs.getString("first_name")), rs.getLong("room_id"),
                rs.getString("room_number"), rs.getString("weekday"), time(rs.getObject("start_time")),
                time(rs.getObject("end_time"))));
    }

    /** The bell periods: distinct start times in order (the end is the longest lesson starting then). */
    static List<Period> periods(List<Lesson> all) {
        TreeMap<LocalTime, LocalTime> byStart = new TreeMap<>();
        for (Lesson l : all) byStart.merge(l.start(), l.end(), (a, b) -> a.isAfter(b) ? a : b);
        List<Period> out = new ArrayList<>();
        int n = 1;
        for (Map.Entry<LocalTime, LocalTime> e : byStart.entrySet()) out.add(new Period(n++, e.getKey(), e.getValue()));
        return out;
    }

    // ------------------------------------------------------------------ hero numbers

    /** Lessons a week, the teachers' average load (hours a week), room use (%) and every conflict. */
    @Transactional(readOnly = true)
    public Kpis kpis(long schoolId) {
        List<Lesson> all = lessonsOf(schoolId);
        int[] byDay = new int[6];
        double[] minutesByDay = new double[6];
        Map<Long, Double> minutesByTeacher = new HashMap<>();
        for (Lesson l : all) {
            int d = weekdayIndex(l.weekday());
            if (d < 0) continue;
            long minutes = Duration.between(l.start(), l.end()).toMinutes();
            byDay[d]++;
            minutesByDay[d] += minutes;
            minutesByTeacher.merge(l.teacherId(), (double) minutes, Double::sum);
        }
        int teachers = minutesByTeacher.size();
        Double teacherHours = teachers == 0 ? null
                : round1(minutesByTeacher.values().stream().mapToDouble(Double::doubleValue).sum() / 60.0 / teachers);
        List<Double> hoursByDay = new ArrayList<>();
        for (int d = 0; d < 6; d++) hoursByDay.add(teachers == 0 ? null : round1(minutesByDay[d] / 60.0 / teachers));

        // room use: (room, day, period) taken / (active rooms × periods × 6 days)
        int periods = periods(all).size();
        Integer activeRooms = jdbc.queryForObject("""
                select count(*) from room r join building b on b.id = r.building_id
                 where b.school_id = :s and coalesce(r.status, 'ACTIVE') = 'ACTIVE'""", p(schoolId), Integer.class);
        int rooms = activeRooms == null ? 0 : activeRooms;
        Set<String> taken = new HashSet<>();
        int[] takenByDay = new int[6];
        for (Lesson l : all) {
            int d = weekdayIndex(l.weekday());
            if (d >= 0 && taken.add(l.roomId() + "|" + d + "|" + l.start())) takenByDay[d]++;
        }
        Double roomUse = rooms == 0 || periods == 0 ? null : round1(taken.size() * 100.0 / (rooms * periods * 6.0));
        List<Double> roomUseByDay = new ArrayList<>();
        for (int d = 0; d < 6; d++) roomUseByDay.add(rooms == 0 || periods == 0 ? null : round1(takenByDay[d] * 100.0 / (rooms * periods)));

        List<Integer> lessonsByDay = new ArrayList<>();
        for (int d = 0; d < 6; d++) lessonsByDay.add(byDay[d]);
        return new Kpis(all.size(), lessonsByDay, teacherHours, hoursByDay, teachers, roomUse, roomUseByDay, rooms, conflicts(all));
    }

    /** Two lessons at overlapping times on one day with the same teacher, room or class. */
    static List<Conflict> conflicts(List<Lesson> all) {
        Map<String, List<Lesson>> byDay = new HashMap<>();
        for (Lesson l : all) byDay.computeIfAbsent(l.weekday(), k -> new ArrayList<>()).add(l);
        List<Conflict> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (List<Lesson> day : byDay.values()) {
            day.sort(Comparator.comparing(Lesson::start));
            for (int a = 0; a < day.size(); a++) {
                for (int b = a + 1; b < day.size(); b++) {
                    Lesson x = day.get(a), y = day.get(b);
                    if (!y.start().isBefore(x.end())) break;
                    String pair = Math.min(x.id(), y.id()) + "-" + Math.max(x.id(), y.id());
                    if (!seen.add(pair)) continue;
                    String kind = null, text = null;
                    if (x.teacherId() == y.teacherId()) {
                        kind = "TEACHER";
                        text = x.teacherShort() + " bir vaqtda " + x.className() + " va " + y.className() + " da";
                    } else if (x.roomId() == y.roomId()) {
                        kind = "ROOM";
                        text = roomLabel(x.room()) + " bir vaqtda " + x.className() + " va " + y.className() + " uchun";
                    } else if (x.classId() == y.classId()) {
                        kind = "CLASS";
                        text = x.className() + " sinfida bir vaqtda ikki dars: " + x.subject() + " va " + y.subject();
                    }
                    if (kind != null) {
                        out.add(new Conflict(kind, x.weekday(), x.start(), x.end().isAfter(y.end()) ? x.end() : y.end(),
                                List.of(x.id(), y.id()), text));
                    }
                }
            }
        }
        out.sort(Comparator.comparing((Conflict c) -> weekdayIndex(c.weekday())).thenComparing(Conflict::start));
        return out;
    }

    static String roomLabel(String number) {
        if (number == null || number.isEmpty()) return "Xona";
        return Character.isDigit(number.charAt(0)) ? number + "-xona" : number;
    }

    // ------------------------------------------------------------------ one day

    /** The date's lessons: attendance state (✓ manual / 🤖 auto / ⏳ none) and substitutions. */
    @Transactional(readOnly = true)
    public Day day(long schoolId, LocalDate date) {
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        boolean schoolDay = weekday != null && schoolDays.isSchoolDay(schoolId, date);
        Map<Long, String> states = new HashMap<>();
        if (schoolDay) {
            for (Map<String, Object> r : jdbc.queryForList("""
                    select ls.id,
                           count(a.id) as marked,
                           count(a.id) filter (where a.source in ('MANUAL', 'DEVICE')) as real_marks
                      from lesson_slot ls
                      join school_class c on c.id = ls.class_id
                      join academic_year y on y.id = c.academic_year_id
                      left join attendance a on a.lesson_slot_id = ls.id and a.record_date = :d
                     where y.school_id = :s and ls.weekday = :w
                     group by ls.id""", p(schoolId).addValue("d", date).addValue("w", weekday))) {
                int marked = i(r.get("marked"));
                states.put(l(r.get("id")), marked == 0 ? "NONE" : i(r.get("real_marks")) > 0 ? "MANUAL" : "AUTO");
            }
        }
        List<Substitution> subs = substitutions.findInRange(schoolId, date, date).stream().map(TimetablePageService::view).toList();
        return new Day(date, weekday, schoolDay, LocalDateTime.now(clock).withNano(0), states, subs);
    }

    static Substitution view(LessonSubstitution s) {
        return new Substitution(s.getId(), s.getLessonSlot().getId(), s.getDate(), s.getSubstitute().getId(),
                fullName(s.getSubstitute()), s.getAbsentTeacher().getId(), fullName(s.getAbsentTeacher()), s.getReason());
    }

    static String fullName(Employee e) {
        return (e.getLastName() + " " + e.getFirstName()).trim();
    }

    // ------------------------------------------------------------------ substitutes

    /** Subjects that are close enough to stand in for each other. */
    private static final List<Set<String>> RELATED = List.of(
            Set.of("algebra", "geometriya", "matematika"),
            Set.of("ona tili", "adabiyot", "o'qish", "ona tili va o'qish savodxonligi"),
            Set.of("fizika", "kimyo", "biologiya", "tabiiy fanlar", "astronomiya"),
            Set.of("ingliz tili", "rus tili", "nemis tili", "fransuz tili", "xorijiy til"),
            Set.of("tarix", "jahon tarixi", "o'zbekiston tarixi", "huquq", "davlat va huquq asoslari", "tarbiya"),
            Set.of("informatika", "informatika va axborot texnologiyalari"),
            Set.of("jismoniy tarbiya", "chqbt"));

    static String match(String subject, Set<String> theirs) {
        String s = subject.toLowerCase(Locale.ROOT).trim();
        if (theirs.contains(s)) return "SAME";
        for (Set<String> group : RELATED) {
            if (group.contains(s) && theirs.stream().anyMatch(group::contains)) return "RELATED";
        }
        return "OTHER";
    }

    /**
     * Teachers free during the lesson on that date (no lesson of their own then, no substitution
     * then, not away), the same subject first, then a close one, then the rest; lighter load first.
     */
    @Transactional(readOnly = true)
    public Candidates candidates(long schoolId, long lessonSlotId, LocalDate date) {
        LessonSlot slot = slotOfSchool(schoolId, lessonSlotId);
        requireLessonOn(slot, date);
        List<Lesson> all = lessonsOf(schoolId);
        Set<Long> busy = new HashSet<>();
        Map<Long, Double> minutes = new HashMap<>();
        Map<Long, Integer> thatDay = new HashMap<>();
        Map<Long, Set<String>> subjectsOf = new HashMap<>();
        for (Lesson l : all) {
            minutes.merge(l.teacherId(), (double) Duration.between(l.start(), l.end()).toMinutes(), Double::sum);
            subjectsOf.computeIfAbsent(l.teacherId(), k -> new TreeSet<>()).add(l.subject().toLowerCase(Locale.ROOT).trim());
            if (!l.weekday().equals(slot.getWeekday())) continue;
            thatDay.merge(l.teacherId(), 1, Integer::sum);
            if (l.start().isBefore(slot.getEndTime()) && l.end().isAfter(slot.getStartTime())) busy.add(l.teacherId());
        }
        for (LessonSubstitution s : substitutions.findInRange(schoolId, date, date)) {
            LessonSlot other = s.getLessonSlot();
            if (other.getId().equals(slot.getId())) continue;
            if (other.getStartTime().isBefore(slot.getEndTime()) && other.getEndTime().isAfter(slot.getStartTime())) {
                busy.add(s.getSubstitute().getId());
            }
        }
        busy.add(slot.getEmployee().getId());
        String subject = slot.getSubject().getName();
        List<Candidate> out = new ArrayList<>();
        for (Employee e : employees.findBySchoolId(schoolId)) {
            if (busy.contains(e.getId()) || !minutes.containsKey(e.getId()) || e.isAwayOn(date)) continue;
            Set<String> theirs = subjectsOf.getOrDefault(e.getId(), Set.of());
            out.add(new Candidate(e.getId(), fullName(e), e.getPhone(), match(subject, theirs),
                    round1(minutes.get(e.getId()) / 60.0), thatDay.getOrDefault(e.getId(), 0), new ArrayList<>(theirs)));
        }
        Map<String, Integer> order = Map.of("SAME", 0, "RELATED", 1, "OTHER", 2);
        out.sort(Comparator.comparing((Candidate c) -> order.get(c.match())).thenComparingDouble(Candidate::hours)
                .thenComparing(Candidate::name));
        return new Candidates(slot.getId(), date, subject, fullName(slot.getEmployee()), slot.getStartTime(), slot.getEndTime(), out);
    }

    /** Saves (or replaces) the substitute for this one lesson on this one date; the timetable stays as it is. */
    @Transactional
    public Substitution substitute(long schoolId, long lessonSlotId, LocalDate date, long substituteId, String reason,
                                   String by) {
        LessonSlot slot = slotOfSchool(schoolId, lessonSlotId);
        requireLessonOn(slot, date);
        Employee sub = employees.findById(substituteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'qituvchi topilmadi"));
        if (sub.getSchool() == null || !sub.getSchool().getId().equals(schoolId)) {
            throw new ForbiddenException("Boshqa maktab o'qituvchisi");
        }
        if (sub.getId().equals(slot.getEmployee().getId())) {
            throw new IllegalStateException("O'rinbosar darsning o'z o'qituvchisi bo'la olmaydi");
        }
        boolean free = candidates(schoolId, lessonSlotId, date).candidates().stream().anyMatch(c -> c.id() == substituteId);
        if (!free) {
            throw new IllegalStateException(shortName(sub.getLastName(), sub.getFirstName()) + " bu vaqtda band yoki ishda emas");
        }
        LessonSubstitution s = substitutions.findByLessonSlotIdAndDate(lessonSlotId, date).orElseGet(LessonSubstitution::new);
        s.setLessonSlot(slot);
        s.setDate(date);
        s.setSubstitute(sub);
        s.setAbsentTeacher(slot.getEmployee());
        s.setReason(blankToNull(reason));
        s.setCreatedBy(by);
        s.setCreatedAt(LocalDateTime.now(clock));
        Substitution saved = view(substitutions.save(s));
        botCache.evictAllClasses();
        cache.evictSchool(schoolId);
        return saved;
    }

    @Transactional
    public void removeSubstitution(long schoolId, long id) {
        LessonSubstitution s = substitutions.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'rinbosar yozuvi topilmadi"));
        slotOfSchool(schoolId, s.getLessonSlot().getId());
        substitutions.delete(s);
        botCache.evictAllClasses();
        cache.evictSchool(schoolId);
    }

    private LessonSlot slotOfSchool(long schoolId, long lessonSlotId) {
        LessonSlot slot = lessons.findById(lessonSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars topilmadi"));
        if (!slot.getSchoolClass().getAcademicYear().getSchool().getId().equals(schoolId)) {
            throw new ForbiddenException("Boshqa maktab darsi");
        }
        return slot;
    }

    private static void requireLessonOn(LessonSlot slot, LocalDate date) {
        if (!slot.getWeekday().equals(SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek()))) {
            throw new IllegalStateException("Bu dars " + slot.getWeekday().toLowerCase(Locale.ROOT) + " kunlari — tanlangan sanada yo'q");
        }
    }
}
