package uz.azizbek.maktabboshqaruv.service.dashboard;

import org.junit.jupiter.api.Test;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoAttendancePlan;
import uz.azizbek.maktabboshqaruv.service.attendance.DemoGradePlan;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The home page formulas, and the demo plans' promised shares, without a database. */
class DashboardMetricsTest {

    @Test
    void attendance_leavesTheExcusedOut() {
        assertEquals(90.0, DashboardMetrics.attendance(17, 1, 2));
        // an excused student changes nothing: they are in neither side
        assertEquals(DashboardMetrics.attendance(17, 1, 2), DashboardMetrics.attendance(17, 1, 2));
        assertNull(DashboardMetrics.attendance(0, 0, 0), "nobody counted: no number, not 0 %");
        assertEquals(0.0, DashboardMetrics.attendance(0, 0, 3));
    }

    @Test
    void masteryAndQuality() {
        // 5, 5, 4, 4, 4, 3, 2 → mastery 6/7, quality 5/7
        assertEquals(85.7, DashboardMetrics.mastery(2, 3, 1, 1));
        assertEquals(71.4, DashboardMetrics.quality(2, 3, 1, 1));
        assertNull(DashboardMetrics.mastery(0, 0, 0, 0));
        assertNull(DashboardMetrics.quality(0, 0, 0, 0));
    }

    @Test
    void partial_isUnderHalf() {
        assertTrue(DashboardMetrics.partial(12, 25));
        assertFalse(DashboardMetrics.partial(13, 25));
        assertFalse(DashboardMetrics.partial(0, 25), "nothing marked is 'not taken', not partial");
    }

    @Test
    void healthIndex_weightsAndMissingParts() {
        DashboardMetrics.Health h = DashboardMetrics.health(95.0, 70.0, 90.0, 40.0, DashboardMetrics.Weights.DEFAULT);
        // 95 × 0.4 + 70 × 0.3 + 90 × 0.2 + 40 × 0.1 = 38 + 21 + 18 + 4 = 81
        assertEquals(81, h.index());
        assertEquals("yaxshi", h.label());
        // no grades yet: quality is left out and the other weights scaled up (95×40 + 90×20 + 40×10) / 70
        assertEquals((int) Math.round((95 * 40 + 90 * 20 + 40 * 10) / 70.0), DashboardMetrics.health(95.0, null, 90.0, 40.0,
                DashboardMetrics.Weights.DEFAULT).index());
        assertNull(DashboardMetrics.health(null, null, null, null, DashboardMetrics.Weights.DEFAULT).index());
        assertEquals("a'lo", DashboardMetrics.label(85));
        assertEquals("e'tibor kerak", DashboardMetrics.label(69));
    }

    @Test
    void riskScore_partsAndCaps() {
        assertEquals(0, DashboardMetrics.risk(0, 4.5, 4.5, 0).score(), "a good, steady student");
        assertEquals(35, DashboardMetrics.risk(6, null, null, 0).score(), "6+ days absent is the whole absence part");
        assertEquals(35, DashboardMetrics.risk(20, null, null, 0).score(), "and no more");
        DashboardMetrics.Risk r = DashboardMetrics.risk(3, 3.0, 4.0, 2);
        assertEquals(18, r.absence());   // 3/6 × 35 = 17.5
        assertEquals(20, r.average());   // (4.0 − 3.0) / 1.5 × 30
        assertEquals(20, r.trend());     // a drop of 1.0
        assertEquals(8, r.twos());       // 2/4 × 15 = 7.5
        assertEquals(66, r.score());
        assertEquals(100, DashboardMetrics.risk(10, 2.0, 5.0, 9).score());
        assertEquals(0, DashboardMetrics.risk(0, 3.5, 3.0, 0).trend(), "a rising average is no risk");
    }

    @Test
    void demoGrades_matchTheirPromisedShares() {
        int[] count = new int[6];
        int total = 0;
        List<String> subjects = List.of("Matematika", "Ona tili", "Ingliz tili", "Fizika", "Jismoniy tarbiya", "Tarix", "Musiqa");
        for (long student = 1; student <= 600; student++) {
            for (int lesson = 0; lesson < 40; lesson++) {
                LocalDate d = LocalDate.of(2026, 9, 7).plusDays(lesson);
                int g = DemoGradePlan.score(1, student, student % 22, subjects.get(lesson % subjects.size()), 1000 + lesson, d, 2026);
                count[g]++;
                total++;
            }
        }
        double five = count[5] * 100.0 / total, four = count[4] * 100.0 / total, three = count[3] * 100.0 / total, two = count[2] * 100.0 / total;
        assertTrue(five > 26 && five < 38, "5 ≈ 30–35 %: " + five);
        assertTrue(four > 35 && four < 46, "4 ≈ 40 %: " + four);
        assertTrue(three > 17 && three < 28, "3 ≈ 20 %: " + three);
        assertTrue(two > 3 && two < 10, "2 ≈ 5–8 %: " + two);

        // exact sciences lower than PE
        assertTrue(DemoGradePlan.subjectOffset("Fizika") < DemoGradePlan.subjectOffset("Jismoniy tarbiya"));
        assertEquals(0.35, DemoGradePlan.subjectOffset("Tasviriy san'at"));
    }

    @Test
    void demoAttendance_matchesItsPromisedShares() {
        int present = 0, late = 0, absent = 0, excused = 0, n = 0;
        for (long student = 1; student <= 500; student++) {
            for (int day = 0; day < 60; day++) {
                LocalDate d = LocalDate.of(2026, 9, 1).plusDays(day);
                if (d.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) continue;
                var plan = DemoAttendancePlan.of(1, student, d, false, x -> x.getDayOfWeek() != java.time.DayOfWeek.SUNDAY);
                switch (plan.status()) {
                    case PRESENT -> present++;
                    case LATE -> late++;
                    case ABSENT -> absent++;
                    default -> excused++;
                }
                n++;
            }
        }
        double p = present * 100.0 / n, l = late * 100.0 / n, a = absent * 100.0 / n, e = excused * 100.0 / n;
        assertTrue(p > 90 && p < 98, "present " + p);
        assertTrue(l >= 1 && l <= 3.5, "late " + l);
        assertTrue(a >= 1.5 && a <= 5.5, "absent " + a);
        assertTrue(e > 0.3 && e < 2, "excused " + e);
    }

    @Test
    void slackers_areTwoOrThree_andForgetFifteenToThirtyPercent() {
        Set<Long> s = DemoAttendancePlan.slackers(1, List.of(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L), 2026);
        assertTrue(s.size() == 2 || s.size() == 3);
        assertEquals(s, DemoAttendancePlan.slackers(1, List.of(17L, 16L, 15L, 14L, 13L, 12L, 11L, 10L), 2026), "fixed, whatever the order");
        for (long emp : List.of(10L, 11L, 12L)) {
            int skipped = 0;
            int other = 0;
            for (int i = 0; i < 4000; i++) {
                LocalDate d = LocalDate.of(2026, 9, 1).plusDays(i % 200);
                if (DemoAttendancePlan.skipped(emp, 5000 + i, d, true)) skipped++;
                if (DemoAttendancePlan.skipped(emp, 5000 + i, d, false)) other++;
            }
            assertTrue(skipped >= 4000 * 0.13 && skipped <= 4000 * 0.32, "slacker skips " + skipped);
            assertTrue(other <= 4000 * 0.035, "others skip at most ~3 %: " + other);
        }
    }
}
