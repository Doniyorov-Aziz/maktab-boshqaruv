package uz.azizbek.maktabboshqaruv.service.dashboard;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every formula of the home page in one place (tested in DashboardMetricsTest). A percentage has
 * one decimal; null means "no data" and is never shown as 0.
 * <ul>
 *   <li><b>Attendance %</b> = (present + late) / (present + late + absent). Excused absences are
 *       left out of both sides.</li>
 *   <li><b>Mastery %</b> (o'zlashtirish) = grades 3, 4, 5 / all grades. <b>Quality %</b> (sifat) =
 *       grades 4, 5 / all grades.</li>
 *   <li><b>Health index</b> (0–100) = attendance × 40 % + quality × 30 % + journal completeness × 20 %
 *       + parents on Telegram × 10 % (weights from the school settings). A missing part is left out
 *       and the rest re-weighted.</li>
 *   <li><b>Risk score</b> (0–100) of a student — see {@link #risk}.</li>
 * </ul>
 */
public final class DashboardMetrics {

    private DashboardMetrics() {
    }

    /** part / whole as a percentage with one decimal; null when whole is 0. */
    public static Double pct(long part, long whole) {
        return whole <= 0 ? null : Math.round(part * 1000.0 / whole) / 10.0;
    }

    public static Double attendance(long present, long late, long absent) {
        return pct(present + late, present + late + absent);
    }

    public static Double mastery(long g5, long g4, long g3, long g2) {
        return pct(g5 + g4 + g3, g5 + g4 + g3 + g2);
    }

    public static Double quality(long g5, long g4, long g3, long g2) {
        return pct(g5 + g4, g5 + g4 + g3 + g2);
    }

    /** A class counts as "taken" today only when at least half of its students have a mark. */
    public static boolean partial(int marked, int students) {
        return marked > 0 && marked * 2 < students;
    }

    // ------------------------------------------------------------------ health index

    public record Weights(int attendance, int quality, int journal, int telegram) {
        public static final Weights DEFAULT = new Weights(40, 30, 20, 10);

        int sum() {
            return attendance + quality + journal + telegram;
        }
    }

    public record Health(Integer index, String label, Map<String, Double> parts, Weights weights) {
    }

    /**
     * The index from the four parts (each 0–100, null when there is no data). Parts without data
     * are left out and the weights of the rest scaled up; no part at all → index null.
     */
    public static Health health(Double attendance, Double quality, Double journal, Double telegram, Weights w) {
        Map<String, Double> parts = new LinkedHashMap<>();
        parts.put("attendance", attendance);
        parts.put("quality", quality);
        parts.put("journal", journal);
        parts.put("telegram", telegram);
        double sum = 0;
        int weight = 0;
        int[] ws = {w.attendance(), w.quality(), w.journal(), w.telegram()};
        Double[] vs = {attendance, quality, journal, telegram};
        for (int i = 0; i < 4; i++) {
            if (vs[i] == null || ws[i] <= 0) continue;
            sum += clamp(vs[i], 0, 100) * ws[i];
            weight += ws[i];
        }
        Integer index = weight == 0 ? null : (int) Math.round(sum / weight);
        return new Health(index, label(index), parts, w);
    }

    /** "a'lo" from 85, "yaxshi" from 70, below that "e'tibor kerak". */
    public static String label(Integer index) {
        if (index == null) return null;
        if (index >= 85) return "a'lo";
        if (index >= 70) return "yaxshi";
        return "e'tibor kerak";
    }

    // ------------------------------------------------------------------ risk score

    public record Risk(int score, int absence, int average, int trend, int twos) {
    }

    /**
     * A student's risk (0–100) from four signals, each capped:
     * <ul>
     *   <li>absence — up to 35: school days absent in the last 30 days, 6+ days = all 35;</li>
     *   <li>average — up to 30: the last 2 weeks' grade average, 4.0 or more = 0, 2.5 or less = 30;</li>
     *   <li>trend — up to 20: how far that average fell below the 4 weeks before, a drop of 1.0+ = 20;</li>
     *   <li>twos — up to 15: grades of 2 in the last 30 days, 4+ = 15.</li>
     * </ul>
     * A missing average (no grades) adds nothing.
     */
    public static Risk risk(int absentDays30, Double avg2w, Double avgBefore, int twos30) {
        int absence = (int) Math.round(Math.min(absentDays30 / 6.0, 1) * 35);
        int average = avg2w == null ? 0 : (int) Math.round(clamp((4.0 - avg2w) / 1.5, 0, 1) * 30);
        int trend = avg2w == null || avgBefore == null ? 0 : (int) Math.round(clamp(avgBefore - avg2w, 0, 1) * 20);
        int twos = (int) Math.round(Math.min(twos30 / 4.0, 1) * 15);
        return new Risk(Math.min(100, absence + average + trend + twos), absence, average, trend, twos);
    }

    /** From this score a student is on the "xavf ostida" list. */
    public static final int RISK_LIST_FROM = 30;

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
