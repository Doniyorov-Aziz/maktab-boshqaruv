package uz.azizbek.maktabboshqaruv.service.library;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * The files of a library book: checked, stored under {app.storage.path}/library/{school}/… with
 * random names (FileStorage), and read back only through the backend.
 *
 * A PDF must really be a PDF ("%PDF" at the start — a renamed .exe is refused whatever its name
 * or Content-Type says) and at most app.library.max-pdf-mb. A cover must be a JPEG or PNG by its
 * magic bytes, at most app.library.max-cover-mb. Without a cover, one is drawn from the PDF's
 * first page here, once, while the book is being uploaded — never later, never per request.
 */
@Service
public class LibraryFiles {

    private static final Logger log = LoggerFactory.getLogger(LibraryFiles.class);
    private static final String AREA = "library";
    /** width of a cover drawn from a PDF page, in pixels */
    private static final int COVER_WIDTH = 480;

    private final FileStorage storage;
    private final LibrarySettings settings;

    public LibraryFiles(FileStorage storage, LibrarySettings settings) {
        this.storage = storage;
        this.settings = settings;
    }

    /** What we learnt from a PDF while checking it. */
    public record PdfInfo(String sha256, long size, Integer pages) {
    }

    /** Refuses anything that is not a PDF within the size limit; returns its hash and page count. */
    public PdfInfo checkPdf(byte[] bytes) {
        if (bytes == null || bytes.length == 0) throw bad("PDF fayl tanlanmagan");
        if (bytes.length > settings.maxPdfBytes()) {
            throw bad("PDF juda katta: ko'pi bilan " + settings.maxPdfBytes() / 1024 / 1024 + " MB");
        }
        if (!isPdf(bytes)) throw bad("Fayl PDF emas (faqat haqiqiy PDF fayllar qabul qilinadi)");
        Integer pages = null;
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            pages = doc.getNumberOfPages();
        } catch (IOException e) {
            // a damaged or encrypted PDF can still be sent to parents as it is; only the page count is unknown
            log.info("PDF sahifalari sanab bo'lmadi: {}", e.getMessage());
        }
        return new PdfInfo(sha256(bytes), bytes.length, pages);
    }

    /** "jpg" or "png" by magic bytes; anything else (or too big) is refused. */
    public String checkCover(byte[] bytes) {
        if (bytes.length > settings.maxCoverBytes()) {
            throw bad("Muqova juda katta: ko'pi bilan " + settings.maxCoverBytes() / 1024 / 1024 + " MB");
        }
        if (bytes.length > 3 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (bytes.length > 8 && (bytes[0] & 0xFF) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
            return "png";
        }
        throw bad("Muqova faqat JPG yoki PNG bo'lishi mumkin");
    }

    public static boolean isPdf(byte[] bytes) {
        // "%PDF" — allowed after a few bytes of junk, as PDF readers do
        int limit = Math.min(bytes.length - 4, 1024);
        for (int i = 0; i <= limit; i++) {
            if (bytes[i] == '%' && bytes[i + 1] == 'P' && bytes[i + 2] == 'D' && bytes[i + 3] == 'F') return true;
        }
        return false;
    }

    public String savePdf(Long schoolId, byte[] bytes) {
        return storage.save(AREA, schoolId, "pdf", bytes);
    }

    public String saveCover(Long schoolId, String ext, byte[] bytes) {
        return storage.save(AREA, schoolId, ext, bytes);
    }

    /** The PDF's first page as a JPEG cover, or null when it cannot be drawn. Called once per upload. */
    public byte[] coverFromPdf(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.getNumberOfPages() == 0) return null;
            float pageWidthPt = doc.getPage(0).getMediaBox().getWidth();
            float scale = pageWidthPt > 0 ? COVER_WIDTH / pageWidthPt : 1f;
            BufferedImage image = new PDFRenderer(doc).renderImage(0, scale, ImageType.RGB);
            return jpeg(image);
        } catch (Exception e) {
            log.info("PDF dan muqova chizib bo'lmadi: {}", e.getMessage());
            return null;
        }
    }

    public byte[] read(String path) {
        return storage.read(path);
    }

    public void delete(String path) {
        storage.delete(path);
    }

    static byte[] jpeg(BufferedImage image) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (MemoryCacheImageOutputStream stream = new MemoryCacheImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.82f);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** "Muallif - Nomi.pdf" for Telegram, without characters a file name cannot hold. */
    public static String downloadName(String author, String title) {
        String base = (author == null || author.isBlank() ? "" : author.strip() + " - ") + title.strip();
        String safe = base.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", " ").replaceAll("\\s+", " ").strip();
        if (safe.length() > 120) safe = safe.substring(0, 120).strip();
        return (safe.isEmpty() ? "kitob" : safe) + ".pdf";
    }

    static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
