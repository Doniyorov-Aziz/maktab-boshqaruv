package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.ParentMessage;
import uz.azizbek.maktabboshqaruv.entity.ParentMessageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ParentMessageRepository extends JpaRepository<ParentMessage, Long> {
    Page<ParentMessage> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    Page<ParentMessage> findBySchoolIdAndStatusOrderByCreatedAtDesc(Long schoolId, ParentMessageStatus status, Pageable pageable);

    long countBySchoolIdAndStatus(Long schoolId, ParentMessageStatus status);

    List<ParentMessage> findTop5ByChatIdAndStudentIdOrderByCreatedAtDesc(Long chatId, Long studentId);
}
