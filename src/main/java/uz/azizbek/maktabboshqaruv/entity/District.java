package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Tuman yoki shahar inside a {@link Region}. Like regions it is never deleted ('P' = o'chirilgan):
 * students point at it for their place of birth and residence. {@code code} is the SOATO code —
 * optional here, since the seeded list carries names only; fill it from the official classifier.
 */
@Entity
@Table(name = "district", uniqueConstraints = @UniqueConstraint(name = "district_code_key", columnNames = "code"))
@BatchSize(size = 100) // a page of students loads its districts in one query
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    private Region region;

    @Column(length = 10)
    private String code;

    @Column(name = "name_uz", nullable = false, length = 100)
    private String nameUz;

    /** A = faol, P = o'chirilgan */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 1)
    private String state = Region.ACTIVE;

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

    /** "Toshkent shahri, Chilonzor tumani" */
    public String placeName() {
        return region.getNameUz() + ", " + nameUz;
    }

    public Long getId() {
        return id;
    }

    public Region getRegion() {
        return region;
    }

    public void setRegion(Region region) {
        this.region = region;
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
