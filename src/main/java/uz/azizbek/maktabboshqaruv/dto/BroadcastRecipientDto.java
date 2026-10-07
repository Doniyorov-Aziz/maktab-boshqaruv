package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDateTime;

/** One recipient of a broadcast (one outbox row): who, which class, delivered / queued / failed, when. */
public record BroadcastRecipientDto(Long id, Long studentId, String studentName, String className, String parentName,
                                    String status, LocalDateTime queuedAt, LocalDateTime sentAt, String error) {

    public BroadcastRecipientDto(Long id, Long studentId, String studentName, Integer grade, String letter,
                                 String parentName, String status, LocalDateTime queuedAt, LocalDateTime sentAt,
                                 String error) {
        this(id, studentId, studentName, grade + "-" + letter, parentName, status, queuedAt, sentAt, error);
    }
}
