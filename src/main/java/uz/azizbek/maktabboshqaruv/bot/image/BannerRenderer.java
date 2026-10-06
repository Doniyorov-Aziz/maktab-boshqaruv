package uz.azizbek.maktabboshqaruv.bot.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Section banners (1280×640) shown on top of every bot page: a gradient in the
 * section's color with a drawn icon, the title, the child and the key number
 * on the left, and a white card on the right (stats, a list, a month calendar
 * or a bar chart). Icons are vector shapes — color emoji cannot be drawn by
 * Java2D — and text uses the bundled Inter font, so o', g', ў, ғ, қ, ҳ render
 * correctly on any server.
 */
public final class BannerRenderer {

    static {
        if (System.getProperty("java.awt.headless") == null) System.setProperty("java.awt.headless", "true");
    }

    public static final int W = 1280;
    public static final int H = 640;

    private static final Color TEXT = new Color(0x0F172A);
    private static final Color MUTED = new Color(0x64748B);
    private static final Color LINE = new Color(0xE2E8F0);
    private static final Color TRACK = new Color(0xEEF2F7);
    private static final Font SEMIBOLD = load("/fonts/Inter-SemiBold.ttf");
    private static final Font BOLD = load("/fonts/Inter-Bold.ttf");

    /** Day status fill / text colors, same as the admin panel and the big calendar image. */
    private static final Map<String, Color[]> DAY = Map.of(
            "PRESENT", new Color[]{new Color(0xD1FAE5), new Color(0x047857)},
            "LATE", new Color[]{new Color(0xFEF3C7), new Color(0xB45309)},
            "ABSENT", new Color[]{new Color(0xFEE2E2), new Color(0xB91C1C)},
            "EXCUSED", new Color[]{new Color(0xDBEAFE), new Color(0x1D4ED8)},
            "NONE", new Color[]{new Color(0xF1F5F9), new Color(0x94A3B8)});

    private BannerRenderer() {
    }

