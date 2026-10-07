package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** "Ota-onalarga xabar": one admin-written message to all or selected classes' parents. Delivery rows live in the outbox. */
@Entity
@Table(name = "broadcast")
public class Broadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private BroadcastAudience audience;

    /** Comma-separated class ids when audience = CLASSES. */
    @Column(columnDefinition = "TEXT")
    private String classIds;

    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private Integer recipientCount;

    /** Comma-separated chat ids when audience = PARENTS. */
    @Column(columnDefinition = "TEXT")
    private String chatIds;

    /** Attached files (photos, videos, documents); 0 for a text-only message. */
    @Column(columnDefinition = "integer default 0")
    private Integer mediaCount = 0;

    public String getChatIds() {
        return chatIds;
    }

    public void setChatIds(String chatIds) {
        this.chatIds = chatIds;
    }

    public Integer getMediaCount() {
        return mediaCount;
    }

    public void setMediaCount(Integer mediaCount) {
        this.mediaCount = mediaCount;
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

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BroadcastAudience getAudience() {
        return audience;
    }

    public void setAudience(BroadcastAudience audience) {
        this.audience = audience;
    }

    public String getClassIds() {
        return classIds;
    }

    public void setClassIds(String classIds) {
        this.classIds = classIds;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(Integer recipientCount) {
        this.recipientCount = recipientCount;
    }
}
