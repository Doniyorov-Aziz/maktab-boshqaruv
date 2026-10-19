package uz.azizbek.maktabboshqaruv.service.attendance;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.AttendanceSource;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;
import uz.azizbek.maktabboshqaruv.event.AttendanceChangedEvent;
import uz.azizbek.maktabboshqaruv.event.AttendanceMarkedEvent;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The one place attendance rows are written, and so the one place the priority rule lives:
 * <ol>
 *   <li>a MANUAL mark is never changed or deleted by an automatic process;</li>
 *   <li>automatic processes (AUTO_CARRY, DEMO_SIM) only insert where there is no row yet —
 *       {@code INSERT … ON CONFLICT DO NOTHING} on the (lesson, student, date) unique key, so two
 *       writers at once can never produce a duplicate;</li>
 *   <li>a staff save is an upsert that always ends as MANUAL: it overwrites an automatic mark,
 *       and if the scheduler inserted a row a moment earlier the staff value still wins;</li>
 *   <li>a DEVICE arrival replaces an automatic mark but never a MANUAL one.</li>
 * </ol>
 * Parents are told about ABSENT / LATE only for marks a person or a device made (and for
 * AUTO_CARRY when the school turned on notifyOnAuto); DEMO_SIM never notifies anyone.
 */
@Service
public class AttendanceWriter {

    private static final String UPSERT_MANUAL = """
            insert into attendance (lesson_slot_id, student_id, record_date, status, comment, source,
                                    created_by, auto_at, late_minutes, auto_note, created_date)
            values (?, ?, ?, ?, ?, 'MANUAL', ?, null, ?, null, ?)
            on conflict (lesson_slot_id, student_id, record_date) do update
               set status = excluded.status, comment = excluded.comment, source = 'MANUAL',
                   created_by = excluded.created_by, auto_at = null, late_minutes = excluded.late_minutes, auto_note = null
            returning id""";

    private static final String INSERT_IF_ABSENT = """
            insert into attendance (lesson_slot_id, student_id, record_date, status, comment, source,
                                    created_by, auto_at, late_minutes, auto_note, created_date)
            values (?, ?, ?, ?, null, ?, null, ?, ?, ?, ?)
            on conflict (lesson_slot_id, student_id, record_date) do nothing""";

    private static final String UPSERT_DEVICE = """
            insert into attendance (lesson_slot_id, student_id, record_date, status, comment, source,
                                    created_by, auto_at, late_minutes, auto_note, created_date)
            values (?, ?, ?, ?, null, 'DEVICE', null, ?, ?, ?, ?)
            on conflict (lesson_slot_id, student_id, record_date) do update
               set status = excluded.status, source = 'DEVICE', auto_at = excluded.auto_at,
                   late_minutes = excluded.late_minutes, auto_note = excluded.auto_note
             where attendance.source <> 'MANUAL'
            returning id""";

    /** A staff member's mark for one student. */
    /** lateMinutes: how late (LATE only, else null) — the "5 / 10 / 15 / 20+ daq" of the page. */
    public record ManualMark(long studentId, AttendanceStatus status, String comment, Integer lateMinutes) {
        public ManualMark(long studentId, AttendanceStatus status, String comment) {
            this(studentId, status, comment, null);
        }
    }

    /** An automatic mark for one student and lesson; note says why ("2-darsdan ko'chirildi"). */
    public record AutoMark(long lessonSlotId, long studentId, LocalDate date, AttendanceStatus status,
                           String note, Integer lateMinutes) {
    }

    private record Previous(AttendanceStatus status, AttendanceSource source) {
    }

    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final AttendanceSettingsService settings;
    private final Clock clock;

    public AttendanceWriter(JdbcTemplate jdbc, ApplicationEventPublisher events,
                            AttendanceSettingsService settings, Clock clock) {
        this.jdbc = jdbc;
        this.events = events;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * A staff save: every mark becomes MANUAL (insert or overwrite, whatever the row was).
     * Returns the row ids in the order of {@code marks}.
     */
    @Transactional
    public List<Long> saveManual(long schoolId, long lessonSlotId, LocalDate date, List<ManualMark> marks, String username) {
        boolean notifyOnAuto = settings.forSchool(schoolId).notifyOnAuto();
        Map<Long, Previous> before = previous(lessonSlotId, date);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
        List<Long> ids = new ArrayList<>(marks.size());
        for (ManualMark m : marks) {
            Long id = jdbc.queryForObject(UPSERT_MANUAL, Long.class, lessonSlotId, m.studentId(), Date.valueOf(date),
                    m.status().name(), blankToNull(m.comment()), username,
                    m.status() == AttendanceStatus.LATE ? m.lateMinutes() : null, now);
            ids.add(id);
            publishIfNotifiable(id, before.get(m.studentId()), m.status(), notifyOnAuto);
        }
        events.publishEvent(new AttendanceChangedEvent(schoolId));
        return ids;
    }

    /**
     * Automatic marks (AUTO_CARRY / DEMO_SIM): inserted only where no row exists. Returns how
     * many rows were actually inserted. MANUAL and DEVICE rows stay exactly as they were.
     */
    @Transactional
    public int insertIfAbsent(long schoolId, List<AutoMark> marks, AttendanceSource source) {
        if (!source.isAutomatic()) throw new IllegalArgumentException("Faqat avtomatik manba: " + source);
        if (marks.isEmpty()) return 0;
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
        boolean notify = source == AttendanceSource.AUTO_CARRY && settings.forSchool(schoolId).notifyOnAuto();
        int inserted = 0;
        if (notify) {
            // row by row: the ids of what was inserted are needed for the parent notifications
            for (AutoMark m : marks) {
                List<Long> id = jdbc.queryForList(INSERT_IF_ABSENT + " returning id", Long.class, args(m, source, now));
                if (!id.isEmpty()) {
                    inserted++;
                    publishIfNotifiable(id.get(0), null, m.status(), true);
                }
            }
        } else {
            List<Object[]> batch = marks.stream().map(m -> args(m, source, now)).toList();
            for (int i = 0; i < batch.size(); i += 500) {
                for (int n : jdbc.batchUpdate(INSERT_IF_ABSENT, batch.subList(i, Math.min(i + 500, batch.size())))) {
                    if (n > 0) inserted += n;
                }
            }
        }
        if (inserted > 0) events.publishEvent(new AttendanceChangedEvent(schoolId));
        return inserted;
    }

    /**
     * A device arrival for one lesson: inserted, or replaces an automatic mark; a MANUAL mark is
     * left alone (returns null then).
     */
    @Transactional
    public Long saveDevice(long schoolId, AutoMark m) {
        Previous before = previous(m.lessonSlotId(), m.date()).get(m.studentId());
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(clock));
        List<Long> id = jdbc.queryForList(UPSERT_DEVICE, Long.class, m.lessonSlotId(), m.studentId(), Date.valueOf(m.date()),
                m.status().name(), now, m.lateMinutes(), blankToNull(m.note()), now);
        if (id.isEmpty()) return null;
        publishIfNotifiable(id.get(0), before, m.status(), settings.forSchool(schoolId).notifyOnAuto());
        events.publishEvent(new AttendanceChangedEvent(schoolId));
        return id.get(0);
    }

