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

    long countBySchoolIdAndStatusAndSentAtBetween(Long schoolId, NotificationStatus status,
                                                   LocalDateTime from, LocalDateTime to);

    long countBySchoolIdAndStatusAndCreatedAtBetween(Long schoolId, NotificationStatus status,
                                                      LocalDateTime from, LocalDateTime to);

    long countBySchoolIdAndStatus(Long schoolId, NotificationStatus status);

    /** After a 403 there is no point delivering the rest of that chat's queue. */
    @Transactional
    @Modifying
    @Query("update NotificationLog n set n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.SKIPPED, " +
            "n.lastError = :reason where n.chatId = :chatId and n.status = uz.azizbek.maktabboshqaruv.entity.NotificationStatus.PENDING")
    int skipPendingForChat(@Param("chatId") Long chatId, @Param("reason") String reason);
}