    private static Font load(String path) {
        try (InputStream in = BannerRenderer.class.getResourceAsStream(path)) {
            if (in == null) return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (IOException | FontFormatException e) {
            return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
    }

    // ------------------------------------------------------------ inputs

    public enum Icon { HOME, CALENDAR, CHECK, BOOK, CHART, MEGAPHONE, FLAG, PERSON, CHAT, CROSS, GEAR, FAMILY, STAR, SCHOOL, WAVE }

    /**
     * @param bigValue large number on the left ("87%", "4.6"), may be null
     * @param legend   optional small legend under the big value (calendar)
     */
    public record Banner(Color color, Icon icon, String title, String subtitle, String bigValue, String bigLabel,
                         String footer, Panel panel, Map<String, String> legend) {
    }

    public sealed interface Panel permits Stats, Rows, Calendar, Bars {
    }

    public record Stat(String label, String value, Color accent) {
    }

    public record Stats(String heading, List<Stat> stats) implements Panel {
    }

    /** {@code tag} — small colored label on the left (time, date, number); {@code note} — muted text on the right. */
    public record Row(String tag, String text, String note) {
    }

    public record Rows(String heading, List<Row> rows, String emptyText) implements Panel {
    }

    public record Calendar(String heading, List<String> weekdays, LocalDate firstDay, List<String> statuses,
                           LocalDate today) implements Panel {
    }

    public record Bar(String label, double value, String trend) {
    }

    public record Bars(String heading, List<Bar> bars, String emptyText) implements Panel {
    }

    // ------------------------------------------------------------ drawing

    public static byte[] render(Banner b) {
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        background(g, b.color());
        left(g, b);
        if (b.panel() != null) card(g, b);
        g.dispose();
        return png(img);
    }

    private static void background(Graphics2D g, Color c) {
        g.setPaint(new GradientPaint(0, 0, c, W, H, shade(c, 0.62f)));
        g.fillRect(0, 0, W, H);
        g.setColor(new Color(255, 255, 255, 26));
        g.fill(new Ellipse2D.Double(-180, 330, 520, 520));
        g.fill(new Ellipse2D.Double(380, -260, 420, 420));
        g.setColor(new Color(255, 255, 255, 14));
        g.fill(new Ellipse2D.Double(140, 470, 260, 260));
    }

    private static void left(Graphics2D g, Banner b) {
        int x = 64;
        // icon tile
        g.setColor(new Color(255, 255, 255, 46));
        g.fill(new RoundRectangle2D.Double(x, 60, 112, 112, 32, 32));
        drawIcon(g, b.icon(), x + 24, 84, 64);

        int maxW = b.panel() == null ? W - 128 : 500;
        g.setColor(Color.WHITE);
        float size = 60f;
        g.setFont(BOLD.deriveFont(size));
        while (size > 38f && width(g, b.title()) > maxW) {
            size -= 2f;
            g.setFont(BOLD.deriveFont(size));
        }
        g.drawString(fit(g, b.title(), maxW), x, 250);
        g.setColor(new Color(255, 255, 255, 225));
        g.setFont(SEMIBOLD.deriveFont(30f));
        List<String> sub = wrap(g, b.subtitle(), maxW, b.bigValue() == null ? 2 : 1);
        for (int i = 0; i < sub.size(); i++) g.drawString(sub.get(i), x, 298 + i * 40);

        if (b.bigValue() != null) {
            g.setColor(Color.WHITE);
            float big = 116f;
            g.setFont(BOLD.deriveFont(big));
            while (big > 60f && width(g, b.bigValue()) > maxW) {
                big -= 4f;
                g.setFont(BOLD.deriveFont(big));
            }
            g.drawString(b.bigValue(), x - 4, 440);
            if (b.bigLabel() != null) {
                g.setColor(new Color(255, 255, 255, 215));
                g.setFont(SEMIBOLD.deriveFont(28f));
                g.drawString(fit(g, b.bigLabel(), maxW), x, 486);
            }
        }
        if (b.legend() != null && !b.legend().isEmpty()) legend(g, b.legend(), x, 530);

        if (b.footer() != null) {
            g.setColor(new Color(255, 255, 255, 190));
            g.setFont(SEMIBOLD.deriveFont(22f));
            g.drawString(fit(g, b.footer(), maxW), x, 600);
        }
    }

    private static void legend(Graphics2D g, Map<String, String> legend, int x, int y) {
        g.setFont(SEMIBOLD.deriveFont(20f));
        int cx = x, cy = y;
        for (String k : new String[]{"PRESENT", "LATE", "ABSENT", "EXCUSED"}) {
            String label = legend.get(k);
            if (label == null) continue;
            int w = 30 + width(g, label) + 26;
            if (cx + w > x + 520) {
                cx = x;
                cy += 32;
            }
            g.setColor(DAY.get(k)[0]);
            g.fill(new RoundRectangle2D.Double(cx, cy - 18, 20, 20, 6, 6));
            g.setColor(new Color(255, 255, 255, 235));
            g.drawString(label, cx + 28, cy);
            cx += w;
        }
    }

    private static void card(Graphics2D g, Banner b) {
        int x = 600, y = 56, w = 616, h = 528;
        g.setColor(new Color(15, 23, 42, 40));
        g.fill(new RoundRectangle2D.Double(x + 4, y + 10, w, h, 40, 40));
        g.setColor(Color.WHITE);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 40, 40));
        int ix = x + 36, iy = y + 36, iw = w - 72, ih = h - 72;
        switch (b.panel()) {
            case Stats s -> stats(g, s, b.color(), ix, iy, iw, ih);
            case Rows r -> rows(g, r, b.color(), ix, iy, iw, ih);
            case Calendar c -> calendar(g, c, b.color(), ix, iy, iw, ih);
            case Bars bars -> bars(g, bars, b.color(), ix, iy, iw, ih);
        }
    }

    private static int heading(Graphics2D g, String text, int x, int y, int w) {
        if (text == null) return y;
        g.setColor(TEXT);
        g.setFont(BOLD.deriveFont(30f));
        g.drawString(fit(g, text, w), x, y + 30);
        return y + 56;
    }

