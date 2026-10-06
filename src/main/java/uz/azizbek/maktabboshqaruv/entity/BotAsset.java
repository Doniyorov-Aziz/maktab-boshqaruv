package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A file uploaded to Telegram once and reused by its file_id afterwards
 * (static section banners: "banner:schedule", …). Survives restarts, so a
 * banner is uploaded once per bot, not once per server start.
 */
@Entity
@Table(name = "bot_asset")
public class BotAsset {

    @Id
    @Column(name = "asset_key", length = 80)
    private String key;

    @Column(name = "file_id", nullable = false, length = 255)
    private String fileId;

    private LocalDateTime updatedAt;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
