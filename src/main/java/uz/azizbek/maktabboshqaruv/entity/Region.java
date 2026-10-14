package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Hudud (viloyat, Qoraqalpog'iston Respublikasi, Toshkent shahri) — the reference list shared by all
 * schools. {@code code} is the SOATO code: stable across databases, unlike the id. A region is never
 * deleted (students and districts point at it): "o'chirish" sets {@code state} to 'P'.
 */
@Entity
// the constraint carries the name PostgreSQL gave the table's UNIQUE(code), so Hibernate does not add a second one
@Table(name = "region", uniqueConstraints = @UniqueConstraint(name = "region_code_key", columnNames = "code"))
@BatchSize(size = 20) // districts' regions load in one query
public class Region {

    public static final String ACTIVE = "A";
    public static final String DELETED = "P";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 10)
    private String code;

    @Column(name = "name_uz", nullable = false, length = 100)
    private String nameUz;

    /** A = faol, P = o'chirilgan. CHAR(1) as in the table, so ddl-auto=update sees nothing to change. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 1)
    private String state = ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNameUz() {
        return nameUz;
    }

    public void setNameUz(String nameUz) {
        this.nameUz = nameUz;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
