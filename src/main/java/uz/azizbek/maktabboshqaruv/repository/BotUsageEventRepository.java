package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BotUsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface BotUsageEventRepository extends JpaRepository<BotUsageEvent, Long> {

    @Query("select count(distinct e.chatId) from BotUsageEvent e where e.schoolId = :schoolId and e.createdAt >= :since")
    long countActiveChats(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since);

    @Query("select e.section, count(e) from BotUsageEvent e where e.schoolId = :schoolId and e.createdAt >= :since " +
            "group by e.section order by count(e) desc")
    List<Object[]> countBySection(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since);
}
