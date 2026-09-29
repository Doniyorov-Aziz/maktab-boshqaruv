package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.BehaviorRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BehaviorRecordRepository extends JpaRepository<BehaviorRecord, Long> {

    @Query("select b from BehaviorRecord b where b.student.schoolClass.academicYear.school.id = :schoolId")
    Page<BehaviorRecord> findBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    @Query("select b from BehaviorRecord b where b.student.schoolClass.academicYear.school.id = :schoolId order by b.recordDate desc, b.id desc")
    List<BehaviorRecord> findLatestBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    List<BehaviorRecord> findByStudentIdOrderByRecordDateDesc(Long studentId);

    long countByStudentIdAndType(Long studentId, uz.azizbek.maktabboshqaruv.entity.BehaviorType type);
}
