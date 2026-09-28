package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    boolean existsByAcademicYearAndGradeNumberAndSectionLetter(AcademicYear academicYear, Integer gradeNumber, String sectionLetter);
    Page<SchoolClass> findByAcademicYearSchoolId(Long schoolId, Pageable pageable);
    List<SchoolClass> findByAcademicYearSchoolId(Long schoolId);
    long countByAcademicYearSchoolId(Long schoolId);
}
