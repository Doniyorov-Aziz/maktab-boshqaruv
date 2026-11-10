package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.Achievement;

import java.time.LocalDate;
import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

    @Query(value = "select a from Achievement a join fetch a.student s join fetch s.schoolClass c left join fetch a.subject " +
            "where a.school.id = :schoolId and (:classId is null or c.id = :classId) and (:studentId is null or s.id = :studentId) " +
            "and (:category is null or a.category = :category) and (:level is null or a.level = :level) " +
            "and (cast(:from as date) is null or a.achievedOn >= :from) and (cast(:to as date) is null or a.achievedOn <= :to)",
            countQuery = "select count(a) from Achievement a where a.school.id = :schoolId " +
                    "and (:classId is null or a.student.schoolClass.id = :classId) and (:studentId is null or a.student.id = :studentId) " +
                    "and (:category is null or a.category = :category) and (:level is null or a.level = :level) " +
                    "and (cast(:from as date) is null or a.achievedOn >= :from) and (cast(:to as date) is null or a.achievedOn <= :to)")
    Page<Achievement> search(@Param("schoolId") Long schoolId, @Param("classId") Long classId, @Param("studentId") Long studentId,
                             @Param("category") Achievement.Category category, @Param("level") Achievement.Level level,
                             @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("select a from Achievement a left join fetch a.subject where a.student.id = :studentId order by a.achievedOn desc, a.id desc")
    List<Achievement> ofStudent(@Param("studentId") Long studentId);
}
