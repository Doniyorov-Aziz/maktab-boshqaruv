package uz.azizbek.maktabboshqaruv.service.library.wikisource;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.service.library.LibraryPdfFonts;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * A readable A5 book from a Wikisource text — made once, while the work is imported, never per
 * request: a cover page (title, author, years of life, "Jamoat mulki · Manba: Wikisource", the
 * school), the table of contents with page numbers, every chapter from a new page, page numbers at
 * the foot and PDF bookmarks. Noto Serif is embedded, so Uzbek Latin and Cyrillic letters are exact.
 *
 * The table of contents needs the chapters' page numbers, so the book is laid out twice: the first
 * pass only measures where each chapter starts.
 */
@Component
public class WikisourcePdf {

    private static final Rectangle PAGE = PageSize.A5;
    private static final Color INK = new Color(0x1f2937);
    private static final Color MUTED = new Color(0x6b7280);
    private static final Color COVER = new Color(0x1e3a5f);
    private static final Color GOLD = new Color(0xd4af37);

    public record Chapter(String heading, String text) {
    }

    public record BookInfo(String title, String author, String lifeYears, String sourceUrl, String school) {
    }

    private final LibraryPdfFonts fonts;

    public WikisourcePdf(LibraryPdfFonts fonts) {
        this.fonts = fonts;
    }

    public byte[] render(BookInfo info, List<Chapter> chapters) {
        List<Integer> starts = new ArrayList<>();
        layout(info, chapters, null, starts);                 // pass 1: where do chapters start?
        return layout(info, chapters, List.copyOf(starts), new ArrayList<>()); // pass 2: with the numbers
    }

    private byte[] layout(BookInfo info, List<Chapter> chapters, List<Integer> tocPages, List<Integer> starts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PAGE, 46, 46, 50, 58);
        PdfWriter writer = PdfWriter.getInstance(doc, out);
        writer.setSpaceCharRatio(PdfWriter.NO_SPACE_CHAR_RATIO); // justify by widening spaces, never letters
        writer.setPageEvent(new Footer());
        doc.addTitle(info.title());
        doc.addAuthor(info.author());
        doc.addSubject("Jamoat mulki · Manba: Wikisource");
        doc.open();

        cover(writer, info);
        writer.setPageEmpty(false); // the cover is drawn directly; without this newPage() would skip it
        doc.newPage();

        // table of contents
        Paragraph tocTitle = new Paragraph("Mundarija", new Font(fonts.bold(), 15, Font.NORMAL, INK));
        tocTitle.setSpacingAfter(14);
        doc.add(tocTitle);
        Font tocFont = new Font(fonts.regular(), 10.5f, Font.NORMAL, INK);
        for (int i = 0; i < chapters.size(); i++) {
            Paragraph line = new Paragraph();
            line.setFont(tocFont);
            line.setLeading(0, 1.35f);
            line.add(new Chunk(chapters.get(i).heading(), tocFont));
            line.add(new Chunk(new com.lowagie.text.pdf.draw.DottedLineSeparator()));
            String number = tocPages == null ? "000" : String.valueOf(tocPages.get(i));
            line.add(new Chunk(" " + number, tocFont));
            doc.add(line);
        }

