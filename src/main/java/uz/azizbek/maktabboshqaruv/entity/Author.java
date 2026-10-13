package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

/**
 * An author in the library's reference list (shared by all schools): the name, its other spellings
 * (Latin / Cyrillic, pen names) and the years of life. The death year decides whether the author's
 * works are in the public domain (CopyrightRules). Seeded with Uzbek classics; admins add and edit.
 */
@Entity
@Table(name = "author", uniqueConstraints = @UniqueConstraint(name = "uk_author_name", columnNames = "full_name"))
public class Author {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    /** other spellings and pen names, one per line */
    @Column(columnDefinition = "TEXT")
    private String aliases;

    private Integer birthYear;

    private Integer deathYear;

    @Column(length = 1000)
    private String note;

    /** normalised name and aliases (LibraryText), for matching typed names and Wikisource pages */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String searchKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getAliases() {
        return aliases;
    }

    public void setAliases(String aliases) {
        this.aliases = aliases;
    }

    public Integer getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(Integer birthYear) {
        this.birthYear = birthYear;
    }

    public Integer getDeathYear() {
        return deathYear;
    }

    public void setDeathYear(Integer deathYear) {
        this.deathYear = deathYear;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    /** "(1894–1938)" or "" */
    public String lifeYears() {
        if (birthYear == null && deathYear == null) return "";
        return "(" + (birthYear == null ? "?" : birthYear) + "–" + (deathYear == null ? "" : deathYear) + ")";
    }
}
