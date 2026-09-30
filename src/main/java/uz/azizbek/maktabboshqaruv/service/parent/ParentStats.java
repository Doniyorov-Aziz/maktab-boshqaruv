package uz.azizbek.maktabboshqaruv.service.parent;

import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Pure calculations behind the parent views (no database), kept apart so they
 * can be unit-tested directly.
 *
 * School year and quarters follow the Uzbek calendar, independent of the
 * AcademicYear rows (which may be stale in seeded data): the year runs from
 * 1 September to 31 August; quarters are Sep–Oct, Nov–Dec, Jan–Mar, Apr–Aug.
 */
public final class ParentStats {

    /** LessonSlot.weekday values used across the app. */
    public static final Map<DayOfWeek, String> WEEKDAY_UZ = Map.of(
            DayOfWeek.MONDAY, "Dushanba", DayOfWeek.TUESDAY, "Seshanba", DayOfWeek.WEDNESDAY, "Chorshanba",
            DayOfWeek.THURSDAY, "Payshanba", DayOfWeek.FRIDAY, "Juma", DayOfWeek.SATURDAY, "Shanba",
            DayOfWeek.SUNDAY, "Yakshanba");

    private ParentStats() {
    }

    public record Counts(int total, int present, int late, int absent, int excused) {
        /** Share of lessons attended (present or late), 0..100 with one decimal; null when nothing is marked. */
        public Double rate() {
            return ParentStats.rate(present + late, total);
        }
    }

    public static Counts count(Collection<AttendanceStatus> statuses) {
        int present = 0, late = 0, absent = 0, excused = 0;
        for (AttendanceStatus s : statuses) {
            switch (s) {
                case PRESENT -> present++;
                case LATE -> late++;
                case ABSENT -> absent++;
                case EXCUSED -> excused++;
            }
        }
        return new Counts(statuses.size(), present, late, absent, excused);
    }

    public static Double rate(long attended, long total) {
        if (total <= 0) return null;
        return Math.round(attended * 1000.0 / total) / 10.0;
    }

    /** The status that describes a whole day: any absence beats lateness, which beats excused, which beats present. */
    public static String dayStatus(List<AttendanceStatus> statuses) {
        if (statuses == null || statuses.isEmpty()) return "NONE";
        if (statuses.contains(AttendanceStatus.ABSENT)) return "ABSENT";
        if (statuses.contains(AttendanceStatus.LATE)) return "LATE";
        if (!statuses.contains(AttendanceStatus.PRESENT)) return "EXCUSED";
        return "PRESENT";
    }

    public static int schoolYearStart(LocalDate date) {
        return date.getMonthValue() >= 9 ? date.getYear() : date.getYear() - 1;
    }

    public static LocalDate[] schoolYearRange(LocalDate date) {
        int start = schoolYearStart(date);
        return new LocalDate[]{LocalDate.of(start, 9, 1), LocalDate.of(start + 1, 8, 31)};
    }

    public static int quarterOf(LocalDate date) {
        int m = date.getMonthValue();
        if (m == 9 || m == 10) return 1;
        if (m == 11 || m == 12) return 2;
        if (m <= 3) return 3;
        return 4;
    }

    /** Date range of quarter {@code n} (1..4) of the school year that contains {@code anyDateInYear}. */
    public static LocalDate[] quarterRange(LocalDate anyDateInYear, int n) {
        int start = schoolYearStart(anyDateInYear);
        return switch (n) {
            case 1 -> new LocalDate[]{LocalDate.of(start, 9, 1), LocalDate.of(start, 10, 31)};
            case 2 -> new LocalDate[]{LocalDate.of(start, 11, 1), LocalDate.of(start, 12, 31)};
            case 3 -> new LocalDate[]{LocalDate.of(start + 1, 1, 1), LocalDate.of(start + 1, 3, 31)};
            case 4 -> new LocalDate[]{LocalDate.of(start + 1, 4, 1), LocalDate.of(start + 1, 8, 31)};
            default -> throw new IllegalArgumentException("Chorak 1..4 bo'lishi kerak: " + n);
        };
    }

    /** "UP"/"DOWN"/"FLAT", or "NONE" when either month has no grades. A change under 0.1 counts as flat. */
    public static String trend(Double thisMonth, Double previousMonth) {
        if (thisMonth == null || previousMonth == null) return "NONE";
        double diff = thisMonth - previousMonth;
        if (diff >= 0.1) return "UP";
        if (diff <= -0.1) return "DOWN";
        return "FLAT";
    }

    public static Double average(Collection<Integer> scores) {
        if (scores == null || scores.isEmpty()) return null;
        double avg = scores.stream().mapToInt(Integer::intValue).average().orElse(0);
        return Math.round(avg * 100) / 100.0;
    }

    /** "▰▰▰▰▰▰▰▰▱▱" for 0..100. */
    public static String progressBar(Double percent, int width) {
        double p = percent == null ? 0 : Math.max(0, Math.min(100, percent));
        int filled = (int) Math.round(p / 100.0 * width);
        return "▰".repeat(filled) + "▱".repeat(width - filled);
    }

    /** Grade bar on a 5-point scale: 4.6 -> "▰▰▰▰▱" (width 5). */
    public static String gradeBar(double average, int width) {
        return progressBar(average / 5.0 * 100.0, width);
    }
}
