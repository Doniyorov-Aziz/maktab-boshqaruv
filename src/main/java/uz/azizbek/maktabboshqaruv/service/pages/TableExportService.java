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
import org.springframework.stereotype.Service;
import uz.azizbek.maktabboshqaruv.service.TimetablePdfService;
import uz.azizbek.maktabboshqaruv.util.PhoneFormat;
import uz.azizbek.maktabboshqaruv.util.XlsxWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * "Eksport ▾ Excel / PDF" of any list page: the page sends the table as it shows it (the visible
 * columns, the rows of the current search and filters, already as text) and gets the file back.
 * A phone column is written as "+998 90 111 22 33", a number column as a number in Excel.
 */
@Service
public class TableExportService {

    public static final int MAX_ROWS = 10_000;

    public record Column(String label, String type) {
    }

    public record Table(String title, String subtitle, List<Column> columns, List<List<Object>> rows) {
    }

    public byte[] xlsx(Table t) {
        List<String> header = t.columns().stream().map(Column::label).toList();
        List<Integer> widths = new ArrayList<>();
        for (int c = 0; c < t.columns().size(); c++) {
            int w = header.get(c).length();
            for (List<Object> row : t.rows()) {
                Object v = c < row.size() ? row.get(c) : null;
                if (v != null) w = Math.max(w, String.valueOf(v).length());
            }
            widths.add(Math.min(Math.max(w + 2, 8), 60));
        }
        return XlsxWriter.write(t.title(), header, widths, cells(t, true));
    }

    public byte[] pdf(Table t) {
        BaseFont regular = TimetablePdfService.font("/fonts/Inter-Regular.ttf");
        BaseFont bold = TimetablePdfService.font("/fonts/Inter-Bold.ttf");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(t.columns().size() > 5 ? PageSize.A4.rotate() : PageSize.A4, 22, 22, 24, 20);
        try {
            PdfWriter.getInstance(doc, out);
            doc.open();
            doc.add(new Paragraph(t.title(), new Font(bold, 14, Font.NORMAL, new Color(0x1e293b))));
            if (t.subtitle() != null && !t.subtitle().isBlank()) {
                doc.add(new Paragraph(t.subtitle(), new Font(regular, 8.5f, Font.NORMAL, new Color(0x64748b))));
            }
            doc.add(new Paragraph(" ", new Font(regular, 4)));
            PdfPTable table = new PdfPTable(t.columns().size());
            table.setWidthPercentage(100);
            table.setHeaderRows(1);
            Font head = new Font(bold, 7.5f, Font.NORMAL, Color.WHITE);
            Font cell = new Font(regular, 7.5f, Font.NORMAL, new Color(0x0f172a));
            for (Column c : t.columns()) table.addCell(box(c.label(), head, new Color(0x4f46e5)));
            int r = 0;
            for (List<Object> row : cells(t, false)) {
                Color zebra = r++ % 2 == 1 ? new Color(0xf8fafc) : null;
                for (Object v : row) table.addCell(box(v == null ? "" : String.valueOf(v), cell, zebra));
            }
            doc.add(table);
        } finally {
            doc.close();
        }
        return out.toByteArray();
    }

    /** The rows as cells: phones formatted, numbers kept as numbers (in Excel), the rest as text. */
    private static List<List<Object>> cells(Table t, boolean numbers) {
        List<List<Object>> out = new ArrayList<>();
        for (List<Object> row : t.rows()) {
            List<Object> cells = new ArrayList<>();
            for (int c = 0; c < t.columns().size(); c++) {
                Object v = c < row.size() ? row.get(c) : null;
                String type = t.columns().get(c).type();
                if (v != null && "phone".equals(type)) v = PhoneFormat.format(String.valueOf(v));
                else if (v instanceof Number n && !numbers) v = n.doubleValue() == Math.rint(n.doubleValue())
                        ? String.valueOf(n.longValue()) : String.valueOf(n);
                cells.add(v);
            }
            out.add(cells);
        }
        return out;
    }

    private static PdfPCell box(String text, Font font, Color bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setHorizontalAlignment(Element.ALIGN_LEFT);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        c.setPadding(3f);
        c.setBorderColor(new Color(0xe2e8f0));
        if (bg != null) c.setBackgroundColor(bg);
        return c;
    }
}
