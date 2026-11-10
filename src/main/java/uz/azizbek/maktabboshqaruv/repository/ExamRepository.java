package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.Exam;

import java.time.LocalDate;
import java.util.List;

public interface ExamRepository extends JpaRepository<Exam, Long> {

    @Query(value = "select x from Exam x join fetch x.schoolClass c join fetch x.subject s left join fetch x.employee e " +
            "where x.school.id = :schoolId and (:classId is null or c.id = :classId) and (:subjectId is null or s.id = :subjectId) " +
            "and (cast(:from as date) is null or x.examDate >= :from) and (cast(:to as date) is null or x.examDate <= :to)",
            countQuery = "select count(x) from Exam x where x.school.id = :schoolId " +
                    "and (:classId is null or x.schoolClass.id = :classId) and (:subjectId is null or x.subject.id = :subjectId) " +
                    "and (cast(:from as date) is null or x.examDate >= :from) and (cast(:to as date) is null or x.examDate <= :to)")
    Page<Exam> search(@Param("schoolId") Long schoolId, @Param("classId") Long classId, @Param("subjectId") Long subjectId,
                      @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("select x from Exam x join fetch x.subject join fetch x.schoolClass where x.schoolClass.id = :classId " +
            "and x.examDate between :from and :to order by x.examDate, x.startTime, x.id")
    List<Exam> ofClass(@Param("classId") Long classId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select x from Exam x join fetch x.subject join fetch x.schoolClass c join fetch x.school " +
            "where x.examDate = :date and x.notifyParents = true")
    List<Exam> onDateToRemind(@Param("date") LocalDate date);

    /** Another exam of the class that day — two big tests on one day are flagged on the form. */
    @Query("select x from Exam x join fetch x.subject where x.schoolClass.id = :classId and x.examDate = :date and x.id <> :exceptId")
    List<Exam> sameDay(@Param("classId") Long classId, @Param("date") LocalDate date, @Param("exceptId") Long exceptId);
}
