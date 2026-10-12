package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

/** A shelf of the school library ("🕵️ Detektiv", "🧒 Bolalar uchun", …), ordered by {@code position}. */
@Entity
@Table(name = "book_category", indexes = @Index(name = "idx_book_category_school", columnList = "school_id, active, position"))
public class BookCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 80)
    private String nameUz;

    @Column(length = 80)
    private String nameRu;

    @Column(length = 16)
    private String emoji;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false)
    private Boolean active;

    /** "🕵️ Detektiv" in the parent's language (Russian falls back to Uzbek). */
    public String label(String lang) {
        String name = "ru".equals(lang) && nameRu != null && !nameRu.isBlank() ? nameRu : nameUz;
        return emoji == null || emoji.isBlank() ? name : emoji + " " + name;
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

    public String getNameUz() {
        return nameUz;
    }

    public void setNameUz(String nameUz) {
        this.nameUz = nameUz;
    }

    public String getNameRu() {
        return nameRu;
    }

    public void setNameRu(String nameRu) {
        this.nameRu = nameRu;
    }

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(String emoji) {
        this.emoji = emoji;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
