package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.AbsenceRequest;
import uz.azizbek.maktabboshqaruv.entity.AbsenceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbsenceRequestRepository extends JpaRepository<AbsenceRequest, Long> {
    Page<AbsenceRequest> findBySchoolIdOrderByCreatedAtDesc(Long schoolId, Pageable pageable);

    Page<AbsenceRequest> findBySchoolIdAndStatusOrderByCreatedAtDesc(Long schoolId, AbsenceStatus status, Pageable pageable);

    long countBySchoolIdAndStatus(Long schoolId, AbsenceStatus status);

    List<AbsenceRequest> findTop5ByChatIdAndStudentIdOrderByCreatedAtDesc(Long chatId, Long studentId);
}
