package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    boolean existsByName(String name);
    boolean existsBySchoolIdAndName(Long schoolId, String name);
    Page<Subject> findBySchoolId(Long schoolId, Pageable pageable);
    List<Subject> findBySchoolId(Long schoolId);
    long countBySchoolId(Long schoolId);
}
