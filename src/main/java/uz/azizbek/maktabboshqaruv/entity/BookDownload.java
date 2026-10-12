package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.DownloadSource;

import java.time.LocalDateTime;

/**
 * One PDF a parent asked for in the bot. The child and the class are deliberately not
 * copied here: reports reach them through the parent's links (chat → children → class),
 * so a child moving to another class needs no fixing of history.
 */
@Entity
@Table(name = "book_download", indexes = {
        @Index(name = "idx_book_download_school_time", columnList = "school_id, downloaded_at"),
        @Index(name = "idx_book_download_book", columnList = "book_id"),
        @Index(name = "idx_book_download_chat_time", columnList = "chat_id, downloaded_at")
})
public class BookDownload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_session_id")
    private ParentSession parent;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "downloaded_at", nullable = false)
    private LocalDateTime downloadedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DownloadSource source;

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

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public ParentSession getParent() {
        return parent;
    }

    public void setParent(ParentSession parent) {
        this.parent = parent;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public LocalDateTime getDownloadedAt() {
        return downloadedAt;
    }

    public void setDownloadedAt(LocalDateTime downloadedAt) {
        this.downloadedAt = downloadedAt;
    }

    public DownloadSource getSource() {
        return source;
    }

    public void setSource(DownloadSource source) {
        this.source = source;
    }
}
