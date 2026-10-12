package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 👍 (+1) or 👎 (-1) of one parent for one book; tapping again changes it, never adds a second vote. */
@Entity
@Table(name = "book_rating",
        uniqueConstraints = @UniqueConstraint(name = "uk_book_rating", columnNames = {"book_id", "chat_id"}))
public class BookRating {

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

    @Column(nullable = false)
    private Integer value;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

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

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
