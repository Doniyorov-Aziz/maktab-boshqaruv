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

    /**
     * Students list: optional class filter and a first/last name search (either order). The class
     * (with its year, school and teacher — eager in SchoolClass) and both places (tuman and its
     * hudud) come in the same statement — no query per row or per class.
     */
    @org.springframework.data.jpa.repository.Query(value = """
            select s from Student s join fetch s.schoolClass c
              join fetch c.academicYear y join fetch y.school
              left join fetch c.classTeacher ct left join fetch ct.position left join fetch ct.school
              left join fetch s.birthDistrict bd left join fetch bd.region
              left join fetch s.residenceDistrict rd left join fetch rd.region
             where c.academicYear.school.id = :schoolId
              and (:classId is null or c.id = :classId)
              and (:q is null
                   or lower(concat(s.firstName, ' ', s.lastName)) like :q
                   or lower(concat(s.lastName, ' ', s.firstName)) like :q)
              and (:riskOnly = false or s.id in :riskIds)
              and (:telegram is null
                   or (:telegram = true and exists (select 1 from ParentTelegramLink l where l.student = s and l.active = true))
                   or (:telegram = false and not exists (select 1 from ParentTelegramLink l where l.student = s and l.active = true)))
              and (:bMonth is null or (extract(month from s.birthDate) = :bMonth and extract(day from s.birthDate) = :bDay))
            """, countQuery = """
            select count(s) from Student s where s.schoolClass.academicYear.school.id = :schoolId
              and (:classId is null or s.schoolClass.id = :classId)
              and (:q is null
                   or lower(concat(s.firstName, ' ', s.lastName)) like :q
                   or lower(concat(s.lastName, ' ', s.firstName)) like :q)
              and (:riskOnly = false or s.id in :riskIds)
              and (:telegram is null
                   or (:telegram = true and exists (select 1 from ParentTelegramLink l where l.student = s and l.active = true))
                   or (:telegram = false and not exists (select 1 from ParentTelegramLink l where l.student = s and l.active = true)))
              and (:bMonth is null or (extract(month from s.birthDate) = :bMonth and extract(day from s.birthDate) = :bDay))
            """)
    Page<Student> search(@org.springframework.data.repository.query.Param("schoolId") Long schoolId,
                         @org.springframework.data.repository.query.Param("classId") Long classId,
                         @org.springframework.data.repository.query.Param("q") String q,
                         @org.springframework.data.repository.query.Param("riskOnly") boolean riskOnly,
                         @org.springframework.data.repository.query.Param("riskIds") java.util.Collection<Long> riskIds,
                         @org.springframework.data.repository.query.Param("telegram") Boolean telegram,
                         @org.springframework.data.repository.query.Param("bMonth") Integer birthMonth,
                         @org.springframework.data.repository.query.Param("bDay") Integer birthDay,
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

    java.util.Optional<Student> findByCardId(String cardId);
}
