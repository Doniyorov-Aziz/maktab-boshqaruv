package uz.azizbek.maktabboshqaruv.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uz.azizbek.maktabboshqaruv.entity.AcademicTerm;

import java.util.Collection;
import java.util.List;

public interface AcademicTermRepository extends JpaRepository<AcademicTerm, Long> {

    List<AcademicTerm> findByAcademicYearIdOrderByStartDateAsc(Long academicYearId);

    /** The terms of several years in one query (the year cards). */
    @Query("select t from AcademicTerm t where t.academicYear.id in :yearIds order by t.startDate")
    List<AcademicTerm> findByYears(@Param("yearIds") Collection<Long> yearIds);

    @Modifying
    @Query("delete from AcademicTerm t where t.academicYear.id = :yearId")
    void deleteByYear(@Param("yearId") Long yearId);
}
