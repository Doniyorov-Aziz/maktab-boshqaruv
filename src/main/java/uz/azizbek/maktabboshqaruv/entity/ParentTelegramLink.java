package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * One Telegram chat (a parent) subscribed to one student. A student can have
 * several parents linked and one parent can follow several children — each
 * pair is its own row. Unlinking only flips {@code active}, so a parent who
 * comes back via the same code or phone reuses the row.
 */
@Entity
@Table(name = "parent_telegram_link",
        uniqueConstraints = @UniqueConstraint(columnNames = {"student_id", "chat_id"}),
        indexes = @Index(name = "idx_parent_link_chat", columnList = "chat_id"))
public class ParentTelegramLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    private String telegramUsername;

    private String firstName;

    @Column(nullable = false)
    private LocalDateTime linkedAt;

    @Column(nullable = false)
    private Boolean active;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public void setTelegramUsername(String telegramUsername) {
        this.telegramUsername = telegramUsername;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public LocalDateTime getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(LocalDateTime linkedAt) {
        this.linkedAt = linkedAt;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
