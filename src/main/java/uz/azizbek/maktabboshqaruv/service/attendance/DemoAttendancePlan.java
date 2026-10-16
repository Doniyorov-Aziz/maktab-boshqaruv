package uz.azizbek.maktabboshqaruv.service.attendance;

import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * What the demo simulator says about one student on one day — the same answer every time it
 * is asked (a hash of school, student and date, no random state), so a re-run or a page refresh
 * never changes a number.
 * <ul>
 *   <li>a new absence starts on 1.2–2.5 % of school days (more on Monday and Saturday), an
 *       illness then continues to the next school day with 55 % — together 2–5 % absent;</li>
 *   <li>1–2 "chronic" students per class start an absence far more often, so the
 *       "3+ days absent" list on the home page has someone in it;</li>
 *   <li>about 1 % excused, 1–3 % late by 5–25 minutes (late in the first lesson only);</li>
 *   <li>an absent or excused student is so for every lesson of that day.</li>
 * </ul>
 */
public final class DemoAttendancePlan {

    /** One student's day. lateMinutes only for LATE. */
    public record Day(AttendanceStatus status, Integer lateMinutes) {
    }

    private static final double CONTINUE_ILLNESS = 0.55;
    private static final double CHRONIC_NEW_ABSENCE = 0.12;
    private static final int LOOKBACK = 6;

    private DemoAttendancePlan() {
    }

    /**
     * @param schoolDay tells whether a date is a school day (not Sunday, not a holiday)
     * @param chronic   the student is one of their class's chronic absentees
     */
    public static Day of(long schoolId, long studentId, LocalDate date, boolean chronic, Predicate<LocalDate> schoolDay) {
        return of(schoolId, studentId, date, chronic, schoolDay, LOOKBACK);
    }

    private static Day of(long schoolId, long studentId, LocalDate date, boolean chronic,
                          Predicate<LocalDate> schoolDay, int depth) {
        double dayFactor = unit(mix(schoolId, date.toEpochDay(), 11));
        double newAbsence = (chronic ? CHRONIC_NEW_ABSENCE : 0.012 + 0.013 * dayFactor)
                + (date.getDayOfWeek() == DayOfWeek.MONDAY || date.getDayOfWeek() == DayOfWeek.SATURDAY ? 0.008 : 0);
        double excused = 0.006 + 0.008 * unit(mix(schoolId, date.toEpochDay(), 13));
        double late = 0.01 + 0.02 * unit(mix(schoolId, date.toEpochDay(), 17));

        double pAbsent = newAbsence;
        if (depth > 0) {
            LocalDate prev = previousSchoolDay(date, schoolDay);
            if (prev != null && of(schoolId, studentId, prev, chronic, schoolDay, depth - 1).status() == AttendanceStatus.ABSENT) {
                pAbsent = CONTINUE_ILLNESS;
            }
        }

        double u = unit(mix(schoolId, studentId, date.toEpochDay()));
        if (u < pAbsent) return new Day(AttendanceStatus.ABSENT, null);
        if (u < pAbsent + excused) return new Day(AttendanceStatus.EXCUSED, null);
        if (u < pAbsent + excused + late) {
            int minutes = 5 + (int) (unit(mix(studentId, date.toEpochDay(), 23)) * 21);
            return new Day(AttendanceStatus.LATE, minutes);
        }
        return new Day(AttendanceStatus.PRESENT, null);
    }

    /** The status for one lesson: late only in the first lesson of the day, present after it. */
    public static AttendanceStatus forLesson(Day day, boolean firstLesson) {
        if (day.status() == AttendanceStatus.LATE && !firstLesson) return AttendanceStatus.PRESENT;
        return day.status();
    }

    /** 1–2 students of a class (fixed for the school year) who miss school more often. */
    public static Set<Long> chronic(long classId, List<Long> studentIds, int schoolYearStart) {
        if (studentIds.isEmpty()) return Set.of();
        List<Long> sorted = new ArrayList<>(studentIds);
        Collections.sort(sorted);
        long h = mix(classId, schoolYearStart, 29);
        int count = Math.min(sorted.size(), 1 + (int) Math.floorMod(h, 2L));
        java.util.LinkedHashSet<Long> picked = new java.util.LinkedHashSet<>();
        for (int i = 0; picked.size() < count && i < 10; i++) {
            picked.add(sorted.get((int) Math.floorMod(mix(h, i, 31), (long) sorted.size())));
        }
        return picked;
    }

    private static LocalDate previousSchoolDay(LocalDate date, Predicate<LocalDate> schoolDay) {
        LocalDate d = date.minusDays(1);
        for (int i = 0; i < 14; i++, d = d.minusDays(1)) {
            if (schoolDay.test(d)) return d;
        }
        return null;
    }

    /** SplitMix64 over the parts: a well-spread 64-bit hash. */
    static long mix(long... parts) {
        long h = 0x9E3779B97F4A7C15L;
        for (long p : parts) {
            h += p * 0xBF58476D1CE4E5B9L + 0x9E3779B97F4A7C15L;
            h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
            h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
            h = h ^ (h >>> 31);
        }
        return h;
    }

    /** A hash as a number in [0, 1). */
    static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }
}
