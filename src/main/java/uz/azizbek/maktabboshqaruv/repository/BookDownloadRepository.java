package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BookDownload;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BookDownloadRepository extends JpaRepository<BookDownload, Long> {

    /** The daily limit: how many books this parent asked for since the start of the day. */
    long countByChatIdAndDownloadedAtGreaterThanEqual(Long chatId, LocalDateTime since);

    /** A second tap on the same button within a few seconds sends the book once. */
    boolean existsByChatIdAndBookIdAndDownloadedAtGreaterThanEqual(Long chatId, Long bookId, LocalDateTime since);

    /** This parent's favourite shelves: shelf id and number of downloads, most first. */
    @Query("select b.category.id, count(d) from BookDownload d join d.book b where d.chatId = :chatId"
            + " and b.school.id = :schoolId and b.category is not null group by b.category.id order by count(d) desc")
    List<Object[]> categoriesOf(@Param("chatId") Long chatId, @Param("schoolId") Long schoolId, Pageable pageable);

    /** Most downloaded books of the school since a moment: book id and count. */
    @Query("select d.book.id, count(d) from BookDownload d join d.book b where d.school.id = :schoolId"
            + " and d.downloadedAt >= :since and b.active = true and b.draft = false"
            + " group by d.book.id order by count(d) desc, d.book.id desc")
    List<Object[]> popularSince(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since, Pageable pageable);

    /** Books this parent downloaded, the latest first (book id, last time) — "📥 Yuklab olganlarim". */
    @Query("select d.book.id, max(d.downloadedAt) from BookDownload d join d.book b where d.chatId = :chatId"
            + " and b.school.id = :schoolId and b.active = true and b.draft = false"
            + " group by d.book.id order by max(d.downloadedAt) desc")
    List<Object[]> historyOf(@Param("chatId") Long chatId, @Param("schoolId") Long schoolId, Pageable pageable);

    @Query("select count(distinct d.book.id) from BookDownload d join d.book b where d.chatId = :chatId"
            + " and b.school.id = :schoolId and b.active = true and b.draft = false")
    long historyCount(@Param("chatId") Long chatId, @Param("schoolId") Long schoolId);
}
