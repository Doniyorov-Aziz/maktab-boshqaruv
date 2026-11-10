package uz.azizbek.maktabboshqaruv.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;
import uz.azizbek.maktabboshqaruv.service.ReportCardService.StudentYear;
import uz.azizbek.maktabboshqaruv.service.ReportCardService.YearAttendance;
import uz.azizbek.maktabboshqaruv.service.ReportCardService.YearSubject;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/** The printed tabel: one A4 page per pupil — the quarter grades of every subject, the year grade, attendance. */
@Service
public class ReportCardPdf {

    private static final Color INK = new Color(0x1e293b);
    private static final Color MUTED = new Color(0x64748b);
    private static final Color LINE = new Color(0xcbd5e1);
    private static final Color HEAD = new Color(0xeef2ff);

    private final BaseFont regular = TimetablePdfService.font("/fonts/Inter-Regular.ttf");
    private final BaseFont bold = TimetablePdfService.font("/fonts/Inter-Bold.ttf");

    public byte[] render(List<StudentYear> pupils) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 40, 40, 36, 30);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Tabel");
        doc.open();
        boolean first = true;
        for (StudentYear p : pupils) {
            if (!first) doc.newPage();
            first = false;
            page(doc, p);
        }
        if (pupils.isEmpty()) doc.add(new Paragraph("Sinfda o'quvchi yo'q.", new Font(regular, 12)));
        doc.close();
        return out.toByteArray();
    }

    private void page(Document doc, StudentYear p) {
        Paragraph school = new Paragraph(p.schoolName(), new Font(regular, 10, Font.NORMAL, MUTED));
        school.setAlignment(Element.ALIGN_CENTER);
        doc.add(school);
        Paragraph title = new Paragraph("O'QUVCHI TABELI", new Font(bold, 18, Font.NORMAL, INK));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingBefore(4);
        doc.add(title);
        Paragraph year = new Paragraph((p.yearTitle() == null ? "" : p.yearTitle() + " o'quv yili"),
                new Font(regular, 10, Font.NORMAL, MUTED));
        year.setAlignment(Element.ALIGN_CENTER);
        year.setSpacingAfter(14);
        doc.add(year);

        PdfPTable who = new PdfPTable(new float[]{1.2f, 3f});
        who.setWidthPercentage(100);
        who.addCell(label("O'quvchi:"));
        who.addCell(value(p.name()));
        who.addCell(label("Sinf:"));
        who.addCell(value(p.className()));
        who.setSpacingAfter(12);
        doc.add(who);

        int n = p.terms().size();
        if (n == 0) {
            doc.add(new Paragraph("Bu o'quv yili uchun choraklar kiritilmagan.", new Font(regular, 11)));
            return;
        }
        float[] widths = new float[n + 2];
        widths[0] = 3.2f;
        for (int i = 1; i <= n + 1; i++) widths[i] = 1f;
        PdfPTable t = new PdfPTable(widths);
        t.setWidthPercentage(100);
        t.setHeaderRows(1);
        t.addCell(head("Fan", Element.ALIGN_LEFT));
        for (ReportCardService.Term term : p.terms()) t.addCell(head(roman(term.number()) + " chorak", Element.ALIGN_CENTER));
        t.addCell(head("Yillik", Element.ALIGN_CENTER));
        int i = 0;
        for (YearSubject s : p.subjects()) {
            Color bg = i++ % 2 == 1 ? new Color(0xf8fafc) : Color.WHITE;
            t.addCell(cell(s.subject(), Element.ALIGN_LEFT, bg, false));
            for (Integer q : s.quarters()) t.addCell(cell(q == null ? "—" : q.toString(), Element.ALIGN_CENTER, bg, false));
            t.addCell(cell(s.year() == null ? "—" : s.year().toString(), Element.ALIGN_CENTER, bg, true));
        }
        t.setSpacingAfter(14);
        doc.add(t);

        Paragraph attTitle = new Paragraph("Davomat (dars soatlari)", new Font(bold, 11, Font.NORMAL, INK));
        attTitle.setSpacingAfter(4);
        doc.add(attTitle);
        PdfPTable a = new PdfPTable(widths);
        a.setWidthPercentage(100);
        a.addCell(head("", Element.ALIGN_LEFT));
        for (ReportCardService.Term term : p.terms()) a.addCell(head(roman(term.number()) + " chorak", Element.ALIGN_CENTER));
        a.addCell(head("Jami", Element.ALIGN_CENTER));
        String[] labels = {"Sababsiz qoldirgan", "Sababli qoldirgan", "Kechikkan"};
        for (int r = 0; r < 3; r++) {
            a.addCell(cell(labels[r], Element.ALIGN_LEFT, Color.WHITE, false));
            int total = 0;
            for (YearAttendance y : p.attendance()) {
                if (y == null) {
                    a.addCell(cell("—", Element.ALIGN_CENTER, Color.WHITE, false));
                    continue;
                }
                int v = r == 0 ? y.absent() : r == 1 ? y.excused() : y.late();
                total += v;
                a.addCell(cell(String.valueOf(v), Element.ALIGN_CENTER, Color.WHITE, false));
            }
            a.addCell(cell(String.valueOf(total), Element.ALIGN_CENTER, Color.WHITE, true));
        }
        a.setSpacingAfter(28);
        doc.add(a);

        PdfPTable sign = new PdfPTable(new float[]{1f, 1f});
        sign.setWidthPercentage(100);
        sign.addCell(label("Sinf rahbari: ____________________"));
        sign.addCell(label("Ota-ona: ____________________"));
        sign.addCell(label(" "));
        sign.addCell(label(" "));
        sign.addCell(label("Direktor: ____________________"));
        sign.addCell(label("M.O'."));
        doc.add(sign);
    }

    private PdfPCell head(String text, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(bold, 9.5f, Font.NORMAL, INK)));
        c.setBackgroundColor(HEAD);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(6);
        return c;
    }

    private PdfPCell cell(String text, int align, Color bg, boolean strong) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(strong ? bold : regular, 10, Font.NORMAL, INK)));
        c.setBackgroundColor(bg);
        c.setBorderColor(LINE);
        c.setHorizontalAlignment(align);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(5);
        return c;
    }

    private PdfPCell label(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(regular, 10.5f, Font.NORMAL, MUTED)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
    }

    private PdfPCell value(String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, new Font(bold, 11, Font.NORMAL, INK)));
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(3);
        return c;
    }

    static String roman(int n) {
        return switch (n) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            default -> String.valueOf(n);
        };
    }
}
