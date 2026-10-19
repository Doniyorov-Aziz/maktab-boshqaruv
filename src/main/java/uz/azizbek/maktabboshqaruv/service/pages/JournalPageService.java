package uz.azizbek.maktabboshqaruv.service.pages;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.JournalService;
import uz.azizbek.maktabboshqaruv.service.TimetablePdfService;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardMetrics;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.util.XlsxWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

import static uz.azizbek.maktabboshqaruv.service.pages.PageSql.*;

/**
 * "Baholar jurnali" beside the journal itself (JournalService): last month's numbers for the
 * change pills, one student in full (the side panel), who came on a day (the "whole class" panel
 * greys out the absent), and the month as Excel or PDF.
 */
@Service
public class JournalPageService {

    public record Stats(Double average, Double mastery, Double quality, int count) {
    }

    public record Summary(String month, Stats previous) {
    }

    public record GradePoint(LocalDate date, int value, String type, String comment, String source) {
    }

    public record AvgPoint(LocalDate date, Double average) {
    }

    public record StudentCard(long studentId, String name, String className, String guardianName, String guardianRelation,
                              String guardianPhone, boolean telegram, List<GradePoint> grades, List<AvgPoint> trend,
                              Double average, Double attendance30, Integer risk) {
    }

    private final NamedParameterJdbcTemplate jdbc;
    private final JournalService journal;
    private final HomeDashboardService home;
    private final DashboardCache cache;
    private final Clock clock;

    public JournalPageService(NamedParameterJdbcTemplate jdbc, JournalService journal, HomeDashboardService home,
                              DashboardCache cache, Clock clock) {
        this.jdbc = jdbc;
        this.journal = journal;
        this.home = home;
        this.cache = cache;
        this.clock = clock;
    }

    /** Last month's average, mastery, quality and count for the class and subject. */
    @Transactional(readOnly = true)
    public Summary summary(long classId, long subjectId, YearMonth month) {
        YearMonth prev = month.minusMonths(1);
        Map<String, Object> r = jdbc.queryForMap("""
                select count(*) as n, avg(g.score) as avg,
                       count(*) filter (where g.score >= 3) as passed, count(*) filter (where g.score >= 4) as good
                  from grade g join students st on st.id = g.student_id
                 where st.class_id = :c and g.subject_id = :sub and g.grade_date between :from and :to""",
                new MapSqlParameterSource("c", classId).addValue("sub", subjectId)
                        .addValue("from", prev.atDay(1)).addValue("to", prev.atEndOfMonth()));
        int n = i(r.get("n"));
        Double avg = r.get("avg") == null ? null : Math.round(((Number) r.get("avg")).doubleValue() * 100) / 100.0;
        return new Summary(month.toString(), new Stats(avg, DashboardMetrics.pct(l(r.get("passed")), n),
                DashboardMetrics.pct(l(r.get("good")), n), n));
    }

    /** Who came on that date: the subject's lesson that day if there was one, else the student's latest lesson. */
    @Transactional(readOnly = true)
    public Map<Long, String> dayAttendance(long classId, long subjectId, LocalDate date) {
        Map<Long, String> out = new HashMap<>();
        for (Map<String, Object> r : jdbc.queryForList("""
                select distinct on (a.student_id) a.student_id, a.status
                  from attendance a join lesson_slot ls on ls.id = a.lesson_slot_id
                 where ls.class_id = :c and a.record_date = :d
                 order by a.student_id, (ls.subject_id = :sub) desc, ls.start_time desc""",
                new MapSqlParameterSource("c", classId).addValue("d", date).addValue("sub", subjectId))) {
            out.put(l(r.get("student_id")), (String) r.get("status"));
        }
        return out;
    }

