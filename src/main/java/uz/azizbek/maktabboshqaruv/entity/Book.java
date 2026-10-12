package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;

import java.time.LocalDateTime;

/**
 * A book of the school library. The PDF (and the cover) live in our storage and are
 * uploaded only by the school's staff — the bot never fetches books from the internet.
 * Every book carries the legal basis on which the school hands it out.
 *
 * {@code searchKey} is the title and author normalised for search (lower case, Cyrillic
 * written in Latin letters, apostrophes dropped — see LibraryText), so "O‘tkan kunlar",
 * "o'tkan kunlar" and "Ўткан кунлар" find the same book. {@code downloadCount},
 * {@code likes} and {@code dislikes} are counters kept next to the rows they count,
 * so lists never count per book.
 */
@Entity
@Table(name = "book", indexes = {
        @Index(name = "idx_book_school_active_category", columnList = "school_id, active, category_id"),
        @Index(name = "idx_book_school_sha", columnList = "school_id, pdf_sha256")
})
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 255)
    private String author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private BookCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** uz / ru / en */
    @Column(nullable = false, length = 2)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AgeGroup ageGroup;

    private Integer pages;

    @Column(name = "publish_year")
    private Integer year;

    /** storage path of the cover (uploaded, or drawn from the PDF's first page once at upload) */
    @Column(length = 255)
    private String coverPath;

    @Column(nullable = false, length = 255)
    private String pdfPath;

    @Column(nullable = false)
    private Long pdfSize;

    @Column(name = "pdf_sha256", nullable = false, length = 64)
    private String pdfSha256;

    /** Telegram's file_id of the PDF after the first sending; later parents get the file by it */
    @Column(length = 255)
    private String telegramFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private LegalBasis legalBasis;

    /** e.g. the number of the permission letter */
    @Column(length = 500)
    private String legalNote;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private Boolean featured;

    /** made by a bulk upload and not reviewed yet — never shown in the bot */
    @Column(nullable = false)
    private Boolean draft;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String searchKey;

    @Column(nullable = false)
    private Long downloadCount;

    @Column(nullable = false)
    private Integer likes;

    @Column(nullable = false)
    private Integer dislikes;

    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    /** What the bot may show: active, reviewed, and with a file. */
    public boolean visibleToParents() {
        return Boolean.TRUE.equals(active) && !Boolean.TRUE.equals(draft);
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public BookCategory getCategory() {
        return category;
    }

    public void setCategory(BookCategory category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public AgeGroup getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(AgeGroup ageGroup) {
        this.ageGroup = ageGroup;
    }

    public Integer getPages() {
        return pages;
    }

    public void setPages(Integer pages) {
        this.pages = pages;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public Long getPdfSize() {
        return pdfSize;
    }

    public void setPdfSize(Long pdfSize) {
        this.pdfSize = pdfSize;
    }

    public String getPdfSha256() {
        return pdfSha256;
    }

    public void setPdfSha256(String pdfSha256) {
        this.pdfSha256 = pdfSha256;
    }

    public String getTelegramFileId() {
        return telegramFileId;
    }

    public void setTelegramFileId(String telegramFileId) {
        this.telegramFileId = telegramFileId;
    }

    public LegalBasis getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(LegalBasis legalBasis) {
        this.legalBasis = legalBasis;
    }

    public String getLegalNote() {
        return legalNote;
    }

    public void setLegalNote(String legalNote) {
        this.legalNote = legalNote;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getFeatured() {
        return featured;
    }

    public void setFeatured(Boolean featured) {
        this.featured = featured;
    }

    public Boolean getDraft() {
        return draft;
    }

    public void setDraft(Boolean draft) {
        this.draft = draft;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public Long getDownloadCount() {
        return downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getLikes() {
        return likes;
    }

    public void setLikes(Integer likes) {
        this.likes = likes;
    }

    public Integer getDislikes() {
        return dislikes;
    }

    public void setDislikes(Integer dislikes) {
        this.dislikes = dislikes;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
