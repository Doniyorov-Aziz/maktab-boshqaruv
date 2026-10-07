package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Appeal;
import uz.azizbek.maktabboshqaruv.entity.AppealEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppealRepository extends JpaRepository<Appeal, Long> {

    /** The parent's open draft (messages are being collected), if any. */
    Optional<Appeal> findFirstByChatIdAndStatusOrderByIdDesc(Long chatId, AppealEnums.Status status);

    /** Appeals a parent sent since a moment (the daily limit). */
    @Query("select count(a) from Appeal a where a.chatId = :chatId and a.status <> uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.DRAFT " +
            "and a.createdAt >= :since")
    long countSentSince(@Param("chatId") Long chatId, @Param("since") LocalDateTime since);

    /** Drafts nobody touched for a while — sent automatically. */
    List<Appeal> findByStatusAndLastMessageAtBefore(AppealEnums.Status status, LocalDateTime before);

    /** The parent's own recent appeals for the bot page (no drafts). */
    @Query("select a from Appeal a where a.chatId = :chatId and a.student.id = :studentId " +
            "and a.status <> uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.DRAFT order by a.lastMessageAt desc")
    List<Appeal> recentOfParent(@Param("chatId") Long chatId, @Param("studentId") Long studentId, Pageable pageable);

    /**
     * Staff list with filters. {@code classIds} null = every class (ADMIN); otherwise only
     * class-teacher appeals of those classes (a class teacher sees just their own classes).
     * Student, class and school come in the same query. No nullable date/text parameters
     * (PostgreSQL cannot type "? is null"): the caller passes a wide date range and '' for no search.
     */
    @Query(value = """
            select a from Appeal a join fetch a.student s join fetch s.schoolClass c
            where a.school.id = :schoolId
              and a.status <> uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.DRAFT
              and (:status is null or a.status = :status)
              and (:target is null or a.target = :target)
              and (:classId is null or c.id = :classId)
              and a.lastMessageAt >= :from and a.lastMessageAt < :to
              and (:allClasses = true or (a.target = uz.azizbek.maktabboshqaruv.entity.AppealEnums.Target.CLASS_TEACHER and c.id in :classIds))
              and (:q = '' or lower(concat(coalesce(a.parentName, ''), ' ', s.firstName, ' ', s.lastName)) like :q
                   or cast(a.id as string) = :idText)
            """,
            countQuery = """
            select count(a) from Appeal a join a.student s join s.schoolClass c
            where a.school.id = :schoolId
              and a.status <> uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.DRAFT
              and (:status is null or a.status = :status)
              and (:target is null or a.target = :target)
              and (:classId is null or c.id = :classId)
              and a.lastMessageAt >= :from and a.lastMessageAt < :to
              and (:allClasses = true or (a.target = uz.azizbek.maktabboshqaruv.entity.AppealEnums.Target.CLASS_TEACHER and c.id in :classIds))
              and (:q = '' or lower(concat(coalesce(a.parentName, ''), ' ', s.firstName, ' ', s.lastName)) like :q
                   or cast(a.id as string) = :idText)
            """)
    Page<Appeal> search(@Param("schoolId") Long schoolId,
                        @Param("status") AppealEnums.Status status,
                        @Param("target") AppealEnums.Target target,
                        @Param("classId") Long classId,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to,
                        @Param("allClasses") boolean allClasses,
                        @Param("classIds") Collection<Long> classIds,
                        @Param("q") String q,
                        @Param("idText") String idText,
                        Pageable pageable);

    /** Unread parent messages visible to the user (the sidebar badge). */
    @Query("""
            select coalesce(sum(a.unreadCount), 0) from Appeal a join a.student s
            where a.school.id = :schoolId
              and a.status <> uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.DRAFT
              and (:allClasses = true or (a.target = uz.azizbek.maktabboshqaruv.entity.AppealEnums.Target.CLASS_TEACHER
                   and s.schoolClass.id in :classIds))
            """)
    long unreadFor(@Param("schoolId") Long schoolId, @Param("allClasses") boolean allClasses,
                   @Param("classIds") Collection<Long> classIds);

    @Query("select a from Appeal a join fetch a.student s join fetch s.schoolClass where a.id = :id")
    Optional<Appeal> findWithStudent(@Param("id") Long id);

    long countBySchoolIdAndStatus(Long schoolId, AppealEnums.Status status);

    /** Markers of old parent_message rows already moved into appeals ("pm#12"). */
    @Query("select a.createdBy from Appeal a where a.createdBy like 'pm#%'")
    List<String> migratedMarkers();

    /** For "Bot statistikasi": appeals that arrived in a period. */
    @Query("select count(a) from Appeal a where a.school.id = :schoolId " +
            "and a.status <> uz.azizbek.maktabboshqaruv.entity.AppealEnums.Status.DRAFT and a.createdAt >= :since")
    long countSince(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since);
}
