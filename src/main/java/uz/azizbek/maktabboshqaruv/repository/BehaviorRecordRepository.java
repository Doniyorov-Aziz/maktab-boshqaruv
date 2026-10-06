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

    /** List page: student and class fetched in the same query (no N+1), optional class filter. */
    @Query(value = "select b from BehaviorRecord b join fetch b.student s join fetch s.schoolClass c " +
            "where c.academicYear.school.id = :schoolId and (:classId is null or c.id = :classId)",
            countQuery = "select count(b) from BehaviorRecord b " +
                    "where b.student.schoolClass.academicYear.school.id = :schoolId " +
                    "and (:classId is null or b.student.schoolClass.id = :classId)")
    Page<BehaviorRecord> search(@Param("schoolId") Long schoolId, @Param("classId") Long classId, Pageable pageable);

    @Query("select b from BehaviorRecord b where b.student.schoolClass.academicYear.school.id = :schoolId order by b.recordDate desc, b.id desc")
    List<BehaviorRecord> findLatestBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    List<BehaviorRecord> findByStudentIdOrderByRecordDateDesc(Long studentId);

    List<BehaviorRecord> findByStudentIdAndRecordDateBetweenOrderByRecordDateDesc(Long studentId, java.time.LocalDate from, java.time.LocalDate to);

    long countByStudentIdAndType(Long studentId, uz.azizbek.maktabboshqaruv.entity.BehaviorType type);
}
