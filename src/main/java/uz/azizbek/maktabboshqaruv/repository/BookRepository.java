package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Book;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.AgeGroup;
import uz.azizbek.maktabboshqaruv.entity.LibraryEnums.LegalBasis;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Books. Every query the bot runs fetches the shelf with the book (no N+1) and returns
 * only what parents may see: active, reviewed (not a bulk-upload draft) and of the
 * parent's own school. A page is one select plus its count.
 */
public interface BookRepository extends JpaRepository<Book, Long> {

    String VISIBLE = "b.school.id = :schoolId and b.active = true and b.draft = false";

    @Query("select b from Book b left join fetch b.category where b.id = :id")
    Optional<Book> findWithCategory(@Param("id") Long id);

    /** Up to three normalised words, each must appear in title + author (empty word = '%'). */
    @Query(value = "select b from Book b left join fetch b.category where " + VISIBLE
            + " and b.searchKey like :w1 and b.searchKey like :w2 and b.searchKey like :w3"
            + " order by b.downloadCount desc, b.title",
            countQuery = "select count(b) from Book b where " + VISIBLE
                    + " and b.searchKey like :w1 and b.searchKey like :w2 and b.searchKey like :w3")
    Page<Book> search(@Param("schoolId") Long schoolId, @Param("w1") String w1, @Param("w2") String w2,
                      @Param("w3") String w3, Pageable pageable);

