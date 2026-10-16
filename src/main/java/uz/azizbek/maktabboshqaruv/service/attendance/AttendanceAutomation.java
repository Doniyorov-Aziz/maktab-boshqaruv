package uz.azizbek.maktabboshqaruv.service.attendance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.AttendanceSource;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The attendance automation, one scheduler for all of it, every minute (Asia/Tashkent):
 * <ol>
 *   <li><b>AUTO_CARRY</b> — {@code delay} minutes into a lesson, a student with no mark gets the
 *       status of their latest earlier lesson that day (late → present). No earlier mark (or the
 *       first lesson) → nothing is written: the student stays "olinmagan";</li>
 *   <li><b>reminder</b> — {@code reminderMinutes} into a lesson without a real (MANUAL / DEVICE)
 *       mark, its teacher gets a note in the app;</li>
 *   <li><b>DEMO_SIM</b> — demo installations only (DemoSimulatorSwitch): {@code delay} minutes into a
 *       lesson, students still without a mark get the simulated status of the day.</li>
 * </ol>
 * Everything is insert-if-absent through AttendanceWriter: MANUAL marks are never touched. A
 * PostgreSQL advisory lock keeps two app instances from running the same minute twice.
 */
@Service
public class AttendanceAutomation {

    private static final Logger log = LoggerFactory.getLogger(AttendanceAutomation.class);
    private static final long LOCK_KEY = 0x4D41_4B54_4154_544EL; // "MAKTATTN"

    /** What one run did (for tests and the log). */
    public record Result(int carried, int simulated, int reminders, boolean skippedLocked) {
        static Result locked() {
            return new Result(0, 0, 0, true);
        }
    }

    private record Lesson(long id, long classId, long employeeId, LocalTime start, LocalTime end, String subject,
                          String className) {
    }

    private record Mark(AttendanceStatus status, AttendanceSource source) {
    }

    private final JdbcTemplate jdbc;
    private final AttendanceWriter writer;
    private final AttendanceSettingsService settings;
    private final DemoSimulatorSwitch demo;
    private final SchoolDays schoolDays;
    private final Clock clock;
    private final org.springframework.transaction.support.TransactionTemplate tx;

    public AttendanceAutomation(JdbcTemplate jdbc, AttendanceWriter writer, AttendanceSettingsService settings,
                                DemoSimulatorSwitch demo, SchoolDays schoolDays, Clock clock,
                                org.springframework.transaction.PlatformTransactionManager txManager) {
        this.tx = new org.springframework.transaction.support.TransactionTemplate(txManager);
        this.jdbc = jdbc;
        this.writer = writer;
        this.settings = settings;
        this.demo = demo;
        this.schoolDays = schoolDays;
        this.clock = clock;
    }

