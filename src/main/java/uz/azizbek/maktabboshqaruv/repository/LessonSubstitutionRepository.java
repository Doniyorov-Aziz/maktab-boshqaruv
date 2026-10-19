package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.LessonSubstitution;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LessonSubstitutionRepository extends JpaRepository<LessonSubstitution, Long> {

    @Query("select s from LessonSubstitution s join fetch s.lessonSlot l join fetch s.substitute join fetch s.absentTeacher "
            + "join l.schoolClass c join c.academicYear y where y.school.id = :schoolId and s.date between :from and :to")
    List<LessonSubstitution> findInRange(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<LessonSubstitution> findByLessonSlotIdAndDate(Long lessonSlotId, LocalDate date);

    @Modifying
    @Query("delete from LessonSubstitution s where s.lessonSlot.id = :lessonSlotId")
    void deleteByLessonSlotId(@Param("lessonSlotId") Long lessonSlotId);
}