    /** One student in this subject: every grade of the school year, a 30-day running average, attendance, risk. */
    @Transactional(readOnly = true)
    public StudentCard student(long studentId, long subjectId, boolean withPhones) {
        List<Map<String, Object>> who = jdbc.queryForList("""
                select st.first_name, st.last_name, st.guardian_name, st.guardian_relation, st.guardian_phone, st.class_id,
                       c.grade_number || '-' || c.section_letter as class_name, y.school_id,
                       exists (select 1 from parent_telegram_link t where t.student_id = st.id and t.active) as telegram
                  from students st join school_class c on c.id = st.class_id join academic_year y on y.id = c.academic_year_id
                 where st.id = :id""", new MapSqlParameterSource("id", studentId));
        if (who.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Bunday o'quvchi topilmadi");
        Map<String, Object> w = who.get(0);
        long schoolId = l(w.get("school_id"));
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(365);
        List<GradePoint> grades = jdbc.query("""
                select grade_date, score, type, comment, source from grade
                 where student_id = :id and subject_id = :sub and grade_date between :from and :to
                 order by grade_date, id""",
                new MapSqlParameterSource("id", studentId).addValue("sub", subjectId).addValue("from", from).addValue("to", today),
                (rs, n) -> new GradePoint(rs.getDate("grade_date").toLocalDate(), rs.getInt("score"), rs.getString("type"),
                        rs.getString("comment"), rs.getString("source")));
        // the average of the 30 days up to each graded day of the last 60 days
        List<AvgPoint> trend = new ArrayList<>();
        for (LocalDate d = today.minusDays(59); !d.isAfter(today); d = d.plusDays(1)) {
            LocalDate day = d;
            boolean graded = grades.stream().anyMatch(g -> g.date().equals(day));
            if (!graded) continue;
            double sum = 0;
            int n = 0;
            for (GradePoint g : grades) {
                if (g.date().isAfter(day) || g.date().isBefore(day.minusDays(29))) continue;
                sum += g.value();
                n++;
            }
            trend.add(new AvgPoint(day, n == 0 ? null : Math.round(sum / n * 100) / 100.0));
        }
        Double average = grades.isEmpty() ? null
                : Math.round(grades.stream().mapToInt(GradePoint::value).average().orElse(0) * 100) / 100.0;
        long[] att = home.studentDays(schoolId, l(w.get("class_id")), today.minusDays(29), today)
                .getOrDefault(studentId, new long[2]);
        Integer risk = cache.get(schoolId, "risk:2000:false", () -> home.riskStudents(schoolId, null, 2000, false)).data()
                .rows().stream().filter(x -> x.studentId() == studentId).map(HomeDashboardService.RiskRow::score)
                .findFirst().orElse(null);
        return new StudentCard(studentId, ((String) w.get("first_name") + " " + w.get("last_name")).trim(),
                (String) w.get("class_name"), (String) w.get("guardian_name"), (String) w.get("guardian_relation"),
                withPhones ? (String) w.get("guardian_phone") : null, Boolean.TRUE.equals(w.get("telegram")),
                grades, trend, average, DashboardMetrics.pct(att[0], att[0] + att[1]), risk);
    }

    // ------------------------------------------------------------------ export

    private static String title(JournalService.Journal j, Map<String, Object> names) {
        return names.get("class_name") + " · " + names.get("subject") + " · " + j.month();
    }

    private Map<String, Object> names(long classId, long subjectId) {
        return jdbc.queryForMap("""
                select (select grade_number || '-' || section_letter from school_class where id = :c) as class_name,
                       (select name from subject where id = :s) as subject""",
                new MapSqlParameterSource("c", classId).addValue("s", subjectId));
    }

    /** grades by student, then by date (several a day joined "5/4"), and each student's average. */
    private static Map<Long, Map<LocalDate, String>> table(JournalService.Journal j, Map<Long, double[]> sums) {
        Map<Long, Map<LocalDate, String>> t = new HashMap<>();
        for (JournalService.GradeCell g : j.grades()) {
            t.computeIfAbsent(g.studentId(), k -> new HashMap<>()).merge(g.date(), String.valueOf(g.value()), (a, b) -> a + "/" + b);
            double[] s = sums.computeIfAbsent(g.studentId(), k -> new double[2]);
            s[0] += g.value();
            s[1]++;
        }
        return t;
    }

    @Transactional(readOnly = true)
    public byte[] xlsx(long classId, long subjectId, YearMonth month) {
        JournalService.Journal j = journal.journal(classId, subjectId, month);
        Map<Long, double[]> sums = new HashMap<>();
        Map<Long, Map<LocalDate, String>> t = table(j, sums);
        List<String> header = new ArrayList<>(List.of("№", "O'quvchi"));
        List<Integer> widths = new ArrayList<>(List.of(5, 28));
        for (JournalService.Day d : j.days()) {
            header.add(String.format("%02d %s", d.date().getDayOfMonth(), d.weekday()));
            widths.add(7);
        }
        header.add("O'rtacha");
        widths.add(10);
        header.add("Soni");
        widths.add(7);
        List<List<Object>> rows = new ArrayList<>();
        int no = 0;
        for (JournalService.StudentRow s : j.students()) {
            List<Object> row = new ArrayList<>(List.of(++no, s.fullName()));
            for (JournalService.Day d : j.days()) row.add(t.getOrDefault(s.id(), Map.of()).getOrDefault(d.date(), ""));
            double[] sum = sums.get(s.id());
            row.add(sum == null ? "" : Math.round(sum[0] / sum[1] * 100) / 100.0);
            row.add(sum == null ? 0 : (int) sum[1]);
            rows.add(row);
        }
        return XlsxWriter.write("Jurnal", header, widths, rows);
    }

    @Transactional(readOnly = true)
    public byte[] pdf(long classId, long subjectId, YearMonth month) {
        JournalService.Journal j = journal.journal(classId, subjectId, month);
        Map<String, Object> names = names(classId, subjectId);
        Map<Long, double[]> sums = new HashMap<>();
        Map<Long, Map<LocalDate, String>> t = table(j, sums);
        BaseFont regular = TimetablePdfService.font("/fonts/Inter-Regular.ttf");
        BaseFont bold = TimetablePdfService.font("/fonts/Inter-Bold.ttf");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 18, 18, 22, 18);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph(title(j, names) + " — baholar jurnali", new Font(bold, 14, Font.NORMAL, new Color(0x1e293b))));
            doc.add(new Paragraph(" ", new Font(regular, 4)));
            int days = j.days().size();
            float[] widths = new float[days + 3];
            widths[0] = 2.2f;
            widths[1] = 11f;
            for (int k = 0; k < days; k++) widths[k + 2] = 1.55f;
            widths[days + 2] = 3f;
            PdfPTable table = new PdfPTable(widths);
            table.setWidthPercentage(100);
            Font head = new Font(bold, 6.5f, Font.NORMAL, Color.WHITE);
            Font cell = new Font(regular, 7, Font.NORMAL, new Color(0x0f172a));
            Color brand = new Color(0x6d28d9);
            for (String h : List.of("№", "O'quvchi")) table.addCell(box(h, head, brand));
            for (JournalService.Day d : j.days()) {
                table.addCell(box(String.format("%02d%n%s", d.date().getDayOfMonth(), d.weekday()), head,
                        d.isSunday() ? new Color(0x94a3b8) : brand));
            }
            table.addCell(box("O'rt.", head, brand));
            int no = 0;
            for (JournalService.StudentRow s : j.students()) {
                table.addCell(box(String.valueOf(++no), cell, null));
                PdfPCell name = box(s.fullName(), cell, null);
                name.setHorizontalAlignment(Element.ALIGN_LEFT);
                table.addCell(name);
                for (JournalService.Day d : j.days()) {
                    String v = t.getOrDefault(s.id(), Map.of()).getOrDefault(d.date(), "");
                    table.addCell(box(v, cell, d.isSunday() ? new Color(0xf1f5f9) : gradeTint(v)));
                }
                double[] sum = sums.get(s.id());
                table.addCell(box(sum == null ? "—" : String.format("%.2f", sum[0] / sum[1]), new Font(bold, 7), null));
            }
            doc.add(table);
            doc.close();
        } catch (Exception e) {
            throw new IllegalStateException("PDF tayyorlab bo'lmadi: " + e.getMessage(), e);
        }
        return out.toByteArray();
    }

    private static PdfPCell box(String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(2.5f);
        c.setBorderColor(new Color(0xe2e8f0));
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }

    /** The grade colours of the page, light (the first grade of the cell decides). */
    private static Color gradeTint(String v) {
        if (v == null || v.isEmpty()) return null;
        return switch (v.charAt(0)) {
            case '5' -> new Color(0xdcfce7);
            case '4' -> new Color(0xdbeafe);
            case '3' -> new Color(0xfef9c3);
            case '2' -> new Color(0xfee2e2);
            default -> null;
        };
    }
}
