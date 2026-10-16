package uz.azizbek.maktabboshqaruv.service.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import uz.azizbek.maktabboshqaruv.entity.AttendanceSource;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.event.AttendanceChangedEvent;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The attendance automation, one scheduler for all of it, every minute (Asia/Tashkent):
 * <ol>
 *   <li><b>DEMO_SIM attendance</b> — demo mode only ({@link DemoMode}): {@code delay} minutes into a
 *       lesson, students still without a mark get the simulated status of the day. Every lesson of
 *       today that started long enough ago is filled, so an app started at noon fills the morning
 *       too. In a school with 6+ teachers a few of them "forget" some lessons (see
 *       {@link DemoAttendancePlan#slackers});</li>
 *   <li><b>AUTO_CARRY</b> — {@code delay} minutes into a lesson, a student with no mark gets the
 *       status of their latest earlier lesson that day (late → present). No earlier mark (or the
 *       first lesson) → nothing is written: the student stays "olinmagan";</li>
 *   <li><b>DEMO_SIM grades</b> — demo mode only: when a lesson is over, 30–40 % of the students who
 *       came get a grade in its subject ({@link DemoGradePlan}); absent students never do;</li>
 *   <li><b>reminder</b> — {@code reminderMinutes} into a lesson without a real (MANUAL / DEVICE)
 *       mark, its teacher gets a note in the app.</li>
 * </ol>
 * Everything is insert-if-absent: MANUAL attendance and grades are never touched. A PostgreSQL
 * advisory lock keeps two app instances (or the startup fill and the scheduler) from running at once.
 */
@Service
public class AttendanceAutomation {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAutomation.class);
    private static final long LOCK_KEY = 0x4D41_4B54_4154_544EL; // "MAKTATTN"
    /** Below this many teachers nobody "forgets" lessons (small test schools stay complete). */
    private static final int SLACKERS_FROM_TEACHERS = 6;

    /** What one run did (for tests and the log). */
    public record Result(int carried, int simulated, int reminders, int grades, int lessons, int gradedLessons,
                         int skippedManual, boolean skippedLocked) {
        static Result locked() {
            return new Result(0, 0, 0, 0, 0, 0, 0, true);
        }

        static Result empty() {
            return new Result(0, 0, 0, 0, 0, 0, 0, false);
        }

        Result plus(Result o) {
            return new Result(carried + o.carried, simulated + o.simulated, reminders + o.reminders, grades + o.grades,
                    lessons + o.lessons, gradedLessons + o.gradedLessons, skippedManual + o.skippedManual, false);
        }
    }

    private record Lesson(long id, long classId, long employeeId, long subjectId, LocalTime start, LocalTime end,
                          String subject, String className, String teacher) {
    }

    private record Mark(AttendanceStatus status, AttendanceSource source) {
    }

    private static final String INSERT_GRADE = """
            insert into grade (student_id, subject_id, grade_date, score, type, comment, created_date, created_by,
                               source, lesson_slot_id)
            select ?, ?, ?, ?, ?, null, ?, ?, 'DEMO_SIM', ?
             where not exists (select 1 from grade g where g.student_id = ? and g.subject_id = ? and g.grade_date = ?
                                  and g.source = 'MANUAL')
            on conflict (lesson_slot_id, student_id, grade_date) where lesson_slot_id is not null do nothing""";

    private final JdbcTemplate jdbc;
    private final AttendanceWriter writer;
    private final AttendanceSettingsService settings;
    private final DemoMode demo;
    private final SchoolDays schoolDays;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final ApplicationEventPublisher events;

    public AttendanceAutomation(JdbcTemplate jdbc, AttendanceWriter writer, AttendanceSettingsService settings,
                                DemoMode demo, SchoolDays schoolDays, Clock clock,
                                PlatformTransactionManager txManager, ApplicationEventPublisher events) {
        this.tx = new TransactionTemplate(txManager);
        this.jdbc = jdbc;
        this.writer = writer;
        this.settings = settings;
        this.demo = demo;
        this.schoolDays = schoolDays;
        this.clock = clock;
        this.events = events;
    }

    @Scheduled(cron = "${attendance.automation-cron:5 * * * * *}", zone = "Asia/Tashkent")
    public void tick() {
        try {
            // one transaction for the whole pass: the advisory lock lives until it ends
            Result r = tx.execute(status -> pass());
            if (r == null || r.skippedLocked()) return;
            if (demo.enabled()) {
                log.info("SIM attendance: lessons={} inserted={} skippedManual={}", r.lessons(), r.simulated(), r.skippedManual());
                log.info("SIM grades: lessons={} inserted={}", r.gradedLessons(), r.grades());
            }
            if (r.carried() + r.reminders() > 0) {
                log.info("Davomat avtomatikasi: ko'chirildi {}, eslatma {}", r.carried(), r.reminders());
            }
        } catch (Exception e) {
            log.error("Davomat avtomatikasi ishlamadi", e);
        }
    }

    /** One pass over every school (also called by tests with a fixed clock). */
    @Transactional
    public Result runOnce() {
        return pass();
    }

    private Result pass() {
        if (!tryLock()) return Result.locked();
        Result total = Result.empty();
        for (Long schoolId : jdbc.queryForList("select id from school order by id", Long.class)) {
            total = total.plus(runForSchool(schoolId));
        }
        return total;
    }

    private boolean tryLock() {
        return Boolean.TRUE.equals(jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, LOCK_KEY));
    }

    /** One pass for one school (tests, and "Hozir to'ldirish"). */
    @Transactional
    public Result runFor(long schoolId) {
        return runForSchool(schoolId);
    }

    Result runForSchool(long schoolId) {
        LocalDateTime nowDt = LocalDateTime.now(clock);
        LocalDate today = nowDt.toLocalDate();
        if (!schoolDays.isSchoolDay(schoolId, today)) return Result.empty();
        Day day = day(schoolId, today);
        if (day.lessons.isEmpty()) return Result.empty();
        Result r = fill(schoolId, day, nowDt.toLocalTime());
        int reminders = remind(schoolId, today, nowDt.toLocalTime(), day, settings.forSchool(schoolId).reminderMinutes());
        return new Result(r.carried, r.simulated, reminders, r.grades, r.lessons, r.gradedLessons, r.skippedManual, false);
    }

    /** The lessons, students and marks of one school day — what every step below reads. */
    private final class Day {
        final LocalDate date;
        final List<Lesson> lessons;
        final Map<Long, List<Lesson>> byClass = new HashMap<>();
        final Map<Long, List<Long>> students;
        final Map<Long, Map<Long, Mark>> marks; // lesson -> student -> mark
        final Set<Long> slackers;
        final boolean gaps;

        Day(long schoolId, LocalDate date, List<Lesson> lessons, Map<Long, List<Long>> students) {
            this.date = date;
            this.lessons = lessons;
            this.students = students;
            for (Lesson l : lessons) byClass.computeIfAbsent(l.classId(), k -> new ArrayList<>()).add(l);
            byClass.values().forEach(list -> list.sort(Comparator.comparing(Lesson::start)));
            this.marks = marksOn(schoolId, date);
            Set<Long> teachers = new HashSet<>();
            for (Lesson l : lessons) teachers.add(l.employeeId());
            this.gaps = teachers.size() >= SLACKERS_FROM_TEACHERS;
            this.slackers = gaps ? DemoAttendancePlan.slackers(schoolId, teachers, yearStart(date)) : Set.of();
        }

        /** The teacher "forgot" this lesson (demo only, schools with 6+ teachers only). */
        boolean skipped(Lesson l) {
            return gaps && DemoAttendancePlan.skipped(l.employeeId(), l.id(), date, slackers.contains(l.employeeId()));
        }
    }

    private Day day(long schoolId, LocalDate date) {
        String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
        List<Lesson> lessons = weekday == null ? List.of() : lessonsOf(schoolId, weekday);
        return new Day(schoolId, date, lessons, lessons.isEmpty() ? Map.of() : studentsByClass(schoolId));
    }

    /**
     * Everything that fills a day up to {@code now}. In demo mode: carry from marks a person or a
     * device made first (a teacher's "absent" in lesson 2 follows the student to lesson 3), then
     * the simulator, then AUTO_CARRY for what is still empty (the lessons a teacher "forgot"), then
     * the grades of finished lessons. In real mode only AUTO_CARRY.
     */
    private Result fill(long schoolId, Day day, LocalTime now) {
        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        boolean carry = s.autoCarryEnabled();
        int carried = 0;
        int[] sim = new int[3];
        int[] grades = new int[2];
        if (demo.enabled()) {
            if (carry) carried += carry(schoolId, day, now, s.autoCarryDelayMinutes(), true);
            sim = simulate(schoolId, day, now, demo.delayMinutes());
        }
        if (carry) carried += carry(schoolId, day, now, s.autoCarryDelayMinutes(), false);
        if (demo.enabled()) grades = grade(schoolId, day, now);
        return new Result(carried, sim[0], 0, grades[0], sim[1], grades[1], sim[2], false);
    }

    /** realOnly: carry only from a MANUAL / DEVICE mark or a carry of one (never from a simulated mark). */
    private int carry(long schoolId, Day day, LocalTime now, int delay, boolean realOnly) {
        List<AttendanceWriter.AutoMark> out = new ArrayList<>();
        for (Map.Entry<Long, List<Lesson>> e : day.byClass.entrySet()) {
            List<Lesson> lessons = e.getValue();
            for (int i = 1; i < lessons.size(); i++) { // the first lesson has nothing to carry from
                Lesson lesson = lessons.get(i);
                if (now.isBefore(lesson.start().plusMinutes(delay))) continue;
                Map<Long, Mark> here = day.marks.computeIfAbsent(lesson.id(), k -> new HashMap<>());
                for (Long student : day.students.getOrDefault(e.getKey(), List.of())) {
                    if (here.containsKey(student)) continue;
                    for (int j = i - 1; j >= 0; j--) {
                        Mark earlier = day.marks.getOrDefault(lessons.get(j).id(), Map.of()).get(student);
                        if (earlier == null) continue;
                        // a chain of carries started at a real mark; a simulated mark is left to the simulator
                        if (realOnly && earlier.source() == AttendanceSource.DEMO_SIM) break;
                        AttendanceStatus carried = earlier.status() == AttendanceStatus.LATE ? AttendanceStatus.PRESENT : earlier.status();
                        out.add(new AttendanceWriter.AutoMark(lesson.id(), student, day.date, carried,
                                (j + 1) + "-darsdan ko'chirildi", null));
                        here.put(student, new Mark(carried, AttendanceSource.AUTO_CARRY));
                        break;
                    }
                }
            }
        }
        return writer.insertIfAbsent(schoolId, out, AttendanceSource.AUTO_CARRY);
    }

    private int remind(long schoolId, LocalDate today, LocalTime now, Day day, int minutes) {
        Timestamp at = Timestamp.valueOf(LocalDateTime.now(clock));
        int created = 0;
        for (Lesson l : day.lessons) {
            if (now.isBefore(l.start().plusMinutes(minutes)) || !now.isBefore(l.end())) continue;
            boolean taken = day.marks.getOrDefault(l.id(), Map.of()).values().stream()
                    .anyMatch(m -> m.source().isReal() || m.source() == AttendanceSource.DEMO_SIM);
            if (taken) continue;
            int number = day.byClass.get(l.classId()).indexOf(l) + 1;
            String text = l.className() + " · " + number + "-dars · " + l.subject() + " — davomat hali olinmagan";
            String link = "/attendance?lessonSlotId=" + l.id() + "&date=" + today;
            created += jdbc.update("""
                    insert into staff_notification (school_id, employee_id, lesson_slot_id, record_date, kind, text, link, created_at)
                    values (?, ?, ?, ?, 'ATTENDANCE_REMINDER', ?, ?, ?)
                    on conflict on constraint uk_staff_notification_lesson_day_kind do nothing""",
                    schoolId, l.employeeId(), l.id(), Date.valueOf(today), text, link, at);
        }
        return created;
    }

    /** Returns {inserted, lessons considered, places a MANUAL / DEVICE mark already held}. */
    private int[] simulate(long schoolId, Day day, LocalTime now, int delay) {
        Predicate<LocalDate> schoolDay = schoolDays.predicate(schoolId, day.date.minusDays(30), day.date);
        int yearStart = yearStart(day.date);
        List<AttendanceWriter.AutoMark> out = new ArrayList<>();
        int lessons = 0;
        int manual = 0;
        for (Map.Entry<Long, List<Lesson>> e : day.byClass.entrySet()) {
            List<Long> kids = day.students.getOrDefault(e.getKey(), List.of());
            Set<Long> chronic = DemoAttendancePlan.chronic(e.getKey(), kids, yearStart);
            List<Lesson> list = e.getValue();
            for (int i = 0; i < list.size(); i++) {
                Lesson lesson = list.get(i);
                if (now.isBefore(lesson.start().plusMinutes(delay)) || day.skipped(lesson)) continue;
                lessons++;
                Map<Long, Mark> here = day.marks.computeIfAbsent(lesson.id(), k -> new HashMap<>());
                for (Long student : kids) {
                    Mark existing = here.get(student);
                    if (existing != null) {
                        if (existing.source().isReal()) manual++;
                        continue;
                    }
                    DemoAttendancePlan.Day plan = DemoAttendancePlan.of(schoolId, student, day.date, chronic.contains(student), schoolDay);
                    AttendanceStatus status = DemoAttendancePlan.forLesson(plan, i == 0);
                    out.add(new AttendanceWriter.AutoMark(lesson.id(), student, day.date, status, "DEMO simulyatsiya",
                            status == AttendanceStatus.LATE ? plan.lateMinutes() : null));
                    here.put(student, new Mark(status, AttendanceSource.DEMO_SIM));
                }
            }
        }
        return new int[]{writer.insertIfAbsent(schoolId, out, AttendanceSource.DEMO_SIM), lessons, manual};
    }

    /**
     * Demo grades for the lessons over by {@code now}: only students marked present / late in that
     * lesson, never on a lesson the teacher "forgot", never where the teacher already graded the
     * student in that subject that day. Returns {inserted, lessons considered}.
     */
    private int[] grade(long schoolId, Day day, LocalTime now) {
        int yearStart = yearStart(day.date);
        Timestamp at = Timestamp.valueOf(LocalDateTime.now(clock));
        Date date = Date.valueOf(day.date);
        // the last lesson of each class and subject this week (a test happens there, ~5 % of weeks)
        Set<Long> lastOfWeek = lastLessonsOfWeek(schoolId);
        List<Object[]> batch = new ArrayList<>();
        int lessons = 0;
        for (Lesson lesson : day.lessons) {
            if (now.isBefore(lesson.end()) || day.skipped(lesson)) continue;
            lessons++;
            boolean test = lastOfWeek.contains(lesson.id()) && DemoGradePlan.testWeek(lesson.classId(), lesson.subjectId(), day.date);
            for (Map.Entry<Long, Mark> m : day.marks.getOrDefault(lesson.id(), Map.of()).entrySet()) {
                AttendanceStatus st = m.getValue().status();
                if (st != AttendanceStatus.PRESENT && st != AttendanceStatus.LATE) continue;
                long student = m.getKey();
                if (!test && !DemoGradePlan.graded(student, lesson.id(), day.date)) continue;
                int score = DemoGradePlan.score(schoolId, student, lesson.classId(), lesson.subject(), lesson.id(), day.date, yearStart);
                batch.add(new Object[]{student, lesson.subjectId(), date, score, test ? "EXAM" : "CURRENT", at,
                        lesson.teacher(), lesson.id(), student, lesson.subjectId(), date});
            }
        }
        int inserted = 0;
        for (int i = 0; i < batch.size(); i += 500) {
            for (int n : jdbc.batchUpdate(INSERT_GRADE, batch.subList(i, Math.min(i + 500, batch.size())))) {
                if (n > 0) inserted += n;
            }
        }
        // the home page cache keys grades on any school: one change event is enough
        if (inserted > 0) events.publishEvent(new AttendanceChangedEvent(schoolId));
        return new int[]{inserted, lessons};
    }

    private Set<Long> lastLessonsOfWeek(long schoolId) {
        return new HashSet<>(jdbc.queryForList("""
                select distinct on (ls.class_id, ls.subject_id) ls.id
                  from lesson_slot ls
                  join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = ?
                 order by ls.class_id, ls.subject_id,
                          case ls.weekday when 'Dushanba' then 1 when 'Seshanba' then 2 when 'Chorshanba' then 3
                                          when 'Payshanba' then 4 when 'Juma' then 5 else 6 end desc,
                          ls.start_time desc""", Long.class, schoolId));
    }

    // ------------------------------------------------------------------ demo: fill / clear

    /**
     * "Oxirgi 30 kunni to'ldirish": the last {@code days} school days before today, every lesson —
     * attendance, carry, grades — insert-if-absent (MANUAL stays). With {@code onlyMissing} a day
     * that already has demo grades is skipped (the startup fill). Returns attendance and grade rows
     * inserted, or null when another run holds the lock right now.
     */
    @Transactional
    public int[] backfillDemo(long schoolId, int days, boolean onlyMissing) {
        if (!demo.enabled()) throw new IllegalStateException("Demo rejim yoqilmagan (app.demo.enabled)");
        if (!tryLock()) return null;
        LocalDate today = LocalDate.now(clock);
        List<LocalDate> dates = schoolDays.lastSchoolDays(schoolId, today.minusDays(1), Math.max(1, Math.min(days, 60)));
        Set<LocalDate> done = new HashSet<>();
        if (onlyMissing && !dates.isEmpty()) {
            for (Date d : jdbc.queryForList("""
                    select distinct g.grade_date from grade g
                      join students st on st.id = g.student_id
                      join school_class c on c.id = st.class_id
                      join academic_year y on y.id = c.academic_year_id
                     where y.school_id = ? and g.source = 'DEMO_SIM' and g.grade_date >= ?""",
                    Date.class, schoolId, Date.valueOf(dates.get(0)))) {
                done.add(d.toLocalDate());
            }
        }
        int attendance = 0;
        int grades = 0;
        for (LocalDate date : dates) {
            if (done.contains(date)) continue;
            Day day = day(schoolId, date);
            if (day.lessons.isEmpty()) continue;
            Result r = fill(schoolId, day, LocalTime.MAX);
            attendance += r.simulated() + r.carried();
            grades += r.grades();
        }
        return new int[]{attendance, grades};
    }

    /** "Demo ma'lumotlarini tozalash" for grades: removes this school's DEMO_SIM grades only. */
    @Transactional
    public int deleteDemoGrades(long schoolId) {
        int n = jdbc.update("""
                delete from grade g using students st, school_class c, academic_year y
                 where g.student_id = st.id and st.class_id = c.id and c.academic_year_id = y.id
                   and y.school_id = ? and g.source = 'DEMO_SIM'""", schoolId);
        if (n > 0) events.publishEvent(new AttendanceChangedEvent(schoolId));
        return n;
    }

    // ------------------------------------------------------------------ reads (a few queries per school day)

    static int yearStart(LocalDate date) {
        return date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
    }

    private List<Lesson> lessonsOf(long schoolId, String weekday) {
        return jdbc.query("""
                select ls.id, ls.class_id, ls.employee_id, ls.subject_id, ls.start_time, ls.end_time, s.name,
                       c.grade_number || '-' || c.section_letter as class_name,
                       e.last_name || ' ' || left(coalesce(e.first_name, ''), 1) || '.' as teacher
                  from lesson_slot ls
                  join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                  join subject s on s.id = ls.subject_id
                  join employee e on e.id = ls.employee_id
                 where y.school_id = ? and ls.weekday = ?""",
                (rs, i) -> new Lesson(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getLong(4),
                        rs.getTime(5).toLocalTime(), rs.getTime(6).toLocalTime(), rs.getString(7), rs.getString(8),
                        rs.getString(9)),
                schoolId, weekday);
    }

    private Map<Long, List<Long>> studentsByClass(long schoolId) {
        Map<Long, List<Long>> map = new HashMap<>();
        jdbc.query("""
                select st.id, st.class_id from students st
                  join school_class c on c.id = st.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = ? order by st.id""",
                rs -> {
                    map.computeIfAbsent(rs.getLong(2), k -> new ArrayList<>()).add(rs.getLong(1));
                }, schoolId);
        return map;
    }

    private Map<Long, Map<Long, Mark>> marksOn(long schoolId, LocalDate date) {
        Map<Long, Map<Long, Mark>> map = new HashMap<>();
        jdbc.query("""
                select a.lesson_slot_id, a.student_id, a.status, a.source from attendance a
                  join lesson_slot ls on ls.id = a.lesson_slot_id
                  join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                 where y.school_id = ? and a.record_date = ?""",
                rs -> {
                    map.computeIfAbsent(rs.getLong(1), k -> new HashMap<>()).put(rs.getLong(2),
                            new Mark(AttendanceStatus.valueOf(rs.getString(3)), AttendanceSource.valueOf(rs.getString(4))));
                }, schoolId, Date.valueOf(date));
        return map;
    }
}
