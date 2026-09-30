package uz.azizbek.maktabboshqaruv.telegram;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class QuietHoursTest {

    private static final LocalTime START = LocalTime.of(22, 0);
    private static final LocalTime END = LocalTime.of(7, 0);

    @Test
    void lateEvening_deliveredNextMorningAt7() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 23, 15);
        assertEquals(LocalDateTime.of(2026, 10, 1, 7, 0), QuietHours.deliveryTime(now, START, END));
    }

    @Test
    void exactlyAtStart_isQuiet() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 22, 0);
        assertEquals(LocalDateTime.of(2026, 10, 1, 7, 0), QuietHours.deliveryTime(now, START, END));
    }

    @Test
    void earlyMorning_deliveredSameDayAt7() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 5, 40);
        assertEquals(LocalDateTime.of(2026, 9, 30, 7, 0), QuietHours.deliveryTime(now, START, END));
    }

    @Test
    void daytime_deliveredImmediately() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 30, 7, 0);
        assertEquals(now, QuietHours.deliveryTime(now, START, END));
        LocalDateTime noon = LocalDateTime.of(2026, 9, 30, 12, 0);
        assertEquals(noon, QuietHours.deliveryTime(noon, START, END));
        LocalDateTime evening = LocalDateTime.of(2026, 9, 30, 21, 59);
        assertEquals(evening, QuietHours.deliveryTime(evening, START, END));
    }

    @Test
    void nonWrappingWindow_supported() {
        LocalTime s = LocalTime.of(13, 0);
        LocalTime e = LocalTime.of(14, 0);
        assertTrue(QuietHours.isQuiet(LocalTime.of(13, 30), s, e));
        assertFalse(QuietHours.isQuiet(LocalTime.of(14, 0), s, e));
        assertEquals(LocalDateTime.of(2026, 9, 30, 14, 0),
                QuietHours.deliveryTime(LocalDateTime.of(2026, 9, 30, 13, 10), s, e));
    }

    @Test
    void equalStartAndEnd_meansNoQuietHours() {
        LocalTime t = LocalTime.of(22, 0);
        assertFalse(QuietHours.isQuiet(LocalTime.of(23, 0), t, t));
    }
}
