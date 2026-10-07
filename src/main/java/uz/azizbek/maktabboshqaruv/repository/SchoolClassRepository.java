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

    /** [classTeacherId, gradeNumber, sectionLetter] for the given employees. */
    @org.springframework.data.jpa.repository.Query("select c.classTeacher.id, c.gradeNumber, c.sectionLetter from SchoolClass c " +
            "where c.classTeacher.id in :employeeIds")
    List<Object[]> ledBy(@org.springframework.data.repository.query.Param("employeeIds") java.util.Collection<Long> employeeIds);
    long countByAcademicYearSchoolId(Long schoolId);
    java.util.Optional<SchoolClass> findByClassTeacherId(Long classTeacherId);

    /** Every class an employee leads (a teacher may lead more than one). */
    @org.springframework.data.jpa.repository.Query("select c.id from SchoolClass c where c.classTeacher.id = :employeeId")
    List<Long> classIdsLedBy(@org.springframework.data.repository.query.Param("employeeId") Long employeeId);
}
