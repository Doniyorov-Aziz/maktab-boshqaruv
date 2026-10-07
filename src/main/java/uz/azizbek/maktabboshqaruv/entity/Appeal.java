package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A parent's appeal to the school ("✉️ Ma'muriyatga xat"): a small chat between one parent and the school. */
@Entity
@Table(name = "appeal", indexes = {
        @Index(name = "idx_appeal_school_status_last", columnList = "school_id, status, last_message_at"),
        @Index(name = "idx_appeal_chat", columnList = "chat_id")
})
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    /** The parent's Telegram chat (null for an appeal registered by staff). */
    @Column(name = "chat_id")
    private Long chatId;

    private String parentName;

    private String parentUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppealEnums.Target target;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppealEnums.Status status;

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private AppealEnums.Source source;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_message_at", nullable = false)
    private LocalDateTime lastMessageAt;

    /** Parent messages the school has not opened yet (the badge). */
    private int unreadCount;

    /** "Assalomu alaykum, …" / "🖼 Rasm" — the list's one-line preview. */
    @Column(length = 200)
    private String lastPreview;

    /** Staff member who registered a manual appeal. */
    private String createdBy;

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

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getParentUsername() {
        return parentUsername;
    }

    public void setParentUsername(String parentUsername) {
        this.parentUsername = parentUsername;
    }

    public AppealEnums.Target getTarget() {
        return target;
    }

    public void setTarget(AppealEnums.Target target) {
        this.target = target;
    }

    public AppealEnums.Status getStatus() {
        return status;
    }

    public void setStatus(AppealEnums.Status status) {
        this.status = status;
    }

    public AppealEnums.Source getSource() {
        return source == null ? AppealEnums.Source.BOT : source;
    }

    public void setSource(AppealEnums.Source source) {
        this.source = source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }

    public String getLastPreview() {
        return lastPreview;
    }

    public void setLastPreview(String lastPreview) {
        this.lastPreview = lastPreview;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
