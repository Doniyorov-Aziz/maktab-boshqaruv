package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.BroadcastAttachment;
import uz.azizbek.maktabboshqaruv.repository.BroadcastAttachmentRepository;
import uz.azizbek.maktabboshqaruv.service.appeal.FileStorage;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Outbox media "broadcast:&lt;id&gt;": the broadcast's files in order. The first delivery
 * uploads each file; the file_id Telegram returns is stored and used for every other parent.
 */
@Component
public class BroadcastMedia implements OutboxMedia.Resolver {

    static final String PREFIX = "broadcast:";

    private final BroadcastAttachmentRepository attachments;
    private final FileStorage storage;

    public BroadcastMedia(BroadcastAttachmentRepository attachments, FileStorage storage) {
        this.attachments = attachments;
        this.storage = storage;
    }

    public static String ref(Long broadcastId) {
        return PREFIX + broadcastId;
    }

    @Override
    public boolean supports(String media) {
        return media.startsWith(PREFIX);
    }

    @Override
    public List<OutboxMedia.File> files(String media) {
        Long id = Long.valueOf(media.substring(PREFIX.length()));
        List<OutboxMedia.File> files = new ArrayList<>();
        for (BroadcastAttachment a : attachments.findByBroadcastIdOrderByPosition(id)) {
            String path = a.getStoragePath();
            Long attachmentId = a.getId();
            files.add(new OutboxMedia.File(OutboxMedia.kindOf(a.getKind()), a.getFileId(), () -> storage.read(path),
                    a.getOriginalName() != null ? a.getOriginalName() : "fayl",
                    fileId -> {
                        if (a.getFileId() == null) attachments.rememberFileId(attachmentId, fileId);
                    }));
        }
        return files;
    }
}
