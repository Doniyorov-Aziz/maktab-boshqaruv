package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /** Employees list with the optional "Lavozim" and "Holat" filters (a null status column counts as ACTIVE). */
    @org.springframework.data.jpa.repository.Query("""
            select e from Employee e where e.school.id = :schoolId
              and (:positionId is null or e.position.id = :positionId)
              and (:status is null or e.status = :status
                   or (e.status is null and :status = uz.azizbek.maktabboshqaruv.entity.EmployeeStatus.ACTIVE))
            """)
    Page<Employee> search(@org.springframework.data.repository.query.Param("schoolId") Long schoolId,
                          @org.springframework.data.repository.query.Param("positionId") Long positionId,
                          @org.springframework.data.repository.query.Param("status") uz.azizbek.maktabboshqaruv.entity.EmployeeStatus status,
                          Pageable pageable);
    boolean existsByPhone(String phone);
    Page<Employee> findBySchoolId(Long schoolId, Pageable pageable);
    List<Employee> findBySchoolId(Long schoolId);
    long countBySchoolId(Long schoolId);
    long countBySchoolIdAndPositionTitleIn(Long schoolId, List<String> titles);
    List<Employee> findBySchoolIdAndPositionTitleIn(Long schoolId, List<String> titles);
    List<Employee> findByStatusAndLeaveToBefore(uz.azizbek.maktabboshqaruv.entity.EmployeeStatus status, java.time.LocalDate day);
}
