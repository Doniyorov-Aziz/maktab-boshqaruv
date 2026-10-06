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

    /** Active subjects only (a null column counts as active) — what new grades/lessons can pick. */
    @org.springframework.data.jpa.repository.Query(
            "select s from Subject s where s.school.id = :schoolId and (s.active is null or s.active = true)")
    Page<Subject> findActiveBySchoolId(@org.springframework.data.repository.query.Param("schoolId") Long schoolId,
                                       Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            "select count(s) > 0 from Subject s where s.school.id = :schoolId and lower(s.name) = lower(:name) " +
            "and (s.active is null or s.active = true)")
    boolean existsActiveBySchoolIdAndName(@org.springframework.data.repository.query.Param("schoolId") Long schoolId,
                                          @org.springframework.data.repository.query.Param("name") String name);
}
