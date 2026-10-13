package uz.azizbek.maktabboshqaruv.service.library;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * "📄 Ruxsat xati shablonini yuklab olish": an A4 letter to print and sign, with the school's name
 * and the book title(s) already filled in. The rights holder fills in the name, the term, the phone,
 * the date and signs; the scan then goes to "Ruxsatnomalar".
 */
@Component
public class PermissionTemplatePdf {

    private static final Color INK = new Color(0x1e293b);
    private static final Color MUTED = new Color(0x64748b);

    private final LibraryPdfFonts fonts;

    public PermissionTemplatePdf(LibraryPdfFonts fonts) {
        this.fonts = fonts;
    }

    public byte[] render(String schoolName, List<String> bookTitles, String grantorName) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 72, 72, 72, 72);
        PdfWriter.getInstance(doc, out);
        doc.addTitle("Ruxsatnoma");
        doc.open();
        Font title = new Font(fonts.bold(), 18, Font.NORMAL, INK);
        Font body = new Font(fonts.regular(), 12.5f, Font.NORMAL, INK);
        Font strong = new Font(fonts.bold(), 12.5f, Font.NORMAL, INK);
        Font small = new Font(fonts.italic(), 9.5f, Font.NORMAL, MUTED);

        Paragraph head = new Paragraph("RUXSATNOMA", title);
        head.setAlignment(Element.ALIGN_CENTER);
        head.setSpacingAfter(28);
        doc.add(head);

        String works = bookTitles == null || bookTitles.isEmpty() ? "______________________________"
                : String.join(", ", bookTitles.stream().map(t -> "«" + t + "»").toList());
        String name = grantorName == null || grantorName.isBlank() ? "______________________________" : grantorName;
        String school = schoolName == null || schoolName.isBlank() ? "______________________" : schoolName;

        Paragraph p1 = new Paragraph();
        p1.setLeading(0, 1.6f);
        p1.setAlignment(Element.ALIGN_JUSTIFIED);
        p1.add(new Chunk("Men, ", body));
        p1.add(new Chunk(name, strong));
        p1.add(new Chunk(" (F.I.Sh.), ", body));
        p1.add(new Chunk(works, strong));
        p1.add(new Chunk(" asar(lar)ining muallifi (huquq egasi), ushbu asar(lar)ni ", body));
        p1.add(new Chunk(school, strong));
        p1.add(new Chunk(" o'quvchilari va ularning ota-onalariga maktabning elektron kutubxonasi va Telegram boti "
                + "orqali BEPUL elektron shaklda (PDF) tarqatishga ruxsat beraman.", body));
        p1.setSpacingAfter(14);
        doc.add(p1);

        doc.add(line("Ruxsat muddati: ____________________ dan ____________________ gacha "
                + "(bo'sh qoldirilsa — muddatsiz).", body));
        doc.add(line("Asarni tijorat maqsadida sotish, o'zgartirish va boshqa tashkilotlarga berish taqiqlanadi. "
                + "Muallif nomi har doim ko'rsatiladi.", body));
        doc.add(line("Ruxsatni yozma ariza bilan istalgan vaqtda bekor qilishim mumkin.", body));

        Paragraph sign = new Paragraph("Telefon: ____________________     Sana: ______________     Imzo: ______________", body);
        sign.setSpacingBefore(40);
        doc.add(sign);

        Paragraph foot = new Paragraph("Imzolangan nusxaning skani maktab kutubxonasining «Ruxsatnomalar» bo'limiga "
                + "yuklanadi. Asl nusxa maktabda saqlanadi.", small);
        foot.setSpacingBefore(60);
        doc.add(foot);
        doc.close();
        return out.toByteArray();
    }

    private static Paragraph line(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setLeading(0, 1.6f);
        p.setAlignment(Element.ALIGN_JUSTIFIED);
        p.setSpacingAfter(12);
        return p;
    }
}
