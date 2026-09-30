package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDateTime;
import java.util.List;

/** One broadcast with its delivery result (from the outbox) — or, for a preview, just html + recipientCount. */
public class BroadcastDto {

    private Long id;
    private String text;
    private String html;
    private String audience;
    private List<String> classNames;
    private String createdBy;
    private LocalDateTime createdAt;
    private int recipientCount;
    private long sent;
    private long pending;
    private long failed;
    private long skipped;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public List<String> getClassNames() {
        return classNames;
    }

    public void setClassNames(List<String> classNames) {
        this.classNames = classNames;
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

    public int getRecipientCount() {
        return recipientCount;
    }

    public void setRecipientCount(int recipientCount) {
        this.recipientCount = recipientCount;
    }

    public long getSent() {
        return sent;
    }

    public void setSent(long sent) {
        this.sent = sent;
    }

    public long getPending() {
        return pending;
    }

    public void setPending(long pending) {
        this.pending = pending;
    }

    public long getFailed() {
        return failed;
    }

    public void setFailed(long failed) {
        this.failed = failed;
    }

    public long getSkipped() {
        return skipped;
    }

    public void setSkipped(long skipped) {
        this.skipped = skipped;
    }
}
