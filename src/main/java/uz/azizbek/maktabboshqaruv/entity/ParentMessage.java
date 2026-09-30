package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A message a parent wrote to the school from the bot ("💬 Maktabga yozish"), and the school's reply. */
@Entity
@Table(name = "parent_message", indexes = {
        @Index(name = "idx_parent_message_school_status", columnList = "school_id, status"),
        @Index(name = "idx_parent_message_chat", columnList = "chat_id")
})
public class ParentMessage {

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

    private String parentName;

    private String parentUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ParentMessageRecipient recipient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    private String photoFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ParentMessageStatus status;

    @Column(columnDefinition = "TEXT")
    private String replyText;

    private String repliedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime repliedAt;

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

    public ParentMessageRecipient getRecipient() {
        return recipient;
    }

    public void setRecipient(ParentMessageRecipient recipient) {
        this.recipient = recipient;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getPhotoFileId() {
        return photoFileId;
    }

    public void setPhotoFileId(String photoFileId) {
        this.photoFileId = photoFileId;
    }

    public ParentMessageStatus getStatus() {
        return status;
    }

    public void setStatus(ParentMessageStatus status) {
        this.status = status;
    }

    public String getReplyText() {
        return replyText;
    }

    public void setReplyText(String replyText) {
        this.replyText = replyText;
    }

    public String getRepliedBy() {
        return repliedBy;
    }

    public void setRepliedBy(String repliedBy) {
        this.repliedBy = repliedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getRepliedAt() {
        return repliedAt;
    }

    public void setRepliedAt(LocalDateTime repliedAt) {
        this.repliedAt = repliedAt;
    }
}
