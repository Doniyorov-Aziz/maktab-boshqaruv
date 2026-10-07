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

    /** History with a date range and text search ('' = none); no nullable parameters. */
    @Query("select b from Broadcast b where b.school.id = :schoolId and b.createdAt >= :since and b.createdAt < :until " +
            "and (:q = '' or lower(b.text) like :q) order by b.createdAt desc")
    Page<Broadcast> search(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since,
                           @Param("until") LocalDateTime until, @Param("q") String q, Pageable pageable);
}
