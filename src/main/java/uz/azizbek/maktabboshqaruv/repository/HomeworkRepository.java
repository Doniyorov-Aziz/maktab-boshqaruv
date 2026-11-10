package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.Homework;

import java.time.LocalDate;
import java.util.List;

public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    /** The staff list: class, subject and teacher fetched with it (one query), every filter optional. */
    @Query(value = "select h from Homework h join fetch h.schoolClass c join fetch h.subject s left join fetch h.employee e " +
            "where h.school.id = :schoolId and (:classId is null or c.id = :classId) " +
            "and (:subjectId is null or s.id = :subjectId) and (:employeeId is null or e.id = :employeeId) " +
            "and (cast(:from as date) is null or h.dueDate >= :from) and (cast(:to as date) is null or h.assignedDate <= :to)",
            countQuery = "select count(h) from Homework h where h.school.id = :schoolId " +
                    "and (:classId is null or h.schoolClass.id = :classId) and (:subjectId is null or h.subject.id = :subjectId) " +
                    "and (:employeeId is null or h.employee.id = :employeeId) " +
                    "and (cast(:from as date) is null or h.dueDate >= :from) and (cast(:to as date) is null or h.assignedDate <= :to)")
    Page<Homework> search(@Param("schoolId") Long schoolId, @Param("classId") Long classId, @Param("subjectId") Long subjectId,
                          @Param("employeeId") Long employeeId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                          Pageable pageable);

    /** A class's homework still to do on {@code from} or later (the parents' bot), nearest due date first. */
    @Query("select h from Homework h join fetch h.subject where h.schoolClass.id = :classId and h.dueDate >= :from " +
            "order by h.dueDate, h.id")
    List<Homework> upcoming(@Param("classId") Long classId, @Param("from") LocalDate from);

    /** Given in a range (the bot's "past" page), latest first. */
    @Query("select h from Homework h join fetch h.subject where h.schoolClass.id = :classId " +
            "and h.assignedDate between :from and :to order by h.assignedDate desc, h.id desc")
    List<Homework> givenBetween(@Param("classId") Long classId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Modifying
    @org.springframework.transaction.annotation.Transactional
    @Query("update Homework h set h.fileTelegramId = :fileId where h.id = :id")
    void rememberFileId(@Param("id") Long id, @Param("fileId") String fileId);
}
