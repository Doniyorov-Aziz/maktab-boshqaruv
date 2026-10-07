package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.AppealEnums;
import uz.azizbek.maktabboshqaruv.entity.AppealMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AppealMessageRepository extends JpaRepository<AppealMessage, Long> {

    List<AppealMessage> findByAppealIdOrderByCreatedAtAscIdAsc(Long appealId);

    /** What the school sees: everything except messages the parent is still composing. */
    @Query("select m from AppealMessage m where m.appeal.id = :appealId and (m.pending is null or m.pending = false) " +
            "order by m.createdAt asc, m.id asc")
    List<AppealMessage> visibleOf(@Param("appealId") Long appealId);

    @Query("select m from AppealMessage m where m.appeal.id = :appealId and m.pending = true order by m.createdAt asc, m.id asc")
    List<AppealMessage> pendingOf(@Param("appealId") Long appealId);

    long countByAppealId(Long appealId);

    /** Appeals whose composing parent went quiet (the 30-minute auto-send). */
    @Query("select m.appeal.id from AppealMessage m where m.pending = true group by m.appeal.id having max(m.createdAt) < :before")
    List<Long> idleComposing(@Param("before") LocalDateTime before);

    /** Files still waiting to be downloaded (retried after a restart or a Telegram hiccup). */
    List<AppealMessage> findByFileStateAndCreatedAtBefore(AppealEnums.FileState state, LocalDateTime before);

    /** For "Bot statistikasi": average minutes from a parent's appeal to the school's first reply. */
    @Query(value = """
            select avg(extract(epoch from (r.first_reply - a.created_at)) / 60.0)
            from appeal a
            join (select m.appeal_id, min(m.created_at) as first_reply from appeal_message m
                  where m.direction = 'OUT' group by m.appeal_id) r on r.appeal_id = a.id
            where a.school_id = :schoolId and a.created_at >= :since
            """, nativeQuery = true)
    Double averageFirstReplyMinutes(@Param("schoolId") Long schoolId, @Param("since") LocalDateTime since);
}
