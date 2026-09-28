package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.EmployeeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.EmployeeResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.entity.School;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
import uz.azizbek.maktabboshqaruv.repository.SchoolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PositionRepository positionRepository;

    @Autowired
    private SchoolRepository schoolRepository;

    public Page<EmployeeResponseDto> getAllEmployees(Long schoolId, Pageable pageable) {
        return employeeRepository.findBySchoolId(schoolId, pageable)
                .map(this::toResponseDto);
    }

    public EmployeeResponseDto getEmployeeById(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim topilmadi: " + id));
        return toResponseDto(employee);
    }

    @Transactional
    public EmployeeResponseDto createEmployee(EmployeeRequestDto request) {
        if (employeeRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Bu telefon raqam bilan xodim allaqachon mavjud");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalStateException("Bunday lavozim mavjud emas"));

        Employee employee = new Employee();
        employee.setSchool(school);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setPosition(position);

        Employee saved = employeeRepository.save(employee);
        return toResponseDto(saved);
    }

    @Transactional
    public EmployeeResponseDto updateEmployee(Long id, EmployeeRequestDto request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim topilmadi: " + id));

        if (!employee.getPhone().equals(request.getPhone()) && employeeRepository.existsByPhone(request.getPhone())) {
            throw new IllegalStateException("Bu telefon raqam bilan xodim allaqachon mavjud");
        }

        School school = schoolRepository.findById(request.getSchoolId())
                .orElseThrow(() -> new IllegalStateException("Bunday maktab mavjud emas"));
        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalStateException("Bunday lavozim mavjud emas"));

        employee.setSchool(school);
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        employee.setPosition(position);

        Employee updated = employeeRepository.save(employee);
        return toResponseDto(updated);
    }

    @Transactional
    public void deleteEmployee(Long id) {
        if (!employeeRepository.existsById(id)) {
            throw new IllegalStateException("Bunday xodim topilmadi: " + id);
        }
        employeeRepository.deleteById(id);
    }

    private EmployeeResponseDto toResponseDto(Employee employee) {
        EmployeeResponseDto dto = new EmployeeResponseDto();
        dto.setId(employee.getId());
        dto.setFirstName(employee.getFirstName());
        dto.setLastName(employee.getLastName());
        dto.setFullName(employee.getFirstName() + " " + employee.getLastName());
        dto.setPhone(employee.getPhone());
        dto.setPositionId(employee.getPosition().getId());
        dto.setPositionTitle(employee.getPosition().getTitle());
        if (employee.getSchool() != null) {
            dto.setSchoolId(employee.getSchool().getId());
            dto.setSchoolName(employee.getSchool().getName());
        }
        return dto;
    }
}
