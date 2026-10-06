package uz.azizbek.maktabboshqaruv.service;

import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import uz.azizbek.maktabboshqaruv.dto.TimetableEntryDto;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalTime;
import java.util.*;

/**
 * Weekly timetable as a PDF: one A4 landscape page per class, the whole week on that one
 * page (days across, lessons down). The Inter font is embedded, so Latin and Cyrillic
 * letters print the same everywhere; subject colours match the web page.
 */
@Service
public class TimetablePdfService {

    static final List<String> WEEKDAYS = List.of("Dushanba", "Seshanba", "Chorshanba", "Payshanba", "Juma", "Shanba");
    /** Same palette and hash as the timetable page (TimetablePage.vue#subjectColor). */
    private static final String[] PALETTE = {"#4f46e5", "#0ea5e9", "#10b981", "#f59e0b", "#ec4899", "#8b5cf6",
            "#14b8a6", "#ef4444", "#3b82f6", "#22c55e", "#eab308", "#f43f5e"};

    private final LessonSlotService lessonSlotService;
    private final SchoolClassRepository schoolClassRepository;
    private final SchoolRepository schoolRepository;
    private final BaseFont regular;
    private final BaseFont bold;

    public TimetablePdfService(LessonSlotService lessonSlotService, SchoolClassRepository schoolClassRepository,
                               SchoolRepository schoolRepository) {
        this.lessonSlotService = lessonSlotService;
        this.schoolClassRepository = schoolClassRepository;
        this.schoolRepository = schoolRepository;
        this.regular = font("/fonts/Inter-Regular.ttf");
        this.bold = font("/fonts/Inter-Bold.ttf");
    }

    private static BaseFont font(String path) {
        try (InputStream in = TimetablePdfService.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Shrift topilmadi: " + path);
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(path.substring(path.lastIndexOf('/') + 1), BaseFont.IDENTITY_H,
                    BaseFont.EMBEDDED, true, bytes, null);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Shriftni yuklab bo'lmadi: " + path, e);
        }
    }

    static Color subjectColor(String name) {
        int hash = 0;
        for (char ch : (name == null ? "" : name).toCharArray()) hash = (hash * 31 + ch) % PALETTE.length;
        return Color.decode(PALETTE[Math.abs(hash) % PALETTE.length]);
    }

