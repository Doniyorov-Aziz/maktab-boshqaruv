package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BroadcastAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

public interface BroadcastAttachmentRepository extends JpaRepository<BroadcastAttachment, Long> {

    List<BroadcastAttachment> findByBroadcastIdOrderByPosition(Long broadcastId);

    /** The files of a page of broadcasts in one query (history list). */
    @Query("select a from BroadcastAttachment a where a.broadcast.id in :ids order by a.broadcast.id, a.position")
    List<BroadcastAttachment> findByBroadcastIds(@Param("ids") Collection<Long> ids);

    /** Telegram's id for a file we uploaded once — every later recipient gets the file by it. */
    @Modifying
    @Transactional
    @Query("update BroadcastAttachment a set a.fileId = :fileId where a.id = :id and a.fileId is null")
    int rememberFileId(@Param("id") Long id, @Param("fileId") String fileId);
}
