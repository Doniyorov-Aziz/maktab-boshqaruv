package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.DemoVisit;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface DemoVisitRepository extends JpaRepository<DemoVisit, Long> {

    Optional<DemoVisit> findFirstByVisitorIdOrderByLastSeenDesc(String visitorId);

    List<DemoVisit> findByFirstSeenGreaterThanEqualOrderByFirstSeenDesc(LocalDateTime since);

    @Query("select count(distinct v.visitorId) from DemoVisit v")
    long countVisitors();

    @Query("select count(distinct v.visitorId) from DemoVisit v where v.lastSeen >= :since")
    long countVisitorsSince(@Param("since") LocalDateTime since);

    @Modifying
    @Query("delete from DemoVisit v where v.visitorId = :visitorId")
    int deleteByVisitorId(@Param("visitorId") String visitorId);
}
