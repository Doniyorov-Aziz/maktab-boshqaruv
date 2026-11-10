package uz.azizbek.maktabboshqaruv.service;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * One attached file of a homework or an achievement: the allowed kinds (documents, pictures,
 * archives), a 20 MB limit, the copy in {@link FileStorage} and the download response.
 */
@Component
public class ModuleFiles {

    public static final long MAX_BYTES = 20L * 1024 * 1024;

    static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("txt", "text/plain"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"),
            Map.entry("zip", "application/zip"));

    /** A stored upload: where it is, its original name and size. */
    public record Stored(String path, String name, long size) {
    }

    private final FileStorage storage;

    public ModuleFiles(FileStorage storage) {
        this.storage = storage;
    }

    public Stored save(String area, Long schoolId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fayl tanlanmagan");
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Fayl 20 MB dan katta bo'lmasin");
        }
        String name = cleanName(file.getOriginalFilename());
        String ext = extensionOf(name);
        if (!TYPES.containsKey(ext)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bu turdagi fayl qabul qilinmaydi (PDF, Word, Excel, PowerPoint, rasm, TXT yoki ZIP bo'lsin)");
        }
        try {
            return new Stored(storage.save(area, schoolId, ext, file.getBytes()), name, file.getSize());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Faylni o'qib bo'lmadi");
        }
    }

    public byte[] read(String path) {
        return storage.read(path);
    }

    public void delete(String path) {
        storage.delete(path);
    }

    public ResponseEntity<byte[]> download(String path, String name) {
        if (path == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fayl biriktirilmagan");
        byte[] bytes;
        try {
            bytes = storage.read(path);
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fayl topilmadi");
        }
        String ext = extensionOf(name);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(TYPES.getOrDefault(ext, "application/octet-stream")))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(name == null ? "fayl" : name, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.maxAge(10, TimeUnit.MINUTES).cachePrivate())
                .body(bytes);
    }

    public static boolean isImage(String name) {
        String ext = extensionOf(name);
        return ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("webp");
    }

    static String extensionOf(String name) {
        if (name == null) return "";
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** Only the file name, without folders or control characters, at most 150 characters. */
    static String cleanName(String original) {
        String n = original == null ? "fayl" : original.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}\"]", "").strip();
        if (n.isEmpty()) n = "fayl";
        if (n.length() > 150) {
            String ext = extensionOf(n);
            n = n.substring(0, 150 - ext.length() - 1) + "." + ext;
        }
        return n;
    }
}
