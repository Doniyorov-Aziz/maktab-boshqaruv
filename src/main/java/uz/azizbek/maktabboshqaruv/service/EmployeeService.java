package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.EmployeeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.EmployeeResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
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

    public Page<EmployeeResponseDto> getAllEmployees(Pageable pageable) {
        return employeeRepository.findAll(pageable)
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

        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalStateException("Bunday lavozim mavjud emas"));

        Employee employee = new Employee();
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

        Position position = positionRepository.findById(request.getPositionId())
                .orElseThrow(() -> new IllegalStateException("Bunday lavozim mavjud emas"));

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
        return dto;
    }
}