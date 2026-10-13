package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;

import java.time.LocalDateTime;
import java.util.List;

/** Requests and responses of the library pages (Kutubxona). */
public final class LibraryDtos {

    private LibraryDtos() {
    }

    /** An author of a book with its public-domain state (FREE since / PROTECTED until / UNKNOWN). */
    public record BookAuthorDto(Long id, String fullName, Integer birthYear, Integer deathYear, String status,
                                Integer statusYear) {
    }

    /** A book on the site (no storage paths, no Telegram ids). */
    public record BookDto(Long id, String title, String author, List<BookAuthorDto> authors, Long categoryId,
                          String categoryName, String categoryEmoji, String description, String language,
                          AgeGroup ageGroup, Integer pages, Integer year, boolean hasCover, Long pdfSize,
                          String pdfSha256, LegalBasis legalBasis, String legalNote, Long permissionId,
                          String permissionGrantor, Boolean publishedInLifetime, Integer firstPublishedYear,
                          Boolean noModernCommentary, String sourceUrl, boolean active, boolean featured, boolean draft,
                          long downloadCount, int likes, int dislikes, String createdBy, LocalDateTime createdAt) {
    }

    /**
     * Create / edit form. The legal basis is required: without it a book is never saved. Authors come
     * from the reference list ({@code authorIds}); a typed {@code author} name is matched to it (or added).
     * JAMOAT_MULKI needs the two confirmations, MUALLIF_RUXSATI / LITSENZIYA a permission in force.
     */
    public record BookRequest(
            @NotBlank(message = "Kitob nomi kiritilishi shart") @Size(max = 255) String title,
            @Size(max = 255) String author,
            List<Long> authorIds,
            Long categoryId,
            @Size(max = 2000, message = "Annotatsiya 2000 belgidan oshmasin") String description,
            @Pattern(regexp = "^(uz|ru|en)$", message = "Til: uz, ru yoki en") String language,
            AgeGroup ageGroup,
            @Min(1) @Max(20000) Integer pages,
            @Min(1000) @Max(2100) Integer year,
            @NotNull(message = "Huquqiy asos tanlanishi shart") LegalBasis legalBasis,
            @Size(max = 500) String legalNote,
            Long permissionId,
            Boolean publishedInLifetime,
            @Min(1000) @Max(2100) Integer firstPublishedYear,
            Boolean noModernCommentary,
            Boolean active,
            Boolean featured) {
    }

    public record AuthorDto(Long id, String fullName, String aliases, Integer birthYear, Integer deathYear, String note,
                            String status, Integer statusYear, long bookCount) {
    }

    public record AuthorRequest(
            @NotBlank(message = "Muallif ismi kiritilishi shart") @Size(max = 200) String fullName,
            @Size(max = 2000) String aliases,
            @Min(500) @Max(2100) Integer birthYear,
            @Min(500) @Max(2100) Integer deathYear,
            @Size(max = 1000) String note) {
    }

    /** status: ACTIVE, ENDING (≤ 30 days), EXPIRED, REVOKED, NOT_STARTED */
    public record PermissionDto(Long id, String grantor, String grantorFullName, String phone, String scope,
                                java.time.LocalDate validFrom, java.time.LocalDate validUntil, String note,
                                String status, Integer daysLeft, String documentType, List<PermissionBookDto> books,
                                String createdBy, LocalDateTime createdAt) {
    }

    public record PermissionBookDto(Long id, String title, boolean active) {
    }

    public record PermissionRequest(
            @NotNull(message = "Kim ruxsat bergani tanlanishi shart") String grantor,
            @NotBlank(message = "F.I.Sh. kiritilishi shart") @Size(max = 200) String grantorFullName,
            @Size(max = 40) String phone,
            @NotNull(message = "Boshlanish sanasi kiritilishi shart") java.time.LocalDate validFrom,
            java.time.LocalDate validUntil,
            @Size(max = 1000) String note,
            List<Long> bookIds) {
    }

    /** A work listed on an author's Wikisource page. */
    public record WikisourceWorkDto(String title, String label, Integer year, String section, String url,
                                    boolean imported) {
    }

    public record WikisourceAuthorDto(Long authorId, String authorName, String page, String pageUrl, Integer birthYear,
                                      Integer deathYear, List<WikisourceWorkDto> works, String message) {
    }

    public record WikisourceImportRequest(
            @NotNull Long authorId,
            @NotBlank String title,
            Long categoryId,
            Boolean publishedInLifetime,
            Integer firstPublishedYear,
            Boolean noModernCommentary) {
    }

    public record PageDto<T>(List<T> items, long total, int page, int size) {
    }

    /** Another book of the school with the same PDF (same SHA-256). */
    public record DuplicateDto(Long id, String title, String author, boolean active) {
    }

    public record CategoryDto(Long id, String nameUz, String nameRu, String emoji, int position, boolean active,
                              long bookCount) {
    }

    public record CategoryRequest(
            @NotBlank(message = "Bo'lim nomi kiritilishi shart") @Size(max = 80) String nameUz,
            @Size(max = 80) String nameRu,
            @Size(max = 16) String emoji,
            Boolean active) {
    }

    public record ReorderRequest(@NotNull List<Long> ids) {
    }
}