    /**
     * "✓ Hammasini tasdiqlash": the lesson's automatic marks become MANUAL under this staff member.
     * Parents hear now about the absences they were not told about before. Returns how many changed.
     */
    @Transactional
    public int confirmAutomatic(long schoolId, long lessonSlotId, LocalDate date, String username) {
        boolean notifyOnAuto = settings.forSchool(schoolId).notifyOnAuto();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select id, status, source from attendance where lesson_slot_id = ? and record_date = ? "
                        + "and source in ('AUTO_CARRY', 'DEMO_SIM') for update", lessonSlotId, Date.valueOf(date));
        if (rows.isEmpty()) return 0;
        jdbc.update("update attendance set source = 'MANUAL', created_by = ?, auto_at = null, auto_note = null "
                + "where lesson_slot_id = ? and record_date = ? and source in ('AUTO_CARRY', 'DEMO_SIM')",
                username, lessonSlotId, Date.valueOf(date));
        for (Map<String, Object> r : rows) {
            AttendanceStatus status = AttendanceStatus.valueOf((String) r.get("status"));
            AttendanceSource source = AttendanceSource.valueOf((String) r.get("source"));
            boolean alreadyTold = source == AttendanceSource.AUTO_CARRY && notifyOnAuto;
            if (!alreadyTold && AttendanceMarkedEvent.isNotifiable(null, status)) {
                events.publishEvent(new AttendanceMarkedEvent(((Number) r.get("id")).longValue(), null, status));
            }
        }
        events.publishEvent(new AttendanceChangedEvent(schoolId));
        return rows.size();
    }

    /** "Demo ma'lumotlarini tozalash": removes DEMO_SIM rows of this school only. */
    @Transactional
    public int deleteDemo(long schoolId) {
        int n = jdbc.update("""
                delete from attendance a using lesson_slot ls, school_class c, academic_year y
                 where a.lesson_slot_id = ls.id and ls.class_id = c.id and c.academic_year_id = y.id
                   and y.school_id = ? and a.source = 'DEMO_SIM'""", schoolId);
        if (n > 0) events.publishEvent(new AttendanceChangedEvent(schoolId));
        return n;
    }

    private Object[] args(AutoMark m, AttendanceSource source, Timestamp now) {
        return new Object[]{m.lessonSlotId(), m.studentId(), Date.valueOf(m.date()), m.status().name(),
                source.name(), now, m.lateMinutes(), blankToNull(m.note()), now};
    }

    private Map<Long, Previous> previous(long lessonSlotId, LocalDate date) {
        Map<Long, Previous> map = new java.util.HashMap<>();
        jdbc.query("select student_id, status, source from attendance where lesson_slot_id = ? and record_date = ?",
                rs -> {
                    map.put(rs.getLong(1), new Previous(AttendanceStatus.valueOf(rs.getString(2)),
                            AttendanceSource.valueOf(rs.getString(3))));
                }, lessonSlotId, Date.valueOf(date));
        return map;
    }

    /**
     * The parent hears about a change into ABSENT / LATE. A previous mark the parent was never
     * told about (DEMO_SIM, or AUTO_CARRY without notifyOnAuto) counts as no mark at all.
     */
    private void publishIfNotifiable(long id, Previous before, AttendanceStatus now, boolean notifyOnAuto) {
        AttendanceStatus told = null;
        if (before != null) {
            boolean wasTold = before.source().isReal() || (before.source() == AttendanceSource.AUTO_CARRY && notifyOnAuto);
            if (wasTold) told = before.status();
        }
        if (AttendanceMarkedEvent.isNotifiable(told, now)) {
            events.publishEvent(new AttendanceMarkedEvent(id, told, now));
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
