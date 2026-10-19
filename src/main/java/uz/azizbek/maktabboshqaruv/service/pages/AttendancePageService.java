package uz.azizbek.maktabboshqaruv.service.pages;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.entity.LessonSlot;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.LessonSlotRepository;
import uz.azizbek.maktabboshqaruv.service.attendance.AttendanceWriter;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoMode;
import uz.azizbek.maktabboshqaruv.service.attendance.SchoolDays;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * "Davomat olish": a class's lessons on the CHOSEN DATE (its weekday only — Monday shows Monday's
 * lessons), each with its attendance state, every class's state for the class picker, a lesson's
 * roster with each student's last 10 school days and warnings, and the save (only the rows that
 * changed; every saved row becomes MANUAL).
 */
@Service
public class AttendancePageService {

    /** School days of history next to each student. */
    public static final int HISTORY_DAYS = 10;

    public record Lesson(long lessonSlotId, int n, LocalTime start, LocalTime end, long subjectId, String subject,
                         String subjectColor, long teacherId, String teacher, String teacherShort, String room,
                         String state, int marked, int auto, int students) {
    }

    public record Day(LocalDate date, String weekday, boolean schoolDay, String dayOff, boolean future,
                      LocalDateTime now, boolean demo, long classId, List<Lesson> lessons,
                      Map<Long, String> classStates) {
    }

    public record Mark(LocalDate date, String status) {
    }

    public record Row(long studentId, String name, String guardianName, String guardianRelation, String guardianPhone,
                      boolean telegram, String status, String source, String autoNote, LocalDateTime autoAt,
                      Integer lateMinutes, String comment, List<Mark> history, int absentStreak, int lateThisWeek) {
    }

    public record Roster(long lessonSlotId, LocalDate date, String className, String subject, LocalTime start,
                         LocalTime end, boolean demo, List<Row> rows) {
    }

    public record Entry(Long studentId, AttendanceStatus status, String comment, Integer lateMinutes) {
    }

    public record SaveRequest(Long lessonSlotId, LocalDate date, List<Entry> entries) {
    }

    public record Saved(int saved, LocalDateTime savedAt) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final LessonSlotRepository lessons;
    private final AttendanceWriter writer;
    private final SchoolDays schoolDays;
    private final DemoMode demo;
    private final Clock clock;

    public AttendancePageService(NamedParameterJdbcTemplate jdbc, LessonSlotRepository lessons, AttendanceWriter writer,
                                 SchoolDays schoolDays, DemoMode demo, Clock clock) {
        this.jdbc = jdbc;
        this.lessons = lessons;
        this.writer = writer;
        this.schoolDays = schoolDays;
        this.demo = demo;
        this.clock = clock;
    }

    private static MapSqlParameterSource p(long schoolId) {
        return new MapSqlParameterSource("s", schoolId);
    }

    /** The weekday name lesson_slot uses for a date; null on Sunday. */
    public static String weekdayOf(LocalDate date) {
        return SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
    }

    // ------------------------------------------------------------------ the day

    @Transactional(readOnly = true)
    public Day day(long schoolId, long classId, LocalDate date) {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        String weekday = weekdayOf(date);
        String dayOff = date.getDayOfWeek() == DayOfWeek.SUNDAY ? "Yakshanba" : holidayName(schoolId, date);
        boolean schoolDay = weekday != null && dayOff == null;
        boolean future = date.isAfter(now.toLocalDate());

        // every lesson of the school on that weekday (the picker needs every class), with its marks
        List<Map<String, Object>> rows = weekday == null ? List.of() : jdbc.queryForList("""
                select ls.id, ls.class_id, ls.start_time, ls.end_time, ls.subject_id, s.name as subject, s.color,
                       ls.employee_id, e.first_name, e.last_name, r.room_number,
                       (select count(*) from students st where st.class_id = ls.class_id) as students,
                       (select count(*) from attendance a where a.lesson_slot_id = ls.id and a.record_date = :d) as marked,
                       (select count(*) from attendance a where a.lesson_slot_id = ls.id and a.record_date = :d
                           and a.source in ('MANUAL', 'DEVICE')) as real_marks,
                       (select count(*) from attendance a where a.lesson_slot_id = ls.id and a.record_date = :d
                           and a.source in ('AUTO_CARRY', 'DEMO_SIM')) as auto_marks
                  from lesson_slot ls
                  join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                  join subject s on s.id = ls.subject_id
                  join employee e on e.id = ls.employee_id
                  left join room r on r.id = ls.room_id
                 where y.school_id = :s and ls.weekday = :w
                 order by ls.class_id, ls.start_time""", p(schoolId).addValue("w", weekday).addValue("d", date));

        Map<Long, List<Map<String, Object>>> byClass = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) byClass.computeIfAbsent(l(r.get("class_id")), k -> new ArrayList<>()).add(r);

