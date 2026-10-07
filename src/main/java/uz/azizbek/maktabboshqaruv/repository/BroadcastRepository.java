package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Broadcast;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface BroadcastRepository extends JpaRepository<Broadcast, Long> {
    Page<Broadcast> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    /**
     * History with a date range, text search, class and delivery status ('' = any):
     * a message matches a class if it reached a parent of that class, and a status if at
     * least one of its recipients is in it (e.g. FAILED = "had errors", PENDING = "still sending").
     */
    @Query("""
            select b from Broadcast b where b.school.id = :schoolId and b.createdAt >= :since and b.createdAt < :until
              and (:q = '' or lower(b.text) like :q)
              and (:classId is null or exists (select 1 from NotificationLog n join n.student s
                   where n.type = uz.azizbek.maktabboshqaruv.entity.NotificationType.BROADCAST
                     and n.referenceId = b.id and s.schoolClass.id = :classId))
              and (:status = '' or exists (select 1 from NotificationLog n
                   where n.type = uz.azizbek.maktabboshqaruv.entity.NotificationType.BROADCAST
                     and n.referenceId = b.id and cast(n.status as string) = :status))
            order by b.createdAt desc
            """)
    Page<Broadcast> search(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since,
                           @Param("until") LocalDateTime until, @Param("q") String q,
                           @Param("classId") Long classId, @Param("status") String status, Pageable pageable);
}
