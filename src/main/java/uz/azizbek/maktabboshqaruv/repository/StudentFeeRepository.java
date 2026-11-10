package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.StudentFee;

import java.util.List;
import java.util.Optional;

public interface StudentFeeRepository extends JpaRepository<StudentFee, Long> {

    Optional<StudentFee> findByStudentId(Long studentId);

    @Query("select f from StudentFee f join fetch f.student s where s.schoolClass.academicYear.school.id = :schoolId")
    List<StudentFee> ofSchool(@Param("schoolId") Long schoolId);
}