        Font headingFont = new Font(fonts.bold(), 14.5f, Font.NORMAL, INK);
        Font sectionFont = new Font(fonts.bold(), 11.5f, Font.NORMAL, INK);
        Font textFont = new Font(fonts.regular(), 10.8f, Font.NORMAL, INK);
        PdfOutline root = writer.getRootOutline();
        for (Chapter c : chapters) {
            doc.newPage();
            starts.add(writer.getPageNumber());
            new PdfOutline(root, new PdfDestination(PdfDestination.FITH, writer.getVerticalPosition(true)), c.heading());
            Paragraph h = new Paragraph(c.heading(), headingFont);
            h.setAlignment(Element.ALIGN_CENTER);
            h.setSpacingAfter(16);
            doc.add(h);
            for (String block : c.text().split("\n\\s*\n")) {
                String b = block.strip();
                if (b.isEmpty()) continue;
                if (b.startsWith(WikiText.HEADING)) {
                    Paragraph s = new Paragraph(b.substring(WikiText.HEADING.length()), sectionFont);
                    s.setSpacingBefore(8);
                    s.setSpacingAfter(6);
                    s.setAlignment(Element.ALIGN_CENTER);
                    doc.add(s);
                    continue;
                }
                if (isVerse(b)) { // poems keep their lines
                    Paragraph p = new Paragraph(b, textFont);
                    p.setLeading(0, 1.45f);
                    p.setSpacingAfter(9);
                    p.setAlignment(Element.ALIGN_LEFT);
                    p.setIndentationLeft(18);
                    doc.add(p);
                    continue;
                }
                // prose: every line is a paragraph (pages without blank lines between paragraphs, too)
                for (String line : b.split("\n")) {
                    if (line.isBlank()) continue;
                    Paragraph p = new Paragraph(line.strip(), textFont);
                    p.setLeading(0, 1.45f);
                    p.setSpacingAfter(4);
                    // ragged right: without hyphenation, justified A5 lines of long Uzbek words get wide gaps
                    p.setAlignment(Element.ALIGN_LEFT);
                    p.setFirstLineIndent(16);
                    doc.add(p);
                }
            }
        }
        doc.close();
        return out.toByteArray();
    }

    /** A block of several mostly short lines is a poem (lines kept); long lines are prose paragraphs. */
    static boolean isVerse(String block) {
        String[] lines = block.split("\n");
        if (lines.length < 2) return false;
        long shortLines = java.util.Arrays.stream(lines).filter(l -> l.strip().length() <= 70).count();
        return shortLines * 10 >= lines.length * 8;
    }

    /** The cover: a dark page with the title, the author, the years, the source and the school. */
    private void cover(PdfWriter writer, BookInfo info) {
        PdfContentByte cb = writer.getDirectContentUnder();
        float w = PAGE.getWidth();
        float h = PAGE.getHeight();
        cb.saveState();
        cb.setColorFill(COVER);
        cb.rectangle(0, 0, w, h);
        cb.fill();
        cb.setColorStroke(GOLD);
        cb.setLineWidth(1.2f);
        cb.rectangle(22, 22, w - 44, h - 44);
        cb.stroke();
        cb.restoreState();

        PdfContentByte top = writer.getDirectContent();
        column(top, info.author(), new Font(fonts.regular(), 13, Font.NORMAL, GOLD), 46, h - 150, w - 46, h - 100);
        column(top, info.title(), new Font(fonts.bold(), 24, Font.NORMAL, Color.WHITE), 46, h - 330, w - 46, h - 160);
        if (info.lifeYears() != null && !info.lifeYears().isBlank()) {
            column(top, info.lifeYears(), new Font(fonts.italic(), 11, Font.NORMAL, new Color(0xcbd5e1)),
                    46, h - 360, w - 46, h - 335);
        }
        column(top, "Jamoat mulki · Manba: Wikisource", new Font(fonts.regular(), 9.5f, Font.NORMAL, new Color(0xcbd5e1)),
                46, 70, w - 46, 92);
        if (info.school() != null) {
            column(top, info.school() + " elektron kutubxonasi", new Font(fonts.bold(), 10, Font.NORMAL, GOLD),
                    46, 46, w - 46, 68);
        }
    }

    private static void column(PdfContentByte cb, String text, Font font, float llx, float lly, float urx, float ury) {
        ColumnText ct = new ColumnText(cb);
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        p.setLeading(0, 1.25f);
        ct.setSimpleColumn(llx, lly, urx, ury);
        ct.addElement(p);
        ct.go();
    }

    /** Page numbers at the foot of every page but the cover. */
    private class Footer extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            int n = writer.getPageNumber();
            if (n == 1) return;
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(String.valueOf(n), new Font(fonts.regular(), 9, Font.NORMAL, MUTED)),
                    PAGE.getWidth() / 2, 30, 0);
        }
    }
}