    /** One class, or every class of the school (classId = null), each on its own page. */
    @Transactional(readOnly = true)
    public byte[] render(Long schoolId, Long classId) {
        List<SchoolClass> classes = classId != null
                ? schoolClassRepository.findById(classId).filter(c -> c.getAcademicYear().getSchool().getId().equals(schoolId))
                .map(List::of).orElseThrow(() -> new IllegalStateException("Bu maktabda bunday sinf yo'q"))
                : schoolClassRepository.findByAcademicYearSchoolId(schoolId).stream()
                .sorted(Comparator.comparing(SchoolClass::getGradeNumber).thenComparing(SchoolClass::getSectionLetter))
                .toList();
        if (classes.isEmpty()) throw new IllegalStateException("Maktabda sinf yo'q");
        String schoolName = schoolRepository.findById(schoolId).map(s -> s.getName()).orElse("");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4.rotate(), 28, 28, 26, 22);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Dars jadvali");
        doc.open();
        boolean first = true;
        for (SchoolClass c : classes) {
            if (!first) doc.newPage();
            first = false;
            List<TimetableEntryDto> entries = lessonSlotService.getTimetable(schoolId, c.getId(), null, null);
            page(doc, schoolName, c, entries);
        }
        doc.close();
        return out.toByteArray();
    }

    private void page(Document doc, String schoolName, SchoolClass c, List<TimetableEntryDto> entries) {
        String className = c.getGradeNumber() + "-" + c.getSectionLetter();
        String year = c.getAcademicYear() != null ? c.getAcademicYear().getTitle() : "";

        Paragraph title = new Paragraph();
        title.add(new Chunk(className + " sinf — dars jadvali", new Font(bold, 17, Font.NORMAL, new Color(0x1e293b))));
        doc.add(title);
        Paragraph sub = new Paragraph(schoolName + (year.isBlank() ? "" : " · " + year + " o'quv yili"),
                new Font(regular, 10, Font.NORMAL, new Color(0x64748b)));
        sub.setSpacingAfter(8);
        doc.add(sub);

        // periods = distinct start times of this class, in order: "1-dars 08:30–09:15"
        TreeMap<LocalTime, LocalTime> periods = new TreeMap<>();
        for (TimetableEntryDto e : entries) periods.merge(e.getStartTime(), e.getEndTime(), (a, b) -> a.isAfter(b) ? a : b);
        if (periods.isEmpty()) {
            doc.add(new Paragraph("Bu sinf uchun dars jadvali kiritilmagan.", new Font(regular, 12)));
            return;
        }
        Map<String, TimetableEntryDto> byCell = new HashMap<>();
        for (TimetableEntryDto e : entries) byCell.putIfAbsent(e.getWeekday() + "|" + e.getStartTime(), e);

        // the rows share whatever height the page has left, so 5 or 9 lessons both fit on one page
        float usable = doc.getPageSize().getHeight() - doc.topMargin() - doc.bottomMargin() - 64;
        float headerH = 24;
        float rowH = Math.min(95, (usable - headerH) / periods.size());
        float subjectSize = Math.max(7.5f, Math.min(11, rowH / 4.2f));
        float smallSize = Math.max(6.5f, subjectSize - 2.5f);

        PdfPTable table = new PdfPTable(1 + WEEKDAYS.size());
        table.setWidthPercentage(100);
        float[] widths = new float[1 + WEEKDAYS.size()];
        widths[0] = 0.62f;
        Arrays.fill(widths, 1, widths.length, 1f);
        try {
            table.setWidths(widths);
        } catch (DocumentException e) {
            throw new IllegalStateException(e);
        }

        table.addCell(headerCell("", headerH));
        for (String day : WEEKDAYS) table.addCell(headerCell(day, headerH));

        int n = 1;
        for (Map.Entry<LocalTime, LocalTime> p : periods.entrySet()) {
            PdfPCell time = new PdfPCell();
            time.setFixedHeight(rowH);
            time.setVerticalAlignment(Element.ALIGN_MIDDLE);
            time.setHorizontalAlignment(Element.ALIGN_CENTER);
            time.setBorderColor(new Color(0xe2e8f0));
            Paragraph tp = new Paragraph(n + "-dars", new Font(bold, subjectSize, Font.NORMAL, new Color(0x334155)));
            tp.setAlignment(Element.ALIGN_CENTER);
            time.addElement(tp);
            Paragraph tt = new Paragraph(hhmm(p.getKey()) + "–" + hhmm(p.getValue()),
                    new Font(regular, smallSize, Font.NORMAL, new Color(0x64748b)));
            tt.setAlignment(Element.ALIGN_CENTER);
            time.addElement(tt);
            table.addCell(time);
            for (String day : WEEKDAYS) {
                table.addCell(lessonCell(byCell.get(day + "|" + p.getKey()), rowH, subjectSize, smallSize));
            }
            n++;
        }
        doc.add(table);
    }

    private PdfPCell headerCell(String text, float h) {
        PdfPCell cell = new PdfPCell(new Phrase(text, new Font(bold, 10.5f, Font.NORMAL, Color.WHITE)));
        cell.setFixedHeight(h);
        cell.setBackgroundColor(new Color(0x4f46e5));
        cell.setBorderColor(new Color(0x4f46e5));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    private PdfPCell lessonCell(TimetableEntryDto e, float h, float subjectSize, float smallSize) {
        PdfPCell cell = new PdfPCell();
        cell.setFixedHeight(h);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(new Color(0xe2e8f0));
        cell.setPaddingLeft(6);
        if (e == null) return cell;
        Color color = subjectColor(e.getSubjectName());
        // a light tint of the subject colour, with a solid stripe on the left
        cell.setBackgroundColor(tint(color, 0.13f));
        cell.setBorderWidthLeft(3.5f);
        cell.setBorderColorLeft(color);
        Paragraph subject = new Paragraph(e.getSubjectName(), new Font(bold, subjectSize, Font.NORMAL, new Color(0x0f172a)));
        subject.setLeading(subjectSize * 1.15f);
        cell.addElement(subject);
        String room = e.getRoomNumber() == null ? "" : e.getRoomNumber();
        // "201-xona", but a named room ("Katta sport zali") as it is
        String roomLabel = room.isEmpty() ? "" : " · " + (Character.isDigit(room.charAt(0)) ? room + "-xona" : room);
        String meta = (e.getTeacherLastName() == null ? "" : e.getTeacherLastName()) + roomLabel;
        Paragraph small = new Paragraph(meta, new Font(regular, smallSize, Font.NORMAL, new Color(0x475569)));
        small.setLeading(smallSize * 1.2f);
        cell.addElement(small);
        return cell;
    }

    private static Color tint(Color c, float alpha) {
        return new Color(
                Math.round(255 - (255 - c.getRed()) * alpha),
                Math.round(255 - (255 - c.getGreen()) * alpha),
                Math.round(255 - (255 - c.getBlue()) * alpha));
    }

    private static String hhmm(LocalTime t) {
        return t == null ? "" : t.toString().substring(0, 5);
    }
}
