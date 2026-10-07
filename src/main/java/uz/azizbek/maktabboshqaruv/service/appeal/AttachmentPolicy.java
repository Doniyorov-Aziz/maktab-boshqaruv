package uz.azizbek.maktabboshqaruv.service.appeal;

import uz.azizbek.maktabboshqaruv.entity.AppealEnums.Kind;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Which files the school accepts: pictures, videos, audio (including Telegram's
 * ogg voice), PDF and Office documents — at most 20 MB (Telegram's getFile limit
 * for bots). Anything executable or scriptable (exe, bat, js, html, svg, …) is
 * refused even when renamed: both the extension and the MIME type must be allowed.
 */
public final class AttachmentPolicy {

    public static final long MAX_BYTES = 20L * 1024 * 1024;

    /** extension → MIME type served back to the browser */
    private static final Map<String, String> ALLOWED = Map.ofEntries(
            Map.entry("jpg", "image/jpeg"), Map.entry("jpeg", "image/jpeg"), Map.entry("png", "image/png"),
            Map.entry("webp", "image/webp"), Map.entry("heic", "image/heic"), Map.entry("gif", "image/gif"),
            Map.entry("mp4", "video/mp4"), Map.entry("mov", "video/quicktime"), Map.entry("webm", "video/webm"),
            Map.entry("ogg", "audio/ogg"), Map.entry("oga", "audio/ogg"), Map.entry("opus", "audio/ogg"),
            Map.entry("mp3", "audio/mpeg"), Map.entry("m4a", "audio/mp4"), Map.entry("wav", "audio/wav"),
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));

    private static final Set<String> DANGEROUS_MIME_PARTS = Set.of("html", "javascript", "ecmascript", "svg",
            "x-msdownload", "x-msdos", "x-sh", "x-bat", "java-archive", "x-executable", "vnd.microsoft.portable-executable");

    /** Telegram's own types for files without a name (photos, voice, round videos). */
    private static final Map<Kind, String> DEFAULT_EXT = Map.of(
            Kind.PHOTO, "jpg", Kind.VOICE, "ogg", Kind.VIDEO, "mp4", Kind.VIDEO_NOTE, "mp4", Kind.AUDIO, "mp3");

    private AttachmentPolicy() {
    }

    /** The reason a file is refused (an i18n key), or null when it is fine. */
    public static String reject(Kind kind, String fileName, String mimeType, Long size) {
        if (size != null && size > MAX_BYTES) return "appeal.file_too_big";
        String ext = extensionOf(kind, fileName, mimeType);
        if (ext == null || !ALLOWED.containsKey(ext)) return "appeal.file_type";
        if (mimeType != null) {
            String m = mimeType.toLowerCase(Locale.ROOT);
            for (String bad : DANGEROUS_MIME_PARTS) if (m.contains(bad)) return "appeal.file_type";
        }
        return null;
    }

    /** "jpg", "pdf", … (lower case, no dot), or null when nothing allowed can be told. */
    public static String extensionOf(Kind kind, String fileName, String mimeType) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT).trim();
        }
        if (mimeType != null) {
            String m = mimeType.toLowerCase(Locale.ROOT);
            for (Map.Entry<String, String> e : ALLOWED.entrySet()) if (e.getValue().equals(m)) return e.getKey();
        }
        return DEFAULT_EXT.get(kind);
    }

    /** The Content-Type a stored file is served with (from our whitelist, not from the uploader). */
    public static String contentType(String ext) {
        return ALLOWED.getOrDefault(ext == null ? "" : ext.toLowerCase(Locale.ROOT), "application/octet-stream");
    }
}
