package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** A book a parent saved in the bot ("❤️ Saqlanganlarim"). One row per parent and book. */
@Entity
@Table(name = "book_favorite",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_favorite", columnNames = {"book_id", "chat_id"}),
        indexes = @Index(name = "idx_book_favorite_chat", columnList = "chat_id, created_at"))
public class BookFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_session_id")
    private ParentSession parent;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
