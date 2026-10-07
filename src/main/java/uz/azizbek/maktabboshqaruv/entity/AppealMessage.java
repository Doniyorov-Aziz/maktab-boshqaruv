package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One message inside an appeal: text, or a file (photo, video, voice, audio,
 * document, round video). Files are kept on our own disk ({@code storagePath},
 * relative to app.storage.path) — never as a Telegram file URL, which would
 * contain the bot token.
 */
@Entity
@Table(name = "appeal_message", indexes = {
        @Index(name = "idx_appeal_message_appeal_created", columnList = "appeal_id, created_at")
})
public class AppealMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appeal_id", nullable = false)
    private Appeal appeal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private AppealEnums.Direction direction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private AppealEnums.Kind kind;

    @Column(columnDefinition = "TEXT")
    private String text;

    /** Telegram's album id: messages of one album are shown as one gallery. */
    @Column(length = 64)
    private String mediaGroupId;

    /** Telegram file_id (to re-send) and file_unique_id (to recognise duplicates). Not secret, not a URL. */
    @Column(length = 255)
    private String fileId;

    @Column(length = 128)
    private String fileUniqueId;

    @Column(length = 100)
    private String mimeType;

    private Long fileSize;

    /** Seconds, for voice/audio/video. */
    private Integer duration;

    @Column(length = 255)
    private String originalName;

    /** Path under app.storage.path, e.g. "telegram/1/2026-10/3f2a….ogg". */
    @Column(length = 255)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private AppealEnums.FileState fileState;

    /** Who answered (username), for OUT messages. */
    private String sentBy;

    /** The parent is still composing (before "✅ Yuborish"): not shown to the school yet. */
    private Boolean pending;

    public boolean isPending() {
        return Boolean.TRUE.equals(pending);
    }

    public void setPending(boolean pending) {
        this.pending = pending;
    }

    private Long telegramMessageId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean hasFile() {
        return kind != AppealEnums.Kind.TEXT;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Appeal getAppeal() {
        return appeal;
    }

    public void setAppeal(Appeal appeal) {
        this.appeal = appeal;
    }

    public AppealEnums.Direction getDirection() {
        return direction;
    }

    public void setDirection(AppealEnums.Direction direction) {
        this.direction = direction;
    }

    public AppealEnums.Kind getKind() {
        return kind;
    }

    public void setKind(AppealEnums.Kind kind) {
        this.kind = kind;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getMediaGroupId() {
        return mediaGroupId;
    }

    public void setMediaGroupId(String mediaGroupId) {
        this.mediaGroupId = mediaGroupId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getFileUniqueId() {
        return fileUniqueId;
    }

    public void setFileUniqueId(String fileUniqueId) {
        this.fileUniqueId = fileUniqueId;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public AppealEnums.FileState getFileState() {
        return fileState == null ? AppealEnums.FileState.NONE : fileState;
    }

    public void setFileState(AppealEnums.FileState fileState) {
        this.fileState = fileState;
    }

    public String getSentBy() {
        return sentBy;
    }

    public void setSentBy(String sentBy) {
        this.sentBy = sentBy;
    }

    public Long getTelegramMessageId() {
        return telegramMessageId;
    }

    public void setTelegramMessageId(Long telegramMessageId) {
        this.telegramMessageId = telegramMessageId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
