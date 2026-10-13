package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A rights holder's written permission (ruxsatnoma) to hand one or more books out — free, as PDF,
 * only to this school's pupils and their parents (the scope text). A book whose legal basis is
 * MUALLIF_RUXSATI or LITSENZIYA is saved only with a permission that is in force; when it expires
 * or is revoked, its books are made inactive.
 */
@Entity
@Table(name = "library_permission", indexes = @Index(name = "idx_library_permission_school", columnList = "school_id, valid_until"))
public class LibraryPermission {

    public enum Grantor { MUALLIF, MEROSXOR, NASHRIYOT }

    public static final String DEFAULT_SCOPE = "Faqat shu maktab o'quvchilari va ularning ota-onalariga maktab elektron "
            + "kutubxonasi va Telegram boti orqali bepul elektron (PDF) tarqatish";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Grantor grantor;

    @Column(nullable = false, length = 200)
    private String grantorFullName;

    @Column(length = 40)
    private String phone;

    @Column(nullable = false, length = 1000)
    private String scope;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /** null = without an end date */
    @Column(name = "valid_until")
    private LocalDate validUntil;

    /** the signed scan (PDF/JPG/PNG) in our storage — required */
    @Column(nullable = false, length = 255)
    private String documentPath;

    @Column(nullable = false, length = 60)
    private String documentType;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false)
    private Boolean revoked;

    private LocalDateTime revokedAt;

    /** when the "30 kunda tugaydi" warning was written, so it is written once */
    private LocalDateTime expiryWarnedAt;

    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    /** In force on {@code day}: not revoked, started, and not past its end date. */
    public boolean inForce(LocalDate day) {
        return !Boolean.TRUE.equals(revoked) && !validFrom.isAfter(day) && (validUntil == null || !validUntil.isBefore(day));
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

    public Grantor getGrantor() {
        return grantor;
    }

    public void setGrantor(Grantor grantor) {
        this.grantor = grantor;
    }

    public String getGrantorFullName() {
        return grantorFullName;
    }

    public void setGrantorFullName(String grantorFullName) {
        this.grantorFullName = grantorFullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Boolean getRevoked() {
        return revoked;
    }

    public void setRevoked(Boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public LocalDateTime getExpiryWarnedAt() {
        return expiryWarnedAt;
    }

    public void setExpiryWarnedAt(LocalDateTime expiryWarnedAt) {
        this.expiryWarnedAt = expiryWarnedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
