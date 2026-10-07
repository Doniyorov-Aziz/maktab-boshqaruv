package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

/**
 * A file of a broadcast, kept in our storage. After the first parent receives it,
 * Telegram's file_id is remembered here and every other parent gets the file by that
 * id — the file is uploaded to Telegram only once, however many parents there are.
 */
@Entity
@Table(name = "broadcast_attachment", indexes = @Index(name = "idx_broadcast_attachment_broadcast", columnList = "broadcast_id"))
public class BroadcastAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "broadcast_id", nullable = false)
    private Broadcast broadcast;

    @Column(nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AppealEnums.Kind kind;

    @Column(length = 120)
    private String mimeType;

    @Column(length = 255)
    private String originalName;

    private Long fileSize;

    @Column(nullable = false, length = 255)
    private String storagePath;

    @Column(length = 255)
    private String fileId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Broadcast getBroadcast() {
        return broadcast;
    }

    public void setBroadcast(Broadcast broadcast) {
        this.broadcast = broadcast;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public AppealEnums.Kind getKind() {
        return kind;
    }

    public void setKind(AppealEnums.Kind kind) {
        this.kind = kind;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
