package uz.azizbek.maktabboshqaruv.telegram;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Quiet-hours arithmetic on Tashkent wall-clock time. The window may wrap
 * midnight (22:00–07:00) or not (13:00–14:00); start == end means "no window".
 */
public final class QuietHours {

    private QuietHours() {
    }

    public static boolean isQuiet(LocalTime time, LocalTime start, LocalTime end) {
        if (start == null || end == null || start.equals(end)) return false;
        if (start.isBefore(end)) {
            return !time.isBefore(start) && time.isBefore(end);
        }
        // wraps midnight
        return !time.isBefore(start) || time.isBefore(end);
    }

    /** Earliest moment a message created at {@code now} may be delivered. */
    public static LocalDateTime deliveryTime(LocalDateTime now, LocalTime start, LocalTime end) {
        LocalTime t = now.toLocalTime();
        if (!isQuiet(t, start, end)) return now;
        LocalDateTime sameDayEnd = now.toLocalDate().atTime(end);
        // In a wrapping window, 23:00 releases tomorrow at `end`; 05:00 releases today.
        return sameDayEnd.isAfter(now) ? sameDayEnd : sameDayEnd.plusDays(1);
    }
}
