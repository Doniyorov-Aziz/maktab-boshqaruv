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

    boolean existsBySubjectId(Long subjectId);

    @Query("select g from Grade g where g.student.schoolClass.academicYear.school.id = :schoolId")
    Page<Grade> findBySchoolId(@Param("schoolId") Long schoolId, Pageable pageable);

    /** A student's grades with subject and student (class, year, school) in the same query — no per-row loads. */
    @Query("select g from Grade g join fetch g.subject join fetch g.student s join fetch s.schoolClass c " +
            "join fetch c.academicYear y join fetch y.school left join fetch c.classTeacher ct left join fetch ct.position " +
            "where s.id = :studentId order by g.gradeDate desc")
    List<Grade> findByStudentIdOrderByGradeDateDesc(@Param("studentId") Long studentId);

    @Query("select g from Grade g join fetch g.subject join fetch g.student s join fetch s.schoolClass c " +
            "join fetch c.academicYear y join fetch y.school left join fetch c.classTeacher ct left join fetch ct.position " +
            "where s.id = :studentId and g.gradeDate between :from and :to order by g.gradeDate desc, g.id desc")
    List<Grade> findByStudentIdAndGradeDateBetweenOrderByGradeDateDescIdDesc(@Param("studentId") Long studentId,
                                                                             @Param("from") LocalDate from,
                                                                             @Param("to") LocalDate to);

    @Query("select g.subject.id as subjectId, g.subject.name as subjectName, avg(g.score) as avgScore " +
            "from Grade g where g.student.id = :studentId group by g.subject.id, g.subject.name order by avgScore desc")
    List<Object[]> averageScoreByStudentGroupedBySubject(@Param("studentId") Long studentId);

    @Query("select g from Grade g where g.student.schoolClass.id = :schoolClassId and g.subject.id = :subjectId " +
            "and g.gradeDate between :from and :to order by g.gradeDate")
    List<Grade> findForGradebook(@Param("schoolClassId") Long schoolClassId, @Param("subjectId") Long subjectId,
                                  @Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Class average per subject (no student data leaves the query — only the averages). */
    @Query("select g.subject.id, avg(g.score) from Grade g where g.student.schoolClass.id = :schoolClassId " +
            "and g.gradeDate between :from and :to group by g.subject.id")
    List<Object[]> classAverageBySubjectBetween(@Param("schoolClassId") Long schoolClassId,
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

    /**
     * The journal in one query, columns only: every student of the class (alphabetical by surname)
     * with that month's grades in the subject — [studentId, lastName, firstName, gradeId, date,
     * score, type, comment, createdBy, createdDate]; the grade columns are null for a student
     * without grades.
     */
    @Query("""
            select s.id, s.lastName, s.firstName, g.id, g.gradeDate, g.score, g.type, g.comment, g.createdBy, g.createdDate
            from Student s
            left join Grade g on g.student = s and g.subject.id = :subjectId and g.gradeDate between :from and :to
            where s.schoolClass.id = :classId
            order by s.lastName, s.firstName, s.id, g.gradeDate, g.id
            """)
    List<Object[]> journal(@Param("classId") Long classId, @Param("subjectId") Long subjectId,
                           @Param("from") LocalDate from, @Param("to") LocalDate to);
}
