package uz.azizbek.maktabboshqaruv.event;

/** Attendance or grades of a school changed: cached dashboard numbers of that school are stale. */
public record AttendanceChangedEvent(long schoolId) {
}
