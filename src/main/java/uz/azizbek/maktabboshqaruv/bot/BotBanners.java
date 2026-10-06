package uz.azizbek.maktabboshqaruv.bot;

import uz.azizbek.maktabboshqaruv.entity.BotAsset;
import uz.azizbek.maktabboshqaruv.repository.BotAssetRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Section banners are static pictures (resources/bot/banners/{section}.png): no
 * personal data, nothing drawn per request. Each one is uploaded to Telegram the
 * first time it is needed; the file_id Telegram returns is kept (memory + DB) and
 * every later page sends only that id — no server CPU, no re-upload.
 */
@Service
public class BotBanners {

    public static final Set<String> SECTIONS = Set.of("home", "schedule", "attendance", "grades", "report",
            "announcements", "events", "teachers", "write", "absence", "settings", "children", "behavior",
            "school", "welcome");

    /** A section's banner: what to send, and the key its file_id is stored under. */
    public record Banner(String section) {
        public String key() {
            return "banner:" + section;
        }

        public String fileName() {
            return section + ".png";
        }
    }

    private final BotAssetRepository repository;
    private final Clock clock;
    private final Map<String, String> fileIds = new ConcurrentHashMap<>();
    private final Map<String, byte[]> bytes = new ConcurrentHashMap<>();

    public BotBanners(BotAssetRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    /** The banner of a section, or null for a section without one. */
    public Banner of(String section) {
        return section != null && SECTIONS.contains(section) ? new Banner(section) : null;
    }

    /** Telegram's file_id for the banner, or null if it was never uploaded. */
    public String fileId(Banner banner) {
        return fileIds.computeIfAbsent(banner.key(),
                k -> repository.findById(k).map(BotAsset::getFileId).orElse(null));
    }

    /** The PNG itself — read from the jar once, only for the very first upload. */
    public byte[] png(Banner banner) {
        return bytes.computeIfAbsent(banner.section(), s -> {
            String path = "/bot/banners/" + s + ".png";
            try (InputStream in = BotBanners.class.getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("Banner topilmadi: " + path);
                return in.readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public void remember(Banner banner, String fileId) {
        if (fileId == null || fileId.equals(fileIds.get(banner.key()))) return;
        fileIds.put(banner.key(), fileId);
        BotAsset asset = repository.findById(banner.key()).orElseGet(BotAsset::new);
        asset.setKey(banner.key());
        asset.setFileId(fileId);
        asset.setUpdatedAt(LocalDateTime.now(clock));
        repository.save(asset);
    }

    /** Telegram no longer knows the id (e.g. the bot token changed): upload again next time. */
    public void forget(Banner banner) {
        fileIds.remove(banner.key());
        repository.deleteById(banner.key());
    }
}
