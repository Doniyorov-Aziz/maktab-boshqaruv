package uz.azizbek.maktabboshqaruv.service.appeal;

import java.time.LocalDateTime;
import java.util.List;

/** What the "Murojaatlar" page receives. Files are only ever addressed by our own signed link. */
public final class AppealViews {

    private AppealViews() {
    }

    public record Row(Long id, String parentName, String parentUsername, Long studentId, String studentName,
                      Long classId, String className, String target, String status, String source,
                      String lastPreview, LocalDateTime lastMessageAt, LocalDateTime createdAt, int unread) {
    }

    /** One bubble. {@code url}: a 30-minute signed link to the file (null for text or while downloading). */
    public record Message(Long id, String direction, String kind, String text, String mediaGroupId,
                          String fileName, String mimeType, Long fileSize, Integer duration, String fileState,
                          String url, String sentBy, LocalDateTime createdAt) {
    }

    public record Chat(Row appeal, String guardianName, String guardianPhone, boolean canReply,
                       List<Message> messages) {
    }
}
