package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Page<Student> findBySchoolClassAcademicYearSchoolId(Long schoolId, Pageable pageable);
    long countBySchoolClassAcademicYearSchoolId(Long schoolId);
    List<Student> findBySchoolClassIdOrderByLastNameAscFirstNameAsc(Long schoolClassId);
    List<Student> findBySchoolClassAcademicYearSchoolId(Long schoolId);
}
