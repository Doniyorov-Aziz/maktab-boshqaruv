package uz.azizbek.maktabboshqaruv.service;

import org.springframework.stereotype.Component;
import uz.azizbek.maktabboshqaruv.repository.HomeworkRepository;
import uz.azizbek.maktabboshqaruv.telegram.TelegramClient.MediaKind;

import java.util.List;

/**
 * Outbox media "homework:&lt;id&gt;": the homework's file as a document (a picture as a photo). Uploaded
 * once; the file_id Telegram returns is used for every other parent.
 */
@Component
public class HomeworkMedia implements OutboxMedia.Resolver {

    static final String PREFIX = "homework:";

    private final HomeworkRepository homework;
    private final ModuleFiles files;

    public HomeworkMedia(HomeworkRepository homework, ModuleFiles files) {
        this.homework = homework;
        this.files = files;
    }

    public static String ref(Long homeworkId) {
        return PREFIX + homeworkId;
    }

    @Override
    public boolean supports(String media) {
        return media.startsWith(PREFIX);
    }

    @Override
    public List<OutboxMedia.File> files(String media) {
        Long id = Long.valueOf(media.substring(PREFIX.length()));
        return homework.findById(id).filter(h -> h.getFilePath() != null).map(h -> {
            String path = h.getFilePath();
            String knownId = h.getFileTelegramId();
            return List.of(new OutboxMedia.File(ModuleFiles.isImage(h.getFileName()) ? MediaKind.PHOTO : MediaKind.DOCUMENT,
                    knownId, () -> files.read(path), h.getFileName(),
                    fileId -> {
                        if (knownId == null) homework.rememberFileId(id, fileId);
                    }));
        }).orElse(List.of());
    }
}
