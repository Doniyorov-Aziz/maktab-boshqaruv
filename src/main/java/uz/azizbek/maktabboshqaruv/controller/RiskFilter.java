package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService.RiskRow;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The "Xavf ostidagi o'quvchilar" panel's filter, the same as in the browser, for the Excel file:
 * name contains q (any case), one class, one level (high ≥ 70, mid 50–69, low under 50), and the
 * order (risk score, absent days, or how far the average fell).
 */
final class RiskFilter {

    private RiskFilter() {
    }

    static String level(int score) {
        return score >= 70 ? "high" : score >= 50 ? "mid" : "low";
    }

    static String levelLabel(int score) {
        return switch (level(score)) {
            case "high" -> "Yuqori";
            case "mid" -> "O'rta";
            default -> "Past";
        };
    }

    static double drop(RiskRow r) {
        return r.avgBefore() == null || r.avgNow() == null ? 0 : r.avgBefore() - r.avgNow();
    }

    static List<RiskRow> apply(List<RiskRow> rows, String q, Long classId, String level, String sort) {
        String needle = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Comparator<RiskRow> order = switch (sort == null ? "score" : sort) {
            case "absent" -> Comparator.comparingInt(RiskRow::absentDays).reversed();
            case "drop" -> Comparator.comparingDouble(RiskFilter::drop).reversed();
            default -> Comparator.comparingInt(RiskRow::score).reversed();
        };
        return rows.stream()
                .filter(r -> needle.isEmpty() || r.name().toLowerCase(Locale.ROOT).contains(needle))
                .filter(r -> classId == null || r.classId() == classId)
                .filter(r -> level == null || level.isBlank() || level.equals(level(r.score())))
                .sorted(order.thenComparing(RiskRow::name))
                .toList();
    }
}
