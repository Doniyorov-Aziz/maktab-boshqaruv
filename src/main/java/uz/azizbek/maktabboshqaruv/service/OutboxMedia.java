package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.AppealEnums;
import uz.azizbek.maktabboshqaruv.entity.AppealMessage;
import uz.azizbek.maktabboshqaruv.entity.NotificationLog;
import uz.azizbek.maktabboshqaruv.repository.AppealMessageRepository;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.MediaItem;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.MediaKind;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.SentMedia;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

/**
 * Sends the files of an outbox row ({@link NotificationLog#getMedia()}): one file with the
 * text as its caption when it fits, otherwise albums (sendMediaGroup, up to 10 per album,
 * photos/videos together, documents together, audio together) followed by the text with
 * its buttons. Every file Telegram stores gets a file_id, which is remembered — the next
 * recipient of the same file is sent the id, the file is never uploaded twice.
 */
@Service
public class OutboxMedia {

    /** Telegram's caption limit (visible characters). */
    private static final int MAX_CAPTION = 1024;

    /** One file of a message: what it is, how to get it, and where to remember Telegram's file_id. */
    public record File(MediaKind kind, String fileId, java.util.function.Supplier<byte[]> bytes, String name,
                       Consumer<String> rememberFileId) {
    }

    /** Resolves a media reference other than "appeal:…" (e.g. "broadcast:7"). */
    public interface Resolver {
        boolean supports(String media);

        List<File> files(String media);
    }

    private final TelegramClient telegram;
    private final AppealMessageRepository appealMessages;
    private final FileStorage storage;
    private final List<Resolver> resolvers;

    public OutboxMedia(TelegramClient telegram, AppealMessageRepository appealMessages, FileStorage storage,
                       List<Resolver> resolvers) {
        this.telegram = telegram;
        this.appealMessages = appealMessages;
        this.storage = storage;
        this.resolvers = resolvers;
    }

    public void send(NotificationLog n) {
        List<File> files = resolve(n.getMedia());
        if (files.isEmpty()) {
            telegram.sendMessage(n.getChatId(), n.getText(), n.getReplyMarkup());
            return;
        }
        long chatId = n.getChatId();
        if (files.size() == 1 && visibleLength(n.getText()) <= MAX_CAPTION) {
            File f = files.get(0);
            SentMedia sent = telegram.sendMedia(chatId, item(f, n.getText()), n.getReplyMarkup());
            remember(f, sent);
            return;
        }
        // albums by compatible kinds (Telegram does not mix documents or audio with photos)
        Map<String, List<File>> groups = new LinkedHashMap<>();
        for (File f : files) groups.computeIfAbsent(groupOf(f.kind()), k -> new ArrayList<>()).add(f);
        for (List<File> group : groups.values()) {
            for (int from = 0; from < group.size(); from += 10) {
                List<File> chunk = group.subList(from, Math.min(group.size(), from + 10));
                if (chunk.size() == 1 || chunk.get(0).kind() == MediaKind.VOICE) {
                    for (File f : chunk) remember(f, telegram.sendMedia(chatId, item(f, null), null));
                } else {
                    List<SentMedia> sent = telegram.sendMediaGroup(chatId, chunk.stream().map(f -> item(f, null)).toList());
                    for (int i = 0; i < chunk.size() && i < sent.size(); i++) remember(chunk.get(i), sent.get(i));
                }
            }
        }
        telegram.sendMessage(chatId, n.getText(), n.getReplyMarkup());
    }

    List<File> resolve(String media) {
        if (media == null || media.isBlank()) return List.of();
        if (media.startsWith("appeal:")) {
            List<Long> ids = Arrays.stream(media.substring("appeal:".length()).split(","))
                    .filter(s -> !s.isBlank()).map(Long::valueOf).toList();
            Map<Long, AppealMessage> byId = new HashMap<>();
            for (AppealMessage m : appealMessages.findAllById(ids)) byId.put(m.getId(), m);
            List<File> files = new ArrayList<>();
            for (Long id : ids) {
                AppealMessage m = byId.get(id);
                if (m == null || m.getStoragePath() == null) continue;
                String path = m.getStoragePath();
                files.add(new File(kindOf(m.getKind()), m.getFileId(), () -> storage.read(path),
                        m.getOriginalName() != null ? m.getOriginalName() : "fayl",
                        fileId -> {
                            if (!fileId.equals(m.getFileId())) {
                                m.setFileId(fileId);
                                appealMessages.save(m);
                            }
                        }));
            }
            return files;
        }
        for (Resolver r : resolvers) if (r.supports(media)) return r.files(media);
        return List.of();
    }

    private static MediaItem item(File f, String caption) {
        return f.fileId() != null
                ? new MediaItem(f.kind(), f.fileId(), null, f.name(), caption)
                : new MediaItem(f.kind(), null, f.bytes().get(), f.name(), caption);
    }

    private static void remember(File f, SentMedia sent) {
        if (sent != null && sent.fileId() != null && f.rememberFileId() != null) f.rememberFileId().accept(sent.fileId());
    }

    private static String groupOf(MediaKind k) {
        return switch (k) {
            case PHOTO, VIDEO -> "visual";
            case DOCUMENT -> "document";
            case AUDIO -> "audio";
            case VOICE -> "voice";
        };
    }

    public static MediaKind kindOf(AppealEnums.Kind kind) {
        return switch (kind) {
            case PHOTO -> MediaKind.PHOTO;
            case VIDEO, VIDEO_NOTE -> MediaKind.VIDEO;
            case AUDIO -> MediaKind.AUDIO;
            case VOICE -> MediaKind.VOICE;
            case DOCUMENT, TEXT -> MediaKind.DOCUMENT;
        };
    }

    static int visibleLength(String html) {
        if (html == null) return 0;
        return html.replaceAll("<[^>]+>", "").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&quot;", "\"").replace("&amp;", "&").length();
    }
}
