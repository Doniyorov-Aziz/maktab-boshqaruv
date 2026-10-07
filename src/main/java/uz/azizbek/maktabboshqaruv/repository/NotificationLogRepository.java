package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.NotificationLog;
import uz.azizbek.maktabboshqaruv.entity.NotificationStatus;
import uz.azizbek.maktabboshqaruv.entity.NotificationType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>, JpaSpecificationExecutor<NotificationLog> {

    boolean existsByStudentIdAndTypeAndReferenceIdAndRecordDate(Long studentId, NotificationType type,
                                                               Long referenceId, LocalDate recordDate);

    boolean existsByTypeAndReferenceId(NotificationType type, Long referenceId);

    @Query("select n from NotificationLog n where n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.PENDING " +
            "and n.scheduledAt <= :now order by n.scheduledAt, n.id")
    List<NotificationLog> findDue(@Param("now") LocalDateTime now, Pageable pageable);

    /** Everything held back for one chat by quiet hours and now due — sent together as one morning note. */
    @Query("select n from NotificationLog n where n.chatId = :chatId and n.quietBundle = true " +
            "and n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.PENDING " +
            "and n.scheduledAt <= :now order by n.createdAt, n.id")
    List<NotificationLog> findDueQuietBundle(@Param("chatId") Long chatId, @Param("now") LocalDateTime now);

    long countBySchoolIdAndStatusAndSentAtBetween(Long schoolId, NotificationStatus status,
                                                   LocalDateTime from, LocalDateTime to);

    long countBySchoolIdAndStatusAndCreatedAtBetween(Long schoolId, NotificationStatus status,
                                                      LocalDateTime from, LocalDateTime to);

    long countBySchoolIdAndStatus(Long schoolId, NotificationStatus status);

    boolean existsByChatIdAndTypeAndReferenceIdAndRecordDate(Long chatId, NotificationType type, Long referenceId, LocalDate recordDate);

    long countByTypeAndReferenceIdAndStatus(NotificationType type, Long referenceId, NotificationStatus status);

    long countByTypeAndReferenceId(NotificationType type, Long referenceId);

    /** [day, count] of SENT messages per day — the chart on "Bot statistikasi". */
    @Query("select cast(n.sentAt as LocalDate), count(n) from NotificationLog n where n.school.id = :schoolId " +
            "and n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.SENT and n.sentAt >= :since " +
            "group by cast(n.sentAt as LocalDate) order by cast(n.sentAt as LocalDate)")
    List<Object[]> sentPerDay(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since);

    /** After a 403 there is no point delivering the rest of that chat's queue. */
    @Transactional
    @Modifying
    @Query("update NotificationLog n set n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.SKIPPED, " +
            "n.lastError = :reason where n.chatId = :chatId and n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.PENDING")
    int skipPendingForChat(@Param("chatId") Long chatId, @Param("reason") String reason);

    /** Delivery result of several broadcasts in one query: [broadcastId, status, count]. */
    @Query("select n.referenceId, n.status, count(n) from NotificationLog n " +
            "where n.type = uz.azizbek.maktabboshqaruv.entity.NotificationType.BROADCAST and n.referenceId in :ids " +
            "group by n.referenceId, n.status")
    List<Object[]> broadcastStatusCounts(@Param("ids") java.util.Collection<Long> ids);

    /**
     * Who a broadcast went to: student, class, the parent's name, status and time.
     * {@code status} '' = any; {@code q} '' = no search.
     */
    @Query(value = """
            select new uz.azizbek.maktabboshqaruv.dto.BroadcastRecipientDto(n.id, s.id,
                   concat(s.lastName, ' ', s.firstName), c.gradeNumber, c.sectionLetter,
                   (select max(l.firstName) from ParentTelegramLink l where l.student = s and l.chatId = n.chatId),
                   cast(n.status as string), n.createdAt, n.sentAt, n.lastError)
            from NotificationLog n join n.student s join s.schoolClass c
            where n.type = uz.azizbek.maktabboshqaruv.entity.NotificationType.BROADCAST and n.referenceId = :id
              and (:status = '' or cast(n.status as string) = :status)
              and (:classId is null or c.id = :classId)
              and (:q = '' or lower(concat(s.lastName, ' ', s.firstName)) like :q)
            order by c.gradeNumber, c.sectionLetter, s.lastName, s.firstName
            """,
            countQuery = """
            select count(n) from NotificationLog n join n.student s join s.schoolClass c
            where n.type = uz.azizbek.maktabboshqaruv.entity.NotificationType.BROADCAST and n.referenceId = :id
              and (:status = '' or cast(n.status as string) = :status)
              and (:classId is null or c.id = :classId)
              and (:q = '' or lower(concat(s.lastName, ' ', s.firstName)) like :q)
            """)
    org.springframework.data.domain.Page<uz.azizbek.maktabboshqaruv.dto.BroadcastRecipientDto> broadcastRecipients(
            @Param("id") Long broadcastId, @Param("status") String status, @Param("classId") Long classId,
            @Param("q") String q, Pageable pageable);

    /** For the 30-day chart: [day the message was queued, status, count]. */
    @Query("select cast(n.createdAt as LocalDate), n.status, count(n) from NotificationLog n " +
            "where n.school.id = :schoolId and n.createdAt >= :since " +
            "group by cast(n.createdAt as LocalDate), n.status")
    List<Object[]> statusPerDay(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since);
}