    @Query(value = "select b from Book b left join fetch b.category where " + VISIBLE
            + " and b.category.id = :categoryId order by b.title",
            countQuery = "select count(b) from Book b where " + VISIBLE + " and b.category.id = :categoryId")
    Page<Book> byCategory(@Param("schoolId") Long schoolId, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query(value = "select b from Book b left join fetch b.category where " + VISIBLE + " order by b.createdAt desc, b.id desc",
            countQuery = "select count(b) from Book b where " + VISIBLE)
    Page<Book> newest(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query(value = "select b from Book b left join fetch b.category where " + VISIBLE
            + " and b.downloadCount > 0 order by b.downloadCount desc, b.id desc",
            countQuery = "select count(b) from Book b where " + VISIBLE + " and b.downloadCount > 0")
    Page<Book> mostRead(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("select b from Book b left join fetch b.category where " + VISIBLE
            + " and b.featured = true order by b.downloadCount desc, b.id desc")
    List<Book> featured(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("select b from Book b left join fetch b.category where " + VISIBLE + " and b.id in :ids")
    List<Book> visibleByIds(@Param("schoolId") Long schoolId, @Param("ids") Collection<Long> ids);

    /** "Siz uchun": the most read books of the given shelves this parent has not downloaded yet. */
    @Query("select b from Book b left join fetch b.category where " + VISIBLE + " and b.category.id in :categoryIds"
            + " and not exists (select 1 from BookDownload d where d.book = b and d.chatId = :chatId)"
            + " order by b.downloadCount desc, b.id desc")
    List<Book> unreadInCategories(@Param("schoolId") Long schoolId, @Param("categoryIds") Collection<Long> categoryIds,
                                  @Param("chatId") Long chatId, Pageable pageable);

    /** "Farzandingiz bilan o'qing": books for the children's age, most read first. */
    @Query("select b from Book b left join fetch b.category where " + VISIBLE + " and b.ageGroup in :groups"
            + " order by b.downloadCount desc, b.id desc")
    List<Book> forAgeGroups(@Param("schoolId") Long schoolId, @Param("groups") Collection<AgeGroup> groups,
                            Pageable pageable);

    @Query(value = "select b from Book b left join fetch b.category where " + VISIBLE
            + " and exists (select 1 from BookFavorite f where f.book = b and f.chatId = :chatId) order by b.title",
            countQuery = "select count(b) from Book b where " + VISIBLE
                    + " and exists (select 1 from BookFavorite f where f.book = b and f.chatId = :chatId)")
    Page<Book> favorites(@Param("schoolId") Long schoolId, @Param("chatId") Long chatId, Pageable pageable);

    long countBySchoolIdAndActiveTrueAndDraftFalse(Long schoolId);

    long countByCategoryId(Long categoryId);

    List<Book> findByPermissionIdAndActiveTrue(Long permissionId);

    long countByPermissionId(Long permissionId);

    @Query("select b.permission.id, count(b) from Book b where b.school.id = :schoolId and b.permission is not null"
            + " group by b.permission.id")
    List<Object[]> countsByPermission(@Param("schoolId") Long schoolId);

    @Query("select b from Book b where b.permission.id = :permissionId order by b.title")
    List<Book> findByPermission(@Param("permissionId") Long permissionId);

    boolean existsBySchoolIdAndSourceUrl(Long schoolId, String sourceUrl);

    @Query("select b.sourceUrl from Book b where b.school.id = :schoolId and b.sourceUrl is not null")
    List<String> sourceUrls(@Param("schoolId") Long schoolId);

    /** Books saved before the author list: text in "author", no linked authors yet. */
    @Query("select b from Book b where b.author is not null and b.authors is empty")
    List<Book> unlinkedAuthors();

    /** Books per shelf (all states except drafts) — the counts on the shelves page, one query. */
    @Query("select b.category.id, count(b) from Book b where b.school.id = :schoolId and b.draft = false"
            + " and b.category is not null group by b.category.id")
    List<Object[]> countsByCategory(@Param("schoolId") Long schoolId);

    List<Book> findBySchoolIdAndPdfSha256(Long schoolId, String pdfSha256);

    /** The site's catalogue: every filter optional (null = any). */
    @Query(value = "select b from Book b left join fetch b.category where b.school.id = :schoolId"
            + " and (:q = '' or b.searchKey like :q)"
            + " and (:categoryId is null or b.category.id = :categoryId)"
            + " and (:language is null or b.language = :language)"
            + " and (:ageGroup is null or b.ageGroup = :ageGroup)"
            + " and (:legalBasis is null or b.legalBasis = :legalBasis)"
            + " and (:status = 'all' or (:status = 'active' and b.active = true and b.draft = false)"
            + "      or (:status = 'inactive' and b.active = false and b.draft = false) or (:status = 'draft' and b.draft = true))",
            countQuery = "select count(b) from Book b where b.school.id = :schoolId"
                    + " and (:q = '' or b.searchKey like :q)"
                    + " and (:categoryId is null or b.category.id = :categoryId)"
                    + " and (:language is null or b.language = :language)"
                    + " and (:ageGroup is null or b.ageGroup = :ageGroup)"
                    + " and (:legalBasis is null or b.legalBasis = :legalBasis)"
                    + " and (:status = 'all' or (:status = 'active' and b.active = true and b.draft = false)"
                    + "      or (:status = 'inactive' and b.active = false and b.draft = false) or (:status = 'draft' and b.draft = true))")
    Page<Book> catalogue(@Param("schoolId") Long schoolId, @Param("q") String q, @Param("categoryId") Long categoryId,
                         @Param("language") String language, @Param("ageGroup") AgeGroup ageGroup,
                         @Param("legalBasis") LegalBasis legalBasis, @Param("status") String status, Pageable pageable);

    @Modifying
    @Transactional
    @Query("update Book b set b.downloadCount = b.downloadCount + 1 where b.id = :id")
    int incrementDownloads(@Param("id") Long id);

    @Modifying
    @Transactional
    @Query("update Book b set b.likes = b.likes + :likes, b.dislikes = b.dislikes + :dislikes where b.id = :id")
    int addRatings(@Param("id") Long id, @Param("likes") int likes, @Param("dislikes") int dislikes);

    /** Telegram's id for the PDF we uploaded once — every later parent gets the file by it. */
    @Modifying
    @Transactional
    @Query("update Book b set b.telegramFileId = :fileId where b.id = :id")
    int rememberFileId(@Param("id") Long id, @Param("fileId") String fileId);
}
