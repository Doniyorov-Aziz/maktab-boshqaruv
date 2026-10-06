package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Outbox row: one Telegram message to one chat. Rows are written after the
 * business transaction commits and picked up by the scheduled sender, so a
 * rolled-back save never produces a message and a Telegram outage never
 * loses one. The unique key is the dedup guard of last resort — the service
 * checks it first, the constraint catches concurrent duplicates.
 */
@Entity
@Table(name = "notification_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_notification_dedup",
                columnNames = {"student_id", "type", "reference_id", "record_date", "chat_id"}),
        indexes = {
                @Index(name = "idx_notification_status_scheduled", columnList = "status, scheduled_at"),
                @Index(name = "idx_notification_school_created", columnList = "school_id, created_at")
        })
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private NotificationType type;

    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private NotificationStatus status;

    @Column(nullable = false)
    private Integer attempts;

    @Column(columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** Earliest moment the sender may deliver it — pushed to the end of quiet hours or by a retry backoff. */
    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    /** Optional inline keyboard (JSON) sent with the message, e.g. "✍️ O'qituvchiga yozish". */
    @Column(columnDefinition = "TEXT")
    private String replyMarkup;

    /**
     * Picture to send instead of a plain message, rendered at delivery time —
     * e.g. "weekly:<studentId>:<from>:<to>:<lang>" for the weekly report card.
     * The text then becomes the photo's caption.
     */
    @Column(length = 120)
    private String image;

    /** Held back by quiet hours: delivered together with the other held messages in one morning note. */
    private Boolean quietBundle;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Boolean getQuietBundle() {
        return quietBundle;
    }

    public void setQuietBundle(Boolean quietBundle) {
        this.quietBundle = quietBundle;
    }

    public String getReplyMarkup() {
        return replyMarkup;
    }

    public void setReplyMarkup(String replyMarkup) {
        this.replyMarkup = replyMarkup;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public Long getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Long referenceId) {
        this.referenceId = referenceId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public NotificationStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationStatus status) {
        this.status = status;
    }

    public Integer getAttempts() {
        return attempts;
    }

    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}
