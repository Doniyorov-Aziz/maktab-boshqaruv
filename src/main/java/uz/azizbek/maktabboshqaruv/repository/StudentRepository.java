package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Page<Student> findBySchoolClassAcademicYearSchoolId(Long schoolId, Pageable pageable);

    /** [classId, count] for the given classes — one query for a whole page of class cards. */
    @org.springframework.data.jpa.repository.Query(
            "select s.schoolClass.id, count(s) from Student s where s.schoolClass.id in :classIds group by s.schoolClass.id")
    List<Object[]> countByClassIds(@org.springframework.data.repository.query.Param("classIds") java.util.Collection<Long> classIds);

    /** Students list: optional class filter and a first/last name search (either order). */
    @org.springframework.data.jpa.repository.Query("""
            select s from Student s where s.schoolClass.academicYear.school.id = :schoolId
              and (:classId is null or s.schoolClass.id = :classId)
              and (:q is null
                   or lower(concat(s.firstName, ' ', s.lastName)) like :q
                   or lower(concat(s.lastName, ' ', s.firstName)) like :q)
            """)
    Page<Student> search(@org.springframework.data.repository.query.Param("schoolId") Long schoolId,
                         @org.springframework.data.repository.query.Param("classId") Long classId,
                         @org.springframework.data.repository.query.Param("q") String q,
                         Pageable pageable);
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
