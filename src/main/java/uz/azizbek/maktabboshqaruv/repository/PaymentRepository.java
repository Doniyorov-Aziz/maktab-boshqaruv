package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.Payment;

import java.time.LocalDate;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query(value = "select p from Payment p join fetch p.student s join fetch s.schoolClass c where p.school.id = :schoolId " +
            "and (:classId is null or c.id = :classId) and (:studentId is null or s.id = :studentId) " +
            "and (cast(:month as date) is null or p.month = :month)",
            countQuery = "select count(p) from Payment p where p.school.id = :schoolId " +
                    "and (:classId is null or p.student.schoolClass.id = :classId) and (:studentId is null or p.student.id = :studentId) " +
                    "and (cast(:month as date) is null or p.month = :month)")
    Page<Payment> search(@Param("schoolId") Long schoolId, @Param("classId") Long classId, @Param("studentId") Long studentId,
                         @Param("month") LocalDate month, Pageable pageable);

    List<Payment> findByStudentIdAndMonthBetweenOrderByMonthAscPaidOnAsc(Long studentId, LocalDate from, LocalDate to);
}
