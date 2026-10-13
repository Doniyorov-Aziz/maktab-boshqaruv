package uz.azizbek.maktabboshqaruv.service.library;

import com.lowagie.text.pdf.BaseFont;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Noto Serif (SIL Open Font License, resources/fonts/NotoSerif-OFL.txt) embedded in the PDFs the
 * library makes — Uzbek Latin (oʻ, gʻ, ʼ) and Cyrillic (ў, қ, ғ, ҳ) and Russian come out right on
 * any reader. Loaded once.
 */
@Component
public class LibraryPdfFonts {

    private final BaseFont regular = load("NotoSerif-Regular");
    private final BaseFont bold = load("NotoSerif-Bold");
    private final BaseFont italic = load("NotoSerif-Italic");

    public BaseFont regular() {
        return regular;
    }

    public BaseFont bold() {
        return bold;
    }

    public BaseFont italic() {
        return italic;
    }

    private static BaseFont load(String name) {
        try (InputStream in = LibraryPdfFonts.class.getResourceAsStream("/fonts/" + name + ".ttf")) {
            if (in == null) throw new IllegalStateException("Shrift topilmadi: " + name);
            byte[] bytes = in.readAllBytes();
            return BaseFont.createFont(name + ".ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, bytes, null);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