    private static void stats(Graphics2D g, Stats s, Color color, int x, int y, int w, int h) {
        int top = heading(g, s.heading(), x, y, w);
        List<Stat> list = s.stats().size() > 4 ? s.stats().subList(0, 4) : s.stats();
        // Two stats stack as full-width tiles; three or four make a 2×2 grid.
        int cols = list.size() <= 2 ? 1 : 2, rows = (list.size() + cols - 1) / cols, gap = 20;
        // Tiles share the card's height, so two stats fill it as well as four.
        int tw = (w - gap * (cols - 1)) / cols, th = (y + h - top - gap * (rows - 1)) / Math.max(1, rows);
        for (int i = 0; i < list.size(); i++) {
            Stat st = list.get(i);
            int tx = x + (i % cols) * (tw + gap), ty = top + (i / cols) * (th + gap);
            Color accent = st.accent() != null ? st.accent() : color;
            g.setColor(tint(accent, 0.10f));
            g.fill(new RoundRectangle2D.Double(tx, ty, tw, th, 28, 28));
            g.setColor(MUTED);
            float labelSize = 24f;
            g.setFont(SEMIBOLD.deriveFont(labelSize));
            while (labelSize > 17f && width(g, st.label()) > tw - 48) {
                labelSize -= 1f;
                g.setFont(SEMIBOLD.deriveFont(labelSize));
            }
            g.drawString(fit(g, st.label(), tw - 48), tx + 24, ty + 46);
            g.setColor(shade(accent, 0.85f));
            float size = 64f;
            g.setFont(BOLD.deriveFont(size));
            while (size > 30f && width(g, st.value()) > tw - 48) {
                size -= 4f;
                g.setFont(BOLD.deriveFont(size));
            }
            g.drawString(st.value(), tx + 24, ty + th - 36);
        }
    }

    private static void rows(Graphics2D g, Rows r, Color color, int x, int y, int w, int h) {
        int top = heading(g, r.heading(), x, y, w);
        if (r.rows().isEmpty()) {
            empty(g, r.emptyText(), x, top, w, y + h - top);
            return;
        }
        int rowH = 64, max = Math.min(r.rows().size(), (y + h - top) / rowH);
        g.setFont(BOLD.deriveFont(24f));
        int tagW = 0;
        for (int i = 0; i < max; i++) {
            String tag = r.rows().get(i).tag();
            if (tag != null) tagW = Math.max(tagW, width(g, tag) + 28);
        }
        for (int i = 0; i < max; i++) {
            Row row = r.rows().get(i);
            int ry = top + i * rowH;
            int tx = x;
            if (row.tag() != null) {
                g.setFont(BOLD.deriveFont(24f));
                g.setColor(tint(color, 0.13f));
                g.fill(new RoundRectangle2D.Double(tx, ry + 10, tagW, 42, 14, 14));
                g.setColor(shade(color, 0.8f));
                g.drawString(row.tag(), tx + (tagW - width(g, row.tag())) / 2, ry + 40);
                tx += tagW + 18;
            }
            int noteW = 0;
            if (row.note() != null) {
                g.setFont(SEMIBOLD.deriveFont(22f));
                String note = fit(g, row.note(), 190);
                noteW = width(g, note) + 12;
                g.setColor(MUTED);
                g.drawString(note, x + w - width(g, note), ry + 40);
            }
            g.setColor(TEXT);
            g.setFont(SEMIBOLD.deriveFont(27f));
            g.drawString(fit(g, row.text(), x + w - tx - noteW - 8), tx, ry + 41);
            if (i < max - 1) {
                g.setColor(LINE);
                g.setStroke(new BasicStroke(2f));
                g.draw(new Line2D.Double(x, ry + rowH - 1, x + w, ry + rowH - 1));
            }
        }
        if (r.rows().size() > max) {
            g.setColor(MUTED);
            g.setFont(SEMIBOLD.deriveFont(22f));
            String more = "+" + (r.rows().size() - max);
            g.drawString(more, x + w - width(g, more), y + h + 18);
        }
    }

