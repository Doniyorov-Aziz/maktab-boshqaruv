package uz.azizbek.maktabboshqaruv.repository;

import uz.azizbek.maktabboshqaruv.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByPhone(String phone);
    Page<Employee> findBySchoolId(Long schoolId, Pageable pageable);
    List<Employee> findBySchoolId(Long schoolId);
    long countBySchoolId(Long schoolId);
}
