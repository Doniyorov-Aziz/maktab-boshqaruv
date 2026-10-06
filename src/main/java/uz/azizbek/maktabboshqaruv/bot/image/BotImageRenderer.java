package uz.azizbek.maktabboshqaruv.bot.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Draws the bot's picture pages with plain Java2D — no browser, no external
 * service. The bundled Inter font (OFL) covers Uzbek Latin (o', g') and
 * Cyrillic (ў, ғ, қ, ҳ), so every language renders correctly on any server.
 * Colors follow the admin panel: indigo brand, green/amber/red/blue statuses.
 */
public final class BotImageRenderer {

    static {
        if (System.getProperty("java.awt.headless") == null) System.setProperty("java.awt.headless", "true");
    }

    // brand palette (matches frontend/src/css/quasar.variables.scss)
    static final Color INDIGO = new Color(0x4F46E5);
    static final Color VIOLET = new Color(0x6D28D9);
    static final Color TEXT = new Color(0x0F172A);
    static final Color MUTED = new Color(0x64748B);
    static final Color BORDER = new Color(0xE2E8F0);
    static final Color CARD = new Color(0xF8FAFC);
    static final Color GREEN = new Color(0x10B981);
    static final Color AMBER = new Color(0xF59E0B);
    static final Color RED = new Color(0xEF4444);
    static final Color BLUE = new Color(0x3B82F6);

    /** fill / text color pairs per day status. */
    static final Map<String, Color[]> DAY_COLORS = Map.of(
            "PRESENT", new Color[]{new Color(0xD1FAE5), new Color(0x065F46)},
            "LATE", new Color[]{new Color(0xFEF3C7), new Color(0x92400E)},
            "ABSENT", new Color[]{new Color(0xFEE2E2), new Color(0x991B1B)},
            "EXCUSED", new Color[]{new Color(0xDBEAFE), new Color(0x1E40AF)},
            "NONE", new Color[]{new Color(0xF1F5F9), new Color(0x94A3B8)});

    private static final int W = 1080;
    private static final Font REGULAR = load("/bot/fonts/Inter-Regular.ttf");
    private static final Font SEMIBOLD = load("/bot/fonts/Inter-SemiBold.ttf");
    private static final Font BOLD = load("/bot/fonts/Inter-Bold.ttf");

    private BotImageRenderer() {
    }

