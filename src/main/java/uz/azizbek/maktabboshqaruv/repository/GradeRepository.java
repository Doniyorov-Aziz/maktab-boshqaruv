package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Grade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface GradeRepository extends JpaRepository<Grade, Long> {

    @Query("select g from Grade g where g.student.schoolClass.academicYear.school.id = :schoolId")
    Page<Grade> findBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    List<Grade> findByStudentIdOrderByGradeDateDesc(Long studentId);

    @Query("select g.subject.id as subjectId, g.subject.name as subjectName, avg(g.score) as avgScore " +
            "from Grade g where g.student.id = :studentId group by g.subject.id, g.subject.name order by avgScore desc")
    List<Object[]> averageScoreByStudentGroupedBySubject(@Param("studentId") Long studentId);

    @Query("select g from Grade g where g.student.schoolClass.id = :schoolClassId and g.subject.id = :subjectId " +
            "and g.gradeDate between :from and :to order by g.gradeDate")
    List<Grade> findForGradebook(@Param("schoolClassId") Long schoolClassId, @Param("subjectId") Long subjectId,
                                  @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select avg(g.score) from Grade g where g.student.schoolClass.academicYear.school.id = :schoolId")
    Double averageScoreBySchoolId(@Param("schoolId") Long schoolId);

    @Query("select avg(g.score) from Grade g where g.student.schoolClass.academicYear.school.id = :schoolId " +
            "and g.gradeDate between :from and :to")
    Double averageScoreBySchoolIdBetween(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select g.subject.id as subjectId, g.subject.name as subjectName, avg(g.score) as avgScore " +
            "from Grade g where g.student.schoolClass.academicYear.school.id = :schoolId " +
            "group by g.subject.id, g.subject.name order by avgScore desc")
    List<Object[]> averageScoreBySubject(@Param("schoolId") Long schoolId);

    @Query("select sc.id as classId, avg(g.score) as avgScore from Grade g join g.student.schoolClass sc " +
            "where sc.academicYear.school.id = :schoolId group by sc.id")
    List<Object[]> averageScoreByClass(@Param("schoolId") Long schoolId);

    @Query("select sc.id as classId, avg(g.score) as avgScore from Grade g join g.student.schoolClass sc " +
            "where sc.academicYear.school.id = :schoolId and g.gradeDate between :from and :to group by sc.id")
    List<Object[]> averageScoreByClassBetween(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select g.student.id as studentId, avg(g.score) as avgScore from Grade g " +
            "where g.student.schoolClass.academicYear.school.id = :schoolId group by g.student.id")
    List<Object[]> averageScoreByStudent(@Param("schoolId") Long schoolId);

    @Query("select g.gradeDate as gradeDate, avg(g.score) as avgScore from Grade g " +
            "where g.student.schoolClass.academicYear.school.id = :schoolId and g.gradeDate between :from and :to " +
            "group by g.gradeDate order by g.gradeDate")
    List<Object[]> dailyAverageTrend(@Param("schoolId") Long schoolId, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
