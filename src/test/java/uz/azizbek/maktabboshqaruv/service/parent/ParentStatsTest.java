package uz.azizbek.maktabboshqaruv.service.parent;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static uz.azizbek.maktabboshqaruv.entity.AttendanceStatus.*;

class ParentStatsTest {

    @Test
    void counts_andRate_lateCountsAsAttended() {
        ParentStats.Counts c = ParentStats.count(List.of(PRESENT, PRESENT, LATE, ABSENT, EXCUSED, PRESENT, PRESENT, PRESENT));
        assertEquals(8, c.total());
        assertEquals(5, c.present());
        assertEquals(1, c.late());
        assertEquals(1, c.absent());
        assertEquals(1, c.excused());
        assertEquals(75.0, c.rate()); // (5 + 1) / 8
    }

    @Test
    void rate_isNullWithoutData_andRoundedToOneDecimal() {
        assertNull(ParentStats.rate(0, 0));
        assertEquals(66.7, ParentStats.rate(2, 3));
        assertEquals(100.0, ParentStats.rate(4, 4));
    }

    @Test
    void dayStatus_worstOfTheDay() {
        assertEquals("NONE", ParentStats.dayStatus(List.of()));
        assertEquals("NONE", ParentStats.dayStatus(null));
        assertEquals("PRESENT", ParentStats.dayStatus(List.of(PRESENT, PRESENT)));
        assertEquals("LATE", ParentStats.dayStatus(List.of(PRESENT, LATE)));
        assertEquals("ABSENT", ParentStats.dayStatus(List.of(LATE, ABSENT, PRESENT)));
        assertEquals("EXCUSED", ParentStats.dayStatus(List.of(EXCUSED, EXCUSED)));
        assertEquals("PRESENT", ParentStats.dayStatus(List.of(PRESENT, EXCUSED)));
    }

    @Test
    void schoolYear_runsSeptemberToAugust() {
        assertEquals(2026, ParentStats.schoolYearStart(LocalDate.of(2026, 9, 1)));
        assertEquals(2025, ParentStats.schoolYearStart(LocalDate.of(2026, 8, 31)));
        LocalDate[] y = ParentStats.schoolYearRange(LocalDate.of(2027, 3, 10));
        assertEquals(LocalDate.of(2026, 9, 1), y[0]);
        assertEquals(LocalDate.of(2027, 8, 31), y[1]);
    }

    @Test
    void quarters() {
        assertEquals(1, ParentStats.quarterOf(LocalDate.of(2026, 9, 30)));
        assertEquals(2, ParentStats.quarterOf(LocalDate.of(2026, 12, 1)));
        assertEquals(3, ParentStats.quarterOf(LocalDate.of(2027, 2, 1)));
        assertEquals(4, ParentStats.quarterOf(LocalDate.of(2027, 5, 1)));
        LocalDate[] q3 = ParentStats.quarterRange(LocalDate.of(2026, 10, 5), 3);
        assertEquals(LocalDate.of(2027, 1, 1), q3[0]);
        assertEquals(LocalDate.of(2027, 3, 31), q3[1]);
        assertThrows(IllegalArgumentException.class, () -> ParentStats.quarterRange(LocalDate.now(), 5));
    }

    @Test
    void trend_needsBothMonths_andIgnoresTinyChanges() {
        assertEquals("NONE", ParentStats.trend(4.5, null));
        assertEquals("NONE", ParentStats.trend(null, 4.0));
        assertEquals("UP", ParentStats.trend(4.6, 4.2));
        assertEquals("DOWN", ParentStats.trend(3.9, 4.5));
        assertEquals("FLAT", ParentStats.trend(4.25, 4.2));
    }

    @Test
    void average_roundedToTwoDecimals() {
        assertNull(ParentStats.average(List.of()));
        assertEquals(4.33, ParentStats.average(List.of(5, 4, 4)));
    }

    @Test
    void bars() {
        assertEquals("▰▰▰▰▰▰▰▰▱▱", ParentStats.progressBar(82.1, 10));
        assertEquals("▱▱▱▱▱▱▱▱▱▱", ParentStats.progressBar(null, 10));
        assertEquals("▰▰▰▰▰▰▰▰▰▰", ParentStats.progressBar(140.0, 10));
        assertEquals("▰▰▰▰▱", ParentStats.gradeBar(4.3, 5));
        assertEquals("▰▰▰▰▰", ParentStats.gradeBar(4.6, 5));
    }

    @Test
    void weekdayNamesMatchTheTimetable() {
        assertEquals("Chorshanba", ParentStats.WEEKDAY_UZ.get(java.time.DayOfWeek.WEDNESDAY));
        assertEquals(7, ParentStats.WEEKDAY_UZ.size());
    }
}
