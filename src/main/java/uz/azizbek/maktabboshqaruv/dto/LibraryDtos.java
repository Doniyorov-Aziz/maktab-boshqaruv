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

    /** A book on the site (no storage paths, no Telegram ids). */
    public record BookDto(Long id, String title, String author, Long categoryId, String categoryName, String categoryEmoji,
                          String description, String language, AgeGroup ageGroup, Integer pages, Integer year,
                          boolean hasCover, Long pdfSize, String pdfSha256, LegalBasis legalBasis, String legalNote,
                          boolean active, boolean featured, boolean draft, long downloadCount, int likes, int dislikes,
                          String createdBy, LocalDateTime createdAt) {
    }

    /** Create / edit form. The legal basis is required: without it a book is never saved. */
    public record BookRequest(
            @NotBlank(message = "Kitob nomi kiritilishi shart") @Size(max = 255) String title,
            @Size(max = 255) String author,
            Long categoryId,
            @Size(max = 2000, message = "Annotatsiya 2000 belgidan oshmasin") String description,
            @Pattern(regexp = "^(uz|ru|en)$", message = "Til: uz, ru yoki en") String language,
            AgeGroup ageGroup,
            @Min(1) @Max(20000) Integer pages,
            @Min(1000) @Max(2100) Integer year,
            @NotNull(message = "Huquqiy asos tanlanishi shart") LegalBasis legalBasis,
            @Size(max = 500) String legalNote,
            Boolean active,
            Boolean featured) {
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
