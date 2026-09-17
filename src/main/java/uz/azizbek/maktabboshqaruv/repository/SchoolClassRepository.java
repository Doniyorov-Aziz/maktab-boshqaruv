package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.AcademicYear;
import uz.azizbek.maktabboshqaruv.entity.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolClassRepository extends JpaRepository<SchoolClass, Long> {
    boolean existsByAcademicYearAndGradeNumberAndSectionLetter(AcademicYear academicYear, Integer gradeNumber, String sectionLetter);
}
