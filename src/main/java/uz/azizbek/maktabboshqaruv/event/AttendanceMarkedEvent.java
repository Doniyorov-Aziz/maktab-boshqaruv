package uz.azizbek.maktabboshqaruv.event;

import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

/**
 * Published by AttendanceService only when a record's status actually changed
 * to ABSENT or LATE. Carries ids, not entities: the listener runs after commit
 * in its own transaction and reloads what it needs.
 */
public record AttendanceMarkedEvent(Long attendanceId, AttendanceStatus previousStatus, AttendanceStatus newStatus) {

    /** The single rule for "does this save deserve a parent notification?". */
    public static boolean isNotifiable(AttendanceStatus previous, AttendanceStatus current) {
        if (current != AttendanceStatus.ABSENT && current != AttendanceStatus.LATE) return false;
        return previous != current;
    }
}