    @Scheduled(cron = "${attendance.automation-cron:5 * * * * *}", zone = "Asia/Tashkent")
    public void tick() {
        try {
            // one transaction for the whole pass: the advisory lock lives until it ends
            Result r = tx.execute(status -> pass());
            if (r.carried() + r.simulated() + r.reminders() > 0) {
                log.info("Davomat avtomatikasi: ko'chirildi {}, demo {}, eslatma {}", r.carried(), r.simulated(), r.reminders());
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
        Boolean got = jdbc.queryForObject("select pg_try_advisory_xact_lock(?)", Boolean.class, LOCK_KEY);
        if (!Boolean.TRUE.equals(got)) return Result.locked();
        int carried = 0;
        int simulated = 0;
        int reminders = 0;
        for (Long schoolId : jdbc.queryForList("select id from school order by id", Long.class)) {
            Result r = runForSchool(schoolId);
            carried += r.carried();
            simulated += r.simulated();
            reminders += r.reminders();
        }
        return new Result(carried, simulated, reminders, false);
    }

    Result runForSchool(long schoolId) {
        LocalDateTime nowDt = LocalDateTime.now(clock);
        LocalDate today = nowDt.toLocalDate();
        LocalTime now = nowDt.toLocalTime();
        if (!schoolDays.isSchoolDay(schoolId, today)) return new Result(0, 0, 0, false);
        String weekday = SchoolDays.WEEKDAY_UZ.get(today.getDayOfWeek());

        List<Lesson> lessons = lessonsOf(schoolId, weekday);
        if (lessons.isEmpty()) return new Result(0, 0, 0, false);
        Map<Long, List<Lesson>> byClass = new HashMap<>();
        for (Lesson l : lessons) byClass.computeIfAbsent(l.classId(), k -> new ArrayList<>()).add(l);
        byClass.values().forEach(list -> list.sort(Comparator.comparing(Lesson::start)));
        Map<Long, List<Long>> students = studentsByClass(schoolId);
        Map<Long, Map<Long, Mark>> marks = marksOn(schoolId, today); // lesson -> student -> mark

        AttendanceSettingsService.Settings s = settings.forSchool(schoolId);
        int carried = s.autoCarryEnabled() ? carry(schoolId, today, now, byClass, students, marks, s.autoCarryDelayMinutes()) : 0;
        int reminders = remind(schoolId, today, now, lessons, marks, s.reminderMinutes());
        int simulated = demo.enabled() ? simulate(schoolId, today, now, byClass, students, marks, demo.delayMinutes()) : 0;
        return new Result(carried, simulated, reminders, false);
    }

    private int carry(long schoolId, LocalDate today, LocalTime now, Map<Long, List<Lesson>> byClass,
                      Map<Long, List<Long>> students, Map<Long, Map<Long, Mark>> marks, int delay) {
        List<AttendanceWriter.AutoMark> out = new ArrayList<>();
        for (Map.Entry<Long, List<Lesson>> e : byClass.entrySet()) {
            List<Lesson> day = e.getValue();
            for (int i = 1; i < day.size(); i++) { // the first lesson has nothing to carry from
                Lesson lesson = day.get(i);
                if (now.isBefore(lesson.start().plusMinutes(delay))) continue;
                Map<Long, Mark> here = marks.computeIfAbsent(lesson.id(), k -> new HashMap<>());
                for (Long student : students.getOrDefault(e.getKey(), List.of())) {
                    if (here.containsKey(student)) continue;
                    for (int j = i - 1; j >= 0; j--) {
                        Mark earlier = marks.getOrDefault(day.get(j).id(), Map.of()).get(student);
                        if (earlier == null) continue;
                        AttendanceStatus carried = earlier.status() == AttendanceStatus.LATE ? AttendanceStatus.PRESENT : earlier.status();
                        out.add(new AttendanceWriter.AutoMark(lesson.id(), student, today, carried,
                                (j + 1) + "-darsdan ko'chirildi", null));
                        here.put(student, new Mark(carried, AttendanceSource.AUTO_CARRY));
                        break;
                    }
                }
            }
        }
        return writer.insertIfAbsent(schoolId, out, AttendanceSource.AUTO_CARRY);
    }

    private int remind(long schoolId, LocalDate today, LocalTime now, List<Lesson> lessons,
                       Map<Long, Map<Long, Mark>> marks, int minutes) {
        Map<Long, List<Lesson>> byClass = new HashMap<>();
        for (Lesson l : lessons) byClass.computeIfAbsent(l.classId(), k -> new ArrayList<>()).add(l);
        byClass.values().forEach(list -> list.sort(Comparator.comparing(Lesson::start)));
        Timestamp at = Timestamp.valueOf(LocalDateTime.now(clock));
        int created = 0;
        for (Lesson l : lessons) {
            if (now.isBefore(l.start().plusMinutes(minutes)) || !now.isBefore(l.end())) continue;
            boolean taken = marks.getOrDefault(l.id(), Map.of()).values().stream().anyMatch(m -> m.source().isReal());
            if (taken) continue;
            int number = byClass.get(l.classId()).indexOf(l) + 1;
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

    private int simulate(long schoolId, LocalDate today, LocalTime now, Map<Long, List<Lesson>> byClass,
                         Map<Long, List<Long>> students, Map<Long, Map<Long, Mark>> marks, int delay) {
        Predicate<LocalDate> schoolDay = schoolDays.predicate(schoolId, today.minusDays(30), today);
        int yearStart = today.getMonthValue() >= 9 ? today.getYear() : today.getYear() - 1;
        List<AttendanceWriter.AutoMark> out = new ArrayList<>();
        for (Map.Entry<Long, List<Lesson>> e : byClass.entrySet()) {
            List<Long> kids = students.getOrDefault(e.getKey(), List.of());
            Set<Long> chronic = DemoAttendancePlan.chronic(e.getKey(), kids, yearStart);
            List<Lesson> day = e.getValue();
            for (int i = 0; i < day.size(); i++) {
                Lesson lesson = day.get(i);
                if (now.isBefore(lesson.start().plusMinutes(delay))) continue;
                Map<Long, Mark> here = marks.computeIfAbsent(lesson.id(), k -> new HashMap<>());
                for (Long student : kids) {
                    if (here.containsKey(student)) continue;
                    DemoAttendancePlan.Day plan = DemoAttendancePlan.of(schoolId, student, today, chronic.contains(student), schoolDay);
                    AttendanceStatus status = DemoAttendancePlan.forLesson(plan, i == 0);
                    out.add(new AttendanceWriter.AutoMark(lesson.id(), student, today, status, "DEMO simulyatsiya",
                            status == AttendanceStatus.LATE ? plan.lateMinutes() : null));
                    here.put(student, new Mark(status, AttendanceSource.DEMO_SIM));
                }
            }
        }
        return writer.insertIfAbsent(schoolId, out, AttendanceSource.DEMO_SIM);
    }

    /**
     * "Demo: oxirgi 30 kunni to'ldirish": the same plan for the last {@code days} school days before
     * today, every lesson, insert-if-absent (MANUAL marks stay). Returns rows inserted.
     */
    @Transactional
    public int backfillDemo(long schoolId, int days) {
        if (!demo.enabled()) throw new IllegalStateException("Demo simulyator yoqilmagan (app.demo.attendance-simulator.enabled)");
        LocalDate today = LocalDate.now(clock);
        List<LocalDate> dates = schoolDays.lastSchoolDays(schoolId, today.minusDays(1), Math.max(1, Math.min(days, 60)));
        if (dates.isEmpty()) return 0;
        Predicate<LocalDate> schoolDay = schoolDays.predicate(schoolId, dates.get(0).minusDays(30), today);
        Map<Long, List<Long>> students = studentsByClass(schoolId);
        Map<String, List<Lesson>> lessonsByWeekday = new HashMap<>();
        int inserted = 0;
        for (LocalDate date : dates) {
            String weekday = SchoolDays.WEEKDAY_UZ.get(date.getDayOfWeek());
            List<Lesson> lessons = lessonsByWeekday.computeIfAbsent(weekday, w -> lessonsOf(schoolId, w));
            Map<Long, List<Lesson>> byClass = new HashMap<>();
            for (Lesson l : lessons) byClass.computeIfAbsent(l.classId(), k -> new ArrayList<>()).add(l);
            int yearStart = date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
            List<AttendanceWriter.AutoMark> out = new ArrayList<>();
            for (Map.Entry<Long, List<Lesson>> e : byClass.entrySet()) {
                List<Lesson> day = e.getValue();
                day.sort(Comparator.comparing(Lesson::start));
                List<Long> kids = students.getOrDefault(e.getKey(), List.of());
                Set<Long> chronic = DemoAttendancePlan.chronic(e.getKey(), kids, yearStart);
                for (Long student : kids) {
                    DemoAttendancePlan.Day plan = DemoAttendancePlan.of(schoolId, student, date, chronic.contains(student), schoolDay);
                    for (int i = 0; i < day.size(); i++) {
                        AttendanceStatus status = DemoAttendancePlan.forLesson(plan, i == 0);
                        out.add(new AttendanceWriter.AutoMark(day.get(i).id(), student, date, status, "DEMO simulyatsiya",
                                status == AttendanceStatus.LATE ? plan.lateMinutes() : null));
                    }
                }
            }
            inserted += writer.insertIfAbsent(schoolId, out, AttendanceSource.DEMO_SIM);
        }
        return inserted;
    }

    // ------------------------------------------------------------------ reads (3 queries per school)

    private List<Lesson> lessonsOf(long schoolId, String weekday) {
        return jdbc.query("""
                select ls.id, ls.class_id, ls.employee_id, ls.start_time, ls.end_time, s.name,
                       c.grade_number || '-' || c.section_letter as class_name
                  from lesson_slot ls
                  join school_class c on c.id = ls.class_id
                  join academic_year y on y.id = c.academic_year_id
                  join subject s on s.id = ls.subject_id
                 where y.school_id = ? and ls.weekday = ?""",
                (rs, i) -> new Lesson(rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getTime(4).toLocalTime(),
                        rs.getTime(5).toLocalTime(), rs.getString(6), rs.getString(7)),
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