    private static void calendar(Graphics2D g, Calendar c, Color color, int x, int y, int w, int h) {
        int top = heading(g, c.heading(), x, y, w);
        int gap = 8, cell = (w - 6 * gap) / 7;
        int lead = c.firstDay().getDayOfWeek().getValue() - 1;
        int days = c.statuses().size();
        int rows = (lead + days + 6) / 7;
        int dowH = 34;
        int cellH = Math.min(cell, (y + h - top - dowH - gap * (rows - 1)) / rows);
        g.setFont(SEMIBOLD.deriveFont(20f));
        for (int i = 0; i < 7 && i < c.weekdays().size(); i++) {
            String d = c.weekdays().get(i);
            g.setColor(i == 6 ? new Color(0xEF4444) : MUTED);
            g.drawString(d, x + i * (cell + gap) + (cell - width(g, d)) / 2, top + 22);
        }
        int gy = top + dowH;
        g.setFont(BOLD.deriveFont(24f));
        for (int d = 0; d < days; d++) {
            int pos = lead + d, col = pos % 7, row = pos / 7;
            int cx = x + col * (cell + gap), cy = gy + row * (cellH + gap);
            Color[] colors = DAY.getOrDefault(c.statuses().get(d), DAY.get("NONE"));
            g.setColor(colors[0]);
            g.fill(new RoundRectangle2D.Double(cx, cy, cell, cellH, 16, 16));
            LocalDate date = c.firstDay().plusDays(d);
            if (date.equals(c.today())) {
                g.setColor(color);
                g.setStroke(new BasicStroke(4f));
                g.draw(new RoundRectangle2D.Double(cx + 2, cy + 2, cell - 4, cellH - 4, 14, 14));
            }
            g.setColor(colors[1]);
            String n = String.valueOf(d + 1);
            g.drawString(n, cx + (cell - width(g, n)) / 2, cy + cellH / 2 + 9);
        }
    }

    private static void bars(Graphics2D g, Bars b, Color color, int x, int y, int w, int h) {
        int top = heading(g, b.heading(), x, y, w);
        if (b.bars().isEmpty()) {
            empty(g, b.emptyText(), x, top, w, y + h - top);
            return;
        }
        int max = Math.min(b.bars().size(), 7);
        int rowH = Math.min(62, (y + h - top) / max);
        int labelW = 190, valueW = 92;
        int bx = x + labelW + 14, bw = w - labelW - 14 - valueW;
        for (int i = 0; i < max; i++) {
            Bar bar = b.bars().get(i);
            int ry = top + i * rowH;
            g.setColor(TEXT);
            g.setFont(SEMIBOLD.deriveFont(23f));
            g.drawString(fit(g, bar.label(), labelW), x, ry + rowH / 2 + 8);
            g.setColor(TRACK);
            g.fill(new RoundRectangle2D.Double(bx, ry + rowH / 2 - 13, bw, 26, 26, 26));
            Color fill = gradeColor(bar.value(), color);
            g.setColor(fill);
            double ratio = Math.max(0.04, Math.min(1, bar.value() / 5.0));
            g.fill(new RoundRectangle2D.Double(bx, ry + rowH / 2 - 13, bw * ratio, 26, 26, 26));
            g.setColor(TEXT);
            g.setFont(BOLD.deriveFont(26f));
            String v = String.format(java.util.Locale.ROOT, "%.1f", bar.value());
            g.drawString(v, bx + bw + 12, ry + rowH / 2 + 9);
            trendArrow(g, bar.trend(), bx + bw + 12 + width(g, v) + 10, ry + rowH / 2);
        }
    }

    private static void trendArrow(Graphics2D g, String trend, int x, int cy) {
        if (!"UP".equals(trend) && !"DOWN".equals(trend)) return;
        boolean up = "UP".equals(trend);
        g.setColor(up ? new Color(0x10B981) : new Color(0xEF4444));
        g.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int dir = up ? -1 : 1;
        g.draw(new Line2D.Double(x + 8, cy - 10 * dir, x + 8, cy + 10 * dir));
        Path2D head = new Path2D.Double();
        head.moveTo(x + 1, cy + 3 * dir);
        head.lineTo(x + 8, cy + 10 * dir);
        head.lineTo(x + 15, cy + 3 * dir);
        g.draw(head);
    }

    private static void empty(Graphics2D g, String text, int x, int y, int w, int h) {
        g.setColor(MUTED);
        g.setFont(SEMIBOLD.deriveFont(28f));
        String t = fit(g, text == null ? "—" : text, w);
        g.drawString(t, x + (w - width(g, t)) / 2, y + h / 2);
    }

    // ------------------------------------------------------------ icons

