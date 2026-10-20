package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AcademicYearRepository extends JpaRepository<AcademicYear, Long> {
    Page<AcademicYear> findBySchoolId(Long schoolId, Pageable pageable);

    /** School years list with a title search (lower(...) like :q, null = all). */
    @org.springframework.data.jpa.repository.Query(
            "select y from AcademicYear y where y.school.id = :schoolId and (:q is null or lower(y.title) like :q)")
    Page<AcademicYear> search(@org.springframework.data.repository.query.Param("schoolId") Long schoolId,
                              @org.springframework.data.repository.query.Param("q") String q,
                              Pageable pageable);
}
