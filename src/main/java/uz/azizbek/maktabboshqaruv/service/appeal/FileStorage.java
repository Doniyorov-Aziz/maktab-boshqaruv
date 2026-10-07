package uz.azizbek.maktabboshqaruv.service.appeal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Our own copy of appeal files on disk: {app.storage.path}/telegram/{schoolId}/{yyyy-MM}/{uuid}.ext.
 * Names are random (never the uploader's name), and reading is confined to the
 * storage root, so a stored path can never point outside it.
 */
@Component
public class FileStorage {

    private final Path root;

    public FileStorage(@Value("${app.storage.path:./storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    /** Writes the bytes and returns the path relative to the storage root. */
    public String save(Long schoolId, String ext, byte[] bytes) {
        String relative = "telegram/" + schoolId + "/" + YearMonth.now() + "/" + UUID.randomUUID()
                + (ext == null || ext.isBlank() ? "" : "." + ext);
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, bytes, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Faylni saqlab bo'lmadi", e);
        }
        return relative;
    }

    public byte[] read(String relative) {
        try {
            return Files.readAllBytes(resolve(relative));
        } catch (IOException e) {
            throw new UncheckedIOException("Fayl topilmadi", e);
        }
    }

    /** The absolute path of a stored file; refuses anything that would leave the storage root. */
    public Path resolve(String relative) {
        Path p = root.resolve(relative).normalize();
        if (!p.startsWith(root)) throw new IllegalStateException("Noto'g'ri fayl yo'li");
        return p;
    }

    public Path root() {
        return root;
    }
}