    /** White line icons in a {@code s}×{@code s} box. */
    static void drawIcon(Graphics2D g, Icon icon, int x, int y, int s) {
        Graphics2D c = (Graphics2D) g.create();
        c.translate(x, y);
        c.scale(s / 64.0, s / 64.0);
        c.setColor(Color.WHITE);
        c.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (icon) {
            case HOME -> {
                Path2D p = new Path2D.Double();
                p.moveTo(8, 30); p.lineTo(32, 9); p.lineTo(56, 30);
                c.draw(p);
                c.draw(new RoundRectangle2D.Double(15, 28, 34, 28, 6, 6));
                c.fill(new RoundRectangle2D.Double(27, 40, 10, 16, 3, 3));
            }
            case CALENDAR, FLAG -> {
                c.draw(new RoundRectangle2D.Double(8, 13, 48, 44, 10, 10));
                c.draw(new Line2D.Double(8, 26, 56, 26));
                c.draw(new Line2D.Double(21, 7, 21, 17));
                c.draw(new Line2D.Double(43, 7, 43, 17));
                if (icon == Icon.FLAG) {
                    star(c, 32, 42, 10, true);
                } else {
                    for (int i = 0; i < 3; i++) {
                        c.fill(new Ellipse2D.Double(16 + i * 13, 34, 6, 6));
                        c.fill(new Ellipse2D.Double(16 + i * 13, 45, 6, 6));
                    }
                }
            }
            case CHECK -> {
                c.draw(new Ellipse2D.Double(7, 7, 50, 50));
                Path2D p = new Path2D.Double();
                p.moveTo(20, 33); p.lineTo(28, 41); p.lineTo(44, 24);
                c.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                c.draw(p);
            }
            case BOOK -> {
                Path2D p = new Path2D.Double();
                p.moveTo(32, 16); p.curveTo(24, 10, 14, 10, 7, 13); p.lineTo(7, 52);
                p.curveTo(14, 49, 24, 49, 32, 55); p.curveTo(40, 49, 50, 49, 57, 52);
                p.lineTo(57, 13); p.curveTo(50, 10, 40, 10, 32, 16); p.closePath();
                c.draw(p);
                c.draw(new Line2D.Double(32, 16, 32, 54));
            }
            case CHART -> {
                c.draw(new Line2D.Double(8, 56, 56, 56));
                c.fill(new RoundRectangle2D.Double(12, 34, 10, 18, 4, 4));
                c.fill(new RoundRectangle2D.Double(27, 22, 10, 30, 4, 4));
                c.fill(new RoundRectangle2D.Double(42, 10, 10, 42, 4, 4));
            }
            case MEGAPHONE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(10, 26); p.lineTo(22, 26); p.lineTo(46, 12); p.lineTo(46, 52);
                p.lineTo(22, 38); p.lineTo(10, 38); p.closePath();
                c.draw(p);
                c.draw(new Line2D.Double(18, 39, 22, 54));
                c.draw(new Arc2D.Double(44, 22, 14, 20, -60, 120, Arc2D.OPEN));
            }
            case PERSON -> {
                c.draw(new Ellipse2D.Double(21, 8, 22, 22));
                c.draw(new Arc2D.Double(10, 34, 44, 40, 0, 180, Arc2D.OPEN));
            }
            case CHAT -> {
                Path2D p = new Path2D.Double();
                p.moveTo(14, 10); p.lineTo(50, 10); p.quadTo(57, 10, 57, 17); p.lineTo(57, 38);
                p.quadTo(57, 45, 50, 45); p.lineTo(28, 45); p.lineTo(16, 55); p.lineTo(17, 45);
                p.lineTo(14, 45); p.quadTo(7, 45, 7, 38); p.lineTo(7, 17); p.quadTo(7, 10, 14, 10); p.closePath();
                c.draw(p);
                for (int i = 0; i < 3; i++) c.fill(new Ellipse2D.Double(19 + i * 11, 25, 6, 6));
            }
            case CROSS -> {
                c.draw(new RoundRectangle2D.Double(8, 8, 48, 48, 14, 14));
                c.setStroke(new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                c.draw(new Line2D.Double(32, 19, 32, 45));
                c.draw(new Line2D.Double(19, 32, 45, 32));
            }
            case GEAR -> {
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    c.draw(new Line2D.Double(32 + Math.cos(a) * 18, 32 + Math.sin(a) * 18,
                            32 + Math.cos(a) * 26, 32 + Math.sin(a) * 26));
                }
                c.draw(new Ellipse2D.Double(14, 14, 36, 36));
                c.draw(new Ellipse2D.Double(25, 25, 14, 14));
            }
            case FAMILY -> {
                c.draw(new Ellipse2D.Double(10, 10, 16, 16));
                c.draw(new Ellipse2D.Double(38, 10, 16, 16));
                c.draw(new Arc2D.Double(4, 30, 28, 34, 0, 180, Arc2D.OPEN));
                c.draw(new Arc2D.Double(32, 30, 28, 34, 0, 180, Arc2D.OPEN));
                c.fill(new Ellipse2D.Double(27, 34, 10, 10));
            }
            case STAR -> star(c, 32, 33, 26, false);
            case SCHOOL -> {
                Path2D roof = new Path2D.Double();
                roof.moveTo(6, 26); roof.lineTo(32, 10); roof.lineTo(58, 26);
                c.draw(roof);
                c.draw(new Line2D.Double(10, 56, 54, 56));
                for (int i = 0; i < 4; i++) c.draw(new Line2D.Double(15 + i * 11.3, 32, 15 + i * 11.3, 50));
            }
            case WAVE -> {
                Path2D p = new Path2D.Double();
                p.moveTo(20, 56); p.curveTo(10, 46, 8, 34, 14, 28); p.lineTo(24, 36);
                p.lineTo(24, 12); p.quadTo(24, 7, 29, 7); p.quadTo(34, 7, 34, 12); p.lineTo(34, 28);
                p.lineTo(34, 9); p.quadTo(34, 4, 39, 4); p.quadTo(44, 4, 44, 9); p.lineTo(44, 30);
                p.lineTo(44, 14); p.quadTo(44, 9, 49, 9); p.quadTo(54, 9, 54, 14); p.lineTo(54, 40);
                p.curveTo(54, 50, 48, 56, 40, 56); p.closePath();
                c.draw(p);
            }
        }
        c.dispose();
    }

    private static void star(Graphics2D g, double cx, double cy, double r, boolean fill) {
        Path2D p = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            double rr = i % 2 == 0 ? r : r * 0.45;
            double px = cx + Math.cos(a) * rr, py = cy + Math.sin(a) * rr;
            if (i == 0) p.moveTo(px, py);
            else p.lineTo(px, py);
        }
        p.closePath();
        if (fill) g.fill(p);
        else g.draw(p);
    }

    // ------------------------------------------------------------ helpers

    static Color gradeColor(double avg, Color brand) {
        if (avg >= 4.5) return new Color(0x10B981);
        if (avg >= 3.5) return brand;
        if (avg >= 2.5) return new Color(0xF59E0B);
        return new Color(0xEF4444);
    }

    /** Darker variant: {@code k} = 1 keeps the color, smaller is darker. */
    static Color shade(Color c, float k) {
        return new Color(Math.round(c.getRed() * k), Math.round(c.getGreen() * k), Math.round(c.getBlue() * k));
    }

    /** Pale variant mixed with white ({@code k} = share of the color). */
    static Color tint(Color c, float k) {
        return new Color(Math.round(255 - (255 - c.getRed()) * k), Math.round(255 - (255 - c.getGreen()) * k),
                Math.round(255 - (255 - c.getBlue()) * k));
    }

    private static int width(Graphics2D g, String s) {
        return s == null ? 0 : g.getFontMetrics().stringWidth(s);
    }

    /** Word-wraps into at most {@code maxLines} lines; the last one is shortened with "…" if needed. */
    static List<String> wrap(Graphics2D g, String text, int maxWidth, int maxLines) {
        List<String> lines = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) return lines;
        StringBuilder line = new StringBuilder();
        String[] words = text.split(" ");
        for (int i = 0; i < words.length; i++) {
            String candidate = line.isEmpty() ? words[i] : line + " " + words[i];
            if (width(g, candidate) <= maxWidth || line.isEmpty()) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (lines.size() == maxLines - 1) {
                line.append(" ").append(String.join(" ", java.util.Arrays.copyOfRange(words, i, words.length)));
                break;
            }
            lines.add(line.toString());
            line = new StringBuilder(words[i]);
        }
        lines.add(fit(g, line.toString(), maxWidth));
        return lines;
    }

    static String fit(Graphics2D g, String text, int maxWidth) {
        if (text == null) return "";
        FontMetrics fm = g.getFontMetrics();
        if (fm.stringWidth(text) <= maxWidth) return text;
        String t = text;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t.stripTrailing() + "…";
    }

    private static byte[] png(BufferedImage img) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