        List<Lesson> out = new ArrayList<>();
        int n = 0;
        for (Map<String, Object> r : byClass.getOrDefault(classId, List.of())) {
            int students = i(r.get("students"));
            int marked = i(r.get("marked"));
            int real = i(r.get("real_marks"));
            int auto = i(r.get("auto_marks"));
            String state = marked == 0 ? "NONE" : real >= Math.max(1, students) ? "MANUAL"
                    : real > 0 ? "PARTIAL" : "AUTO";
            out.add(new Lesson(l(r.get("id")), ++n, time(r.get("start_time")), time(r.get("end_time")),
                    l(r.get("subject_id")), (String) r.get("subject"), (String) r.get("color"), l(r.get("employee_id")),
                    ((String) r.get("last_name") + " " + r.get("first_name")).trim(),
                    shortName((String) r.get("last_name"), (String) r.get("first_name")), (String) r.get("room_number"),
                    state, marked, auto, students));
        }

        // the picker's dot per class: lessons already started that day all taken (FULL), some (PARTIAL), none
        Map<Long, String> states = new LinkedHashMap<>();
        LocalTime cut = date.equals(now.toLocalDate()) ? now.toLocalTime() : LocalTime.MAX;
        if (schoolDay && !future) {
            byClass.forEach((cid, list) -> {
                int started = 0, taken = 0;
                for (Map<String, Object> r : list) {
                    if (time(r.get("start_time")).isAfter(cut)) continue;
                    started++;
                    if (i(r.get("marked")) * 2 >= Math.max(1, i(r.get("students")))) taken++;
                }
                states.put(cid, started == 0 ? "NONE" : taken == started ? "FULL" : taken > 0 ? "PARTIAL" : "NONE");
            });
        }
        return new Day(date, weekday, schoolDay, dayOff, future, now, demo.enabled(), classId, out, states);
    }

    private String holidayName(long schoolId, LocalDate date) {
        List<String> names = jdbc.queryForList("""
                select title from calendar_event where school_id = :s and type in ('HOLIDAY', 'VACATION')
                   and :d between start_date and end_date order by start_date limit 1""",
                p(schoolId).addValue("d", date), String.class);
        return names.isEmpty() ? null : names.get(0);
    }

    // ------------------------------------------------------------------ the roster

    @Transactional(readOnly = true)
    public Roster roster(long lessonSlotId, LocalDate date, boolean withPhones) {
        LessonSlot slot = lessons.findById(lessonSlotId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars mavjud emas"));
        long schoolId = slot.getSchoolClass().getAcademicYear().getSchool().getId();
        long classId = slot.getSchoolClass().getId();

        Map<Long, Map<String, Object>> marks = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select student_id, status, source, auto_note, auto_at, late_minutes, comment from attendance
                 where lesson_slot_id = :l and record_date = :d""", new MapSqlParameterSource("l", lessonSlotId).addValue("d", date))) {
            marks.put(l(r.get("student_id")), r);
        }

        // the last school days before the date: each student's status that day (their latest lesson)
        List<LocalDate> days = schoolDays.lastSchoolDays(schoolId, date.minusDays(1), HISTORY_DAYS);
        Map<Long, Map<LocalDate, String>> history = new HashMap<>();
        if (!days.isEmpty()) {
            for (Map<String, Object> r : jdbc.queryForList("""
                    select distinct on (a.record_date, a.student_id) a.record_date as d, a.student_id, a.status
                      from attendance a join lesson_slot ls on ls.id = a.lesson_slot_id
                     where ls.class_id = :c and a.record_date between :from and :to
                     order by a.record_date, a.student_id, ls.start_time desc""",
                    new MapSqlParameterSource("c", classId).addValue("from", days.get(0)).addValue("to", days.get(days.size() - 1)))) {
                history.computeIfAbsent(l(r.get("student_id")), k -> new HashMap<>()).put(date(r.get("d")), (String) r.get("status"));
            }
        }
        LocalDate monday = date.with(DayOfWeek.MONDAY);

        List<Row> rows = jdbc.query("""
                select st.id, st.first_name, st.last_name, st.guardian_name, st.guardian_relation, st.guardian_phone,
                       exists (select 1 from parent_telegram_link t where t.student_id = st.id and t.active) as telegram
                  from students st where st.class_id = :c order by st.last_name, st.first_name""",
                new MapSqlParameterSource("c", classId), (rs, k) -> {
                    long id = rs.getLong("id");
                    Map<String, Object> m = marks.get(id);
                    Map<LocalDate, String> h = history.getOrDefault(id, Map.of());
                    List<Mark> list = new ArrayList<>();
                    for (LocalDate d : days) list.add(new Mark(d, h.get(d)));
                    int streak = 0;
                    for (int j = days.size() - 1; j >= 0 && "ABSENT".equals(h.get(days.get(j))); j--) streak++;
                    int late = 0;
                    for (LocalDate d : days) if (!d.isBefore(monday) && "LATE".equals(h.get(d))) late++;
                    Object autoAt = m == null ? null : m.get("auto_at");
                    return new Row(id, (rs.getString("first_name") + " " + rs.getString("last_name")).trim(),
                            rs.getString("guardian_name"), rs.getString("guardian_relation"),
                            withPhones ? rs.getString("guardian_phone") : null, rs.getBoolean("telegram"),
                            m == null ? null : (String) m.get("status"), m == null ? null : (String) m.get("source"),
                            m == null ? null : (String) m.get("auto_note"),
                            autoAt instanceof java.sql.Timestamp ts ? ts.toLocalDateTime() : null,
                            m == null || m.get("late_minutes") == null ? null : i(m.get("late_minutes")),
                            m == null ? null : (String) m.get("comment"), list, streak, late);
                });
        return new Roster(lessonSlotId, date, slot.getSchoolClass().getGradeNumber() + "-" + slot.getSchoolClass().getSectionLetter(),
                slot.getSubject().getName(), slot.getStartTime(), slot.getEndTime(), demo.enabled(), rows);
    }

    // ------------------------------------------------------------------ save

    /**
     * Saves the given rows (a few at a time — the page saves as you go). The date must be a school
     * day, not in the future, and the lesson's own weekday (no Wednesday lesson on a Monday).
     */
    @Transactional
    public Saved save(long schoolId, SaveRequest r) {
        if (r.lessonSlotId() == null || r.date() == null || r.entries() == null) {
            throw new IllegalStateException("Dars, sana va belgilar kerak");
        }
        LessonSlot slot = lessons.findById(r.lessonSlotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday dars mavjud emas"));
        long own = slot.getSchoolClass().getAcademicYear().getSchool().getId();
        if (own != schoolId) throw new ForbiddenException("Boshqa maktab darsi");
        LocalDate today = LocalDate.now(clock);
        if (r.date().isAfter(today)) throw new IllegalStateException("Kelajak sanaga davomat qo'yib bo'lmaydi");
        if (r.date().getDayOfWeek() == DayOfWeek.SUNDAY) throw new IllegalStateException("Yakshanba — maktab ishlamaydi");
        if (!slot.getWeekday().equals(weekdayOf(r.date()))) {
            throw new IllegalStateException("Bu dars " + slot.getWeekday().toLowerCase(Locale.ROOT)
                    + " kunlari — tanlangan sana " + weekdayOf(r.date()).toLowerCase(Locale.ROOT));
        }
        Set<Long> ofClass = new HashSet<>(jdbc.queryForList("select id from students where class_id = :c",
                new MapSqlParameterSource("c", slot.getSchoolClass().getId()), Long.class));
        List<AttendanceWriter.ManualMark> marks = new ArrayList<>();
        for (Entry e : r.entries()) {
            if (e.studentId() == null || e.status() == null) throw new IllegalStateException("O'quvchi va holat kerak");
            if (!ofClass.contains(e.studentId())) throw new IllegalStateException("O'quvchi bu sinfda emas: " + e.studentId());
            Integer late = e.status() == AttendanceStatus.LATE && e.lateMinutes() != null
                    ? Math.max(1, Math.min(240, e.lateMinutes())) : null;
            marks.add(new AttendanceWriter.ManualMark(e.studentId(), e.status(), e.comment(), late));
        }
        if (!marks.isEmpty()) writer.saveManual(own, slot.getId(), r.date(), marks, currentUser());
        return new Saved(marks.size(), LocalDateTime.now(clock).withNano(0));
    }

    private static String currentUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? null : auth.getName();
    }
}
