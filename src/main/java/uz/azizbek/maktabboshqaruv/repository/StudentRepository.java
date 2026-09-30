package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Page<Student> findBySchoolClassAcademicYearSchoolId(Long schoolId, Pageable pageable);
    long countBySchoolClassAcademicYearSchoolId(Long schoolId);
    List<Student> findBySchoolClassIdOrderByLastNameAscFirstNameAsc(Long schoolClassId);
    List<Student> findBySchoolClassAcademicYearSchoolId(Long schoolId);

    java.util.Optional<Student> findByTelegramLinkCode(String telegramLinkCode);
    boolean existsByTelegramLinkCode(String telegramLinkCode);
    List<Student> findByTelegramLinkCodeIsNull();

    // Pre-filter on the last 9 digits whatever the stored formatting ("+998 90 123-45-67",
    // "901234567", ...); the caller then compares fully normalized numbers.
    @org.springframework.data.jpa.repository.Query(nativeQuery = true, value =
            "select * from students where guardian_phone is not null " +
            "and right(regexp_replace(guardian_phone, '[^0-9]', '', 'g'), 9) = :last9")
    List<Student> findByGuardianPhoneLast9(@org.springframework.data.repository.query.Param("last9") String last9);
}
