package uz.azizbek.maktabboshqaruv.service.attendance;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

import static uz.azizbek.maktabboshqaruv.service.attendance.DemoAttendancePlan.mix;
import static uz.azizbek.maktabboshqaruv.service.attendance.DemoAttendancePlan.unit;

/**
 * What the demo simulator says about grades — like {@link DemoAttendancePlan}, a hash of the
 * school, student, lesson and date, so a re-run gives the same grades.
 * <ul>
 *   <li>every student has a level for the school year: strong ~25 % (mostly 5 and 4), middle ~55 %
 *       (4 and 3, sometimes 5), weak ~20 % (3 and 2, sometimes 4);</li>
 *   <li>each class has its own strength (±0.2 of a point), exact sciences come out a little
 *       lower, PE / music / art higher;</li>
 *   <li>~8 % of students go through "bad fortnights" (about one in three): their grades drop by
 *       about a point, so "bahosi tushganlar" on the home page always has someone in it;</li>
 *   <li>in a lesson 30–40 % of the students who came get a grade; in ~5 % of weeks the last
 *       lesson of a subject is a test (EXAM) and everyone present gets one.</li>
 * </ul>
 * Overall about 5 — 31 %, 4 — 40 %, 3 — 23 %, 2 — 6 %.
 */
public final class DemoGradePlan {

    private static final double[] LEVEL_MEAN = {4.8, 4.1, 3.0}; // strong, middle, weak
    private static final double NOISE = 1.3;
    private static final double DECLINER = 0.08;
    private static final double DROP = 0.9;

    private static final Set<String> EXACT = Set.of("matematika", "algebra", "geometriya", "fizika", "kimyo");
    private static final Set<String> EASY = Set.of("jismoniy tarbiya", "musiqa", "tasviriy san'at", "texnologiya");

    private DemoGradePlan() {
    }

    /** 0 strong, 1 middle, 2 weak — fixed for the school year. */
    public static int level(long studentId, int schoolYearStart) {
        double u = unit(mix(studentId, schoolYearStart, 41));
        return u < 0.25 ? 0 : u < 0.80 ? 1 : 2;
    }

    /** The class's strength in points, in [-0.2, 0.2], fixed for the school year. */
    public static double classStrength(long classId, int schoolYearStart) {
        return (unit(mix(classId, schoolYearStart, 43)) - 0.5) * 0.4;
    }

    /** Exact sciences a little lower, PE / music / art higher. */
    public static double subjectOffset(String subject) {
        String s = subject == null ? "" : subject.toLowerCase(Locale.ROOT).replace('‘', '\'').replace('’', '\'').trim();
        for (String e : EXACT) if (s.startsWith(e)) return -0.25;
        for (String e : EASY) if (s.startsWith(e)) return 0.35;
        return 0;
    }

    /** The student is in one of their "bad fortnights" on this date (only ~8 % of students ever are). */
    public static boolean declining(long studentId, LocalDate date) {
        if (unit(mix(studentId, 47)) >= DECLINER) return false;
        long fortnight = Math.floorDiv(date.toEpochDay(), 14);
        return Math.floorMod(fortnight + mix(studentId, 53), 3L) == 0;
    }

    /** Share of the present students graded in this lesson: 30–40 %. */
    public static double gradedShare(long lessonSlotId, LocalDate date) {
        return 0.30 + 0.10 * unit(mix(lessonSlotId, date.toEpochDay(), 59));
    }

    public static boolean graded(long studentId, long lessonSlotId, LocalDate date) {
        return unit(mix(studentId, lessonSlotId, date.toEpochDay(), 61)) < gradedShare(lessonSlotId, date);
    }

    /** ~5 % of weeks: the last lesson of the week in this subject is a test for the class. */
    public static boolean testWeek(long classId, long subjectId, LocalDate date) {
        long week = Math.floorDiv(date.toEpochDay() + 3, 7); // weeks start on Monday
        return unit(mix(classId, subjectId, week, 67)) < 0.05;
    }

    /** The grade (2..5) one student gets in one lesson. */
    public static int score(long schoolId, long studentId, long classId, String subject, long lessonSlotId,
                            LocalDate date, int schoolYearStart) {
        double mean = LEVEL_MEAN[level(studentId, schoolYearStart)]
                + classStrength(classId, schoolYearStart)
                + subjectOffset(subject)
                - (declining(studentId, date) ? DROP : 0);
        double noise = (unit(mix(schoolId, studentId, lessonSlotId, date.toEpochDay(), 71))
                + unit(mix(studentId, lessonSlotId, date.toEpochDay(), 73))
                + unit(mix(lessonSlotId, studentId, date.toEpochDay(), 79)) - 1.5) * NOISE;
        long g = Math.round(mean + noise);
        return (int) Math.max(2, Math.min(5, g));
    }
}