    private static Font load(String path) {
        try (InputStream in = BotImageRenderer.class.getResourceAsStream(path)) {
            if (in == null) return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
    }

    // ------------------------------------------------------------ inputs

    public record CalendarInput(String title, String subtitle, String footer, List<String> weekdayLabels,
                                YearMonthDays month, Map<String, String> legend, Double rate, String rateLabel,
                                String countsLine) {
    }

    /** Days of one month with a status each ("PRESENT", "LATE", "ABSENT", "EXCUSED", "NONE"). */
    public record YearMonthDays(LocalDate firstDay, List<String> statuses, LocalDate today) {
    }

    public record SubjectBar(String subject, double average, String trend) {
    }

    public record ChartInput(String title, String subtitle, String footer, List<SubjectBar> bars, String emptyText) {
    }

    public record ReportInput(String title, String period, String childName, String classAndSchool,
                              String attendanceLabel, Double attendanceRate, String gradesLabel, Double gradeAverage,
                              String gradesCountText, String rewardsLabel, int rewards, String warningsLabel,
                              int warnings, String strongLabel, List<String> strong, String attentionLabel,
                              List<String> attention, String noneText, String footer) {
    }

    // ------------------------------------------------------------ calendar

    public static byte[] attendanceCalendar(CalendarInput in) {
        LocalDate first = in.month().firstDay();
        int lead = first.getDayOfWeek().getValue() - 1; // Monday-first grid
        int days = in.month().statuses().size();
        int rows = (lead + days + 6) / 7;
        int cell = 128, gap = 12, gridX = (W - (7 * cell + 6 * gap)) / 2, gridTop = 330;
        int height = gridTop + rows * (cell + gap) + 250;

        BufferedImage img = new BufferedImage(W, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(img);
        header(g, in.title(), in.subtitle(), 220, true);
        rateBadge(g, in.rate(), in.rateLabel());

        g.setFont(SEMIBOLD.deriveFont(26f));
        for (int c = 0; c < 7; c++) {
            String label = in.weekdayLabels().get(c);
            g.setColor(c == 6 ? RED : MUTED);
            centered(g, label, gridX + c * (cell + gap), gridX + c * (cell + gap) + cell, gridTop - 30);
        }

        for (int i = 0; i < days; i++) {
            int pos = lead + i;
            int x = gridX + (pos % 7) * (cell + gap);
            int y = gridTop + (pos / 7) * (cell + gap);
            String status = in.month().statuses().get(i);
            Color[] colors = DAY_COLORS.getOrDefault(status, DAY_COLORS.get("NONE"));
            g.setColor(colors[0]);
            g.fill(new RoundRectangle2D.Double(x, y, cell, cell, 28, 28));
            LocalDate date = first.plusDays(i);
            if (date.equals(in.month().today())) {
                g.setColor(INDIGO);
                g.setStroke(new BasicStroke(5f));
                g.draw(new RoundRectangle2D.Double(x + 2.5, y + 2.5, cell - 5, cell - 5, 26, 26));
            }
            g.setColor(colors[1]);
            g.setFont(BOLD.deriveFont(40f));
            centered(g, String.valueOf(date.getDayOfMonth()), x, x + cell, y + cell / 2 + 14);
            if (!"NONE".equals(status)) {
                g.fillOval(x + cell / 2 - 7, y + cell - 30, 14, 14);
            }
        }

        int legendY = gridTop + rows * (cell + gap) + 50;
        legend(g, in.legend(), legendY);
        g.setFont(SEMIBOLD.deriveFont(28f));
        g.setColor(TEXT);
        centered(g, in.countsLine(), 0, W, legendY + 90);
        footer(g, in.footer(), height - 36);
        return png(img, g);
    }

    // --------------------------------------------------------------- chart

    public static byte[] subjectChart(ChartInput in) {
        int rowH = 96, top = 280;
        int rows = Math.max(1, in.bars().size());
        int height = top + rows * rowH + 120;
        BufferedImage img = new BufferedImage(W, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(img);
        header(g, in.title(), in.subtitle(), 220, false);

        if (in.bars().isEmpty()) {
            g.setColor(MUTED);
            g.setFont(SEMIBOLD.deriveFont(34f));
            centered(g, in.emptyText(), 0, W, top + 60);
        }
        int labelW = 330, barX = 60 + labelW + 20, barW = W - barX - 190;
        for (int i = 0; i < in.bars().size(); i++) {
            SubjectBar b = in.bars().get(i);
            int y = top + i * rowH;
            g.setColor(TEXT);
            g.setFont(SEMIBOLD.deriveFont(30f));
            g.drawString(fit(g, b.subject(), labelW), 60, y + 46);
            g.setColor(new Color(0xEEF2FF));
            g.fill(new RoundRectangle2D.Double(barX, y + 18, barW, 38, 38, 38));
            double ratio = Math.max(0.04, Math.min(1, b.average() / 5.0));
            g.setColor(gradeColor(b.average()));
            g.fill(new RoundRectangle2D.Double(barX, y + 18, barW * ratio, 38, 38, 38));
            g.setColor(TEXT);
            g.setFont(BOLD.deriveFont(34f));
            String value = String.format(java.util.Locale.ROOT, "%.1f", b.average());
            g.drawString(value, barX + barW + 24, y + 50);
            String arrow = switch (b.trend() == null ? "NONE" : b.trend()) {
                case "UP" -> "↑";
                case "DOWN" -> "↓";
                case "FLAT" -> "→";
                default -> "";
            };
            if (!arrow.isEmpty()) {
                g.setColor("UP".equals(b.trend()) ? GREEN : "DOWN".equals(b.trend()) ? RED : MUTED);
                g.setFont(BOLD.deriveFont(34f));
                g.drawString(arrow, barX + barW + 100, y + 50);
            }
            g.setColor(BORDER);
            g.fillRect(60, y + rowH - 8, W - 120, 2);
        }
        footer(g, in.footer(), height - 36);
        return png(img, g);
    }

    // -------------------------------------------------------------- report

    public static byte[] reportCard(ReportInput in) {
        int height = 1320;
        BufferedImage img = new BufferedImage(W, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(img);
        header(g, in.title(), in.period(), 220, false);

        g.setColor(TEXT);
        g.setFont(BOLD.deriveFont(52f));
        g.drawString(fit(g, in.childName(), W - 120), 60, 320);
        g.setColor(MUTED);
        g.setFont(SEMIBOLD.deriveFont(30f));
        g.drawString(fit(g, in.classAndSchool(), W - 120), 60, 368);

        int tileW = (W - 120 - 30) / 2, tileH = 230, tx = 60, ty = 420;
        tile(g, tx, ty, tileW, tileH, in.attendanceLabel(),
                in.attendanceRate() == null ? in.noneText() : fmt1(in.attendanceRate()) + "%",
                rateColor(in.attendanceRate()), null, in.attendanceRate());
        tile(g, tx + tileW + 30, ty, tileW, tileH, in.gradesLabel(),
                in.gradeAverage() == null ? in.noneText() : String.format(java.util.Locale.ROOT, "%.2f", in.gradeAverage()),
                in.gradeAverage() == null ? MUTED : gradeColor(in.gradeAverage()), in.gradesCountText(), null);
        tile(g, tx, ty + tileH + 30, tileW, tileH, in.rewardsLabel(), String.valueOf(in.rewards()), GREEN, null, null);
        tile(g, tx + tileW + 30, ty + tileH + 30, tileW, tileH, in.warningsLabel(), String.valueOf(in.warnings()),
                in.warnings() > 0 ? AMBER : MUTED, null, null);

        int y = ty + 2 * tileH + 110;
        y = chips(g, in.strongLabel(), in.strong(), in.noneText(), new Color(0xD1FAE5), new Color(0x065F46), y);
        chips(g, in.attentionLabel(), in.attention(), in.noneText(), new Color(0xFEF3C7), new Color(0x92400E), y + 40);
        footer(g, in.footer(), height - 36);
        return png(img, g);
    }

    // ------------------------------------------------------- weekly card

    /** Inputs of the 1080×1350 weekly report card sent every Saturday. */
    public record WeeklyInput(String title, String period, String childName, String classAndSchool,
                              String attendanceLabel, Double attendanceRate,
                              String gradesLabel, Double gradeAverage, String gradesNote,
                              String chartTitle, List<SubjectBar> bars, String emptyText,
                              String bestLabel, String best, String attentionLabel, String attention,
                              String noneText, String footer) {
    }

    public static final int WEEKLY_H = 1350;

    public static byte[] weeklyCard(WeeklyInput in) {
        BufferedImage img = new BufferedImage(W, WEEKLY_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(img);

        // header: report title and period, the child in large type
        int headH = 290;
        g.setPaint(new GradientPaint(0, 0, INDIGO, W, headH, VIOLET));
        g.fillRect(0, 0, W, headH);
        g.setColor(new Color(255, 255, 255, 220));
        g.setFont(SEMIBOLD.deriveFont(30f));
        g.drawString(fit(g, in.title() + " · " + in.period(), W - 120), 60, 88);
        g.setColor(Color.WHITE);
        float nameSize = 64f;
        g.setFont(BOLD.deriveFont(nameSize));
        while (nameSize > 44f && g.getFontMetrics().stringWidth(in.childName()) > W - 120) {
            nameSize -= 2f;
            g.setFont(BOLD.deriveFont(nameSize));
        }
        g.drawString(fit(g, in.childName(), W - 120), 60, 176);
        g.setColor(new Color(255, 255, 255, 215));
        g.setFont(SEMIBOLD.deriveFont(30f));
        g.drawString(fit(g, in.classAndSchool(), W - 120), 60, 228);

        // two big numbers
        int tileW = (W - 120 - 30) / 2, tileH = 230, ty = headH + 40;
        tile(g, 60, ty, tileW, tileH, in.attendanceLabel(),
                in.attendanceRate() == null ? in.noneText() : fmt1(in.attendanceRate()) + "%",
                rateColor(in.attendanceRate()), null, in.attendanceRate());
        tile(g, 60 + tileW + 30, ty, tileW, tileH, in.gradesLabel(),
                in.gradeAverage() == null ? in.noneText() : String.format(java.util.Locale.ROOT, "%.2f", in.gradeAverage()),
                in.gradeAverage() == null ? MUTED : gradeColor(in.gradeAverage()), in.gradesNote(), null);

        // subject averages bar chart
        int cy = ty + tileH + 36, ch = 470;
        g.setColor(CARD);
        g.fill(new RoundRectangle2D.Double(60, cy, W - 120, ch, 36, 36));
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(60, cy, W - 120, ch, 36, 36));
        g.setColor(TEXT);
        g.setFont(BOLD.deriveFont(34f));
        g.drawString(fit(g, in.chartTitle(), W - 180), 90, cy + 62);
        List<SubjectBar> bars = in.bars().size() > 6 ? in.bars().subList(0, 6) : in.bars();
        if (bars.isEmpty()) {
            g.setColor(MUTED);
            g.setFont(SEMIBOLD.deriveFont(30f));
            centered(g, in.emptyText(), 60, W - 60, cy + ch / 2 + 20);
        }
        int rowH = 62, labelW = 280, barX = 90 + labelW + 16, barW = W - 60 - 30 - barX - 96;
        for (int i = 0; i < bars.size(); i++) {
            SubjectBar b = bars.get(i);
            int y = cy + 96 + i * rowH;
            g.setColor(TEXT);
            g.setFont(SEMIBOLD.deriveFont(28f));
            g.drawString(fit(g, b.subject(), labelW), 90, y + 34);
            g.setColor(new Color(0xEEF2FF));
            g.fill(new RoundRectangle2D.Double(barX, y + 8, barW, 32, 32, 32));
            g.setColor(gradeColor(b.average()));
            double ratio = Math.max(0.04, Math.min(1, b.average() / 5.0));
            g.fill(new RoundRectangle2D.Double(barX, y + 8, barW * ratio, 32, 32, 32));
            g.setColor(TEXT);
            g.setFont(BOLD.deriveFont(30f));
            g.drawString(String.format(java.util.Locale.ROOT, "%.1f", b.average()), barX + barW + 20, y + 36);
        }

        // best subject and the one that needs attention
        int by = cy + ch + 32, bw = (W - 120 - 30) / 2, bh = 150;
        badge(g, 60, by, bw, bh, new Color(0xD1FAE5), new Color(0x065F46), true, in.bestLabel(),
                in.best() == null ? in.noneText() : in.best());
        badge(g, 60 + bw + 30, by, bw, bh, new Color(0xFEF3C7), new Color(0x92400E), false, in.attentionLabel(),
                in.attention() == null ? in.noneText() : in.attention());

        footer(g, in.footer(), WEEKLY_H - 40);
        return png(img, g);
    }

    /** Rounded badge with a drawn icon: a star for the best subject, a warning triangle otherwise. */
    private static void badge(Graphics2D g, int x, int y, int w, int h, Color fill, Color ink, boolean star,
                              String label, String value) {
        g.setColor(fill);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 32, 32));
        int ix = x + 32, iy = y + 38, s = 56;
        g.setColor(ink);
        java.awt.geom.Path2D p = new java.awt.geom.Path2D.Double();
        if (star) {
            for (int i = 0; i < 10; i++) {
                double a = -Math.PI / 2 + i * Math.PI / 5;
                double r = i % 2 == 0 ? s / 2.0 : s / 4.6;
                double px = ix + s / 2.0 + Math.cos(a) * r, py = iy + s / 2.0 + Math.sin(a) * r;
                if (i == 0) p.moveTo(px, py);
                else p.lineTo(px, py);
            }
            p.closePath();
            g.fill(p);
        } else {
            p.moveTo(ix + s / 2.0, iy);
            p.lineTo(ix + s, iy + s - 4);
            p.lineTo(ix, iy + s - 4);
            p.closePath();
            g.fill(p);
            g.setColor(fill);
            g.setFont(BOLD.deriveFont(34f));
            centered(g, "!", ix, ix + s, iy + s - 12);
        }
        int tx = ix + s + 22, tw = x + w - tx - 24;
        g.setColor(ink);
        g.setFont(SEMIBOLD.deriveFont(24f));
        g.drawString(fit(g, label, tw), tx, y + 60);
        g.setFont(BOLD.deriveFont(34f));
        g.drawString(fit(g, value, tw), tx, y + 108);
    }

    // ------------------------------------------------------------- helpers

    private static Graphics2D graphics(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        return g;
    }

    private static void header(Graphics2D g, String title, String subtitle, int h, boolean badge) {
        g.setPaint(new GradientPaint(0, 0, INDIGO, W, h, VIOLET));
        g.fillRect(0, 0, W, h);
        int maxW = badge ? W - 330 : W - 120;
        g.setColor(Color.WHITE);
        // Long titles (e.g. in Russian) get a smaller font before being shortened.
        float size = 52f;
        g.setFont(BOLD.deriveFont(size));
        while (size > 38f && g.getFontMetrics().stringWidth(title == null ? "" : title) > maxW) {
            size -= 2f;
            g.setFont(BOLD.deriveFont(size));
        }
        g.drawString(fit(g, title, maxW), 60, 100);
        g.setColor(new Color(255, 255, 255, 215));
        g.setFont(SEMIBOLD.deriveFont(32f));
        g.drawString(fit(g, subtitle, maxW), 60, 152);
    }

    private static void rateBadge(Graphics2D g, Double rate, String label) {
        int size = 170, x = W - size - 50, y = 25;
        g.setColor(new Color(255, 255, 255, 40));
        g.fillOval(x, y, size, size);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(10f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (rate != null) {
            g.drawArc(x + 10, y + 10, size - 20, size - 20, 90, (int) -Math.round(rate / 100.0 * 360));
        }
        g.setFont(BOLD.deriveFont(42f));
        centered(g, rate == null ? "—" : fmt0(rate) + "%", x, x + size, y + size / 2 + 12);
        g.setFont(SEMIBOLD.deriveFont(20f));
        g.setColor(new Color(255, 255, 255, 220));
        centered(g, label, x, x + size, y + size / 2 + 44);
    }

    private static void legend(Graphics2D g, Map<String, String> legend, int y) {
        String[] order = {"PRESENT", "LATE", "ABSENT", "EXCUSED", "NONE"};
        g.setFont(SEMIBOLD.deriveFont(26f));
        int total = 0;
        for (String k : order) total += 34 + g.getFontMetrics().stringWidth(legend.getOrDefault(k, k)) + 34;
        int x = (W - total) / 2;
        for (String k : order) {
            Color[] c = DAY_COLORS.get(k);
            g.setColor(c[0]);
            g.fill(new RoundRectangle2D.Double(x, y - 24, 28, 28, 8, 8));
            g.setColor(c[1]);
            g.draw(new RoundRectangle2D.Double(x, y - 24, 28, 28, 8, 8));
            g.setColor(TEXT);
            String text = legend.getOrDefault(k, k);
            g.drawString(text, x + 36, y);
            x += 34 + g.getFontMetrics().stringWidth(text) + 34;
        }
    }

    private static void tile(Graphics2D g, int x, int y, int w, int h, String label, String value, Color valueColor,
                             String note, Double progress) {
        g.setColor(CARD);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 36, 36));
        g.setColor(BORDER);
        g.setStroke(new BasicStroke(2f));
        g.draw(new RoundRectangle2D.Double(x, y, w, h, 36, 36));
        g.setColor(MUTED);
        g.setFont(SEMIBOLD.deriveFont(28f));
        g.drawString(fit(g, label, w - 60), x + 30, y + 56);
        g.setColor(valueColor);
        g.setFont(BOLD.deriveFont(80f));
        g.drawString(value, x + 30, y + 150);
        if (note != null) {
            g.setColor(MUTED);
            g.setFont(SEMIBOLD.deriveFont(24f));
            g.drawString(fit(g, note, w - 60), x + 30, y + 196);
        }
        if (progress != null) {
            int bw = w - 60;
            g.setColor(new Color(0xE2E8F0));
            g.fill(new RoundRectangle2D.Double(x + 30, y + 180, bw, 18, 18, 18));
            g.setColor(valueColor);
            g.fill(new RoundRectangle2D.Double(x + 30, y + 180, bw * Math.max(0.02, progress / 100.0), 18, 18, 18));
        }
    }

    private static int chips(Graphics2D g, String label, List<String> items, String noneText, Color fill, Color text, int y) {
        g.setColor(TEXT);
        g.setFont(BOLD.deriveFont(32f));
        g.drawString(label, 60, y);
        y += 26;
        int x = 60;
        g.setFont(SEMIBOLD.deriveFont(28f));
        if (items == null || items.isEmpty()) {
            g.setColor(MUTED);
            g.drawString(noneText == null ? "—" : noneText, x, y + 36);
            return y + 66;
        }
        for (String item : items) {
            String t = fit(g, item, W - 200);
            int w = g.getFontMetrics().stringWidth(t) + 44;
            if (x + w > W - 60) {
                x = 60;
                y += 66;
            }
            g.setColor(fill);
            g.fill(new RoundRectangle2D.Double(x, y, w, 52, 52, 52));
            g.setColor(text);
            g.drawString(t, x + 22, y + 36);
            x += w + 14;
        }
        return y + 66;
    }

    private static void footer(Graphics2D g, String text, int y) {
        g.setColor(MUTED);
        g.setFont(SEMIBOLD.deriveFont(24f));
        centered(g, text, 0, W, y);
    }

    private static void centered(Graphics2D g, String text, int x1, int x2, int baseline) {
        if (text == null) return;
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, x1 + (x2 - x1 - w) / 2, baseline);
    }

    /** Shortens text with "…" until it fits {@code maxWidth} pixels. */
    static String fit(Graphics2D g, String text, int maxWidth) {
        if (text == null) return "";
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t.stripTrailing() + "…";
    }

    static Color gradeColor(double avg) {
        if (avg >= 4.5) return GREEN;
        if (avg >= 3.5) return INDIGO;
        if (avg >= 2.5) return AMBER;
        return RED;
    }

    static Color rateColor(Double rate) {
        if (rate == null) return MUTED;
        if (rate >= 90) return GREEN;
        if (rate >= 75) return AMBER;
        return RED;
    }

    private static String fmt0(double v) {
        return String.valueOf(Math.round(v));
    }

    private static String fmt1(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    private static byte[] png(BufferedImage img, Graphics2D g) {
        g.dispose();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Monday-first short weekday labels helper for callers. */
    public static List<DayOfWeek> weekOrder() {
        return List.of(DayOfWeek.values());
    }
}
