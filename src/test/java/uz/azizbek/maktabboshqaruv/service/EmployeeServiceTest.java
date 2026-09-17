package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.EmployeeRequestDto;
import uz.azizbek.maktabboshqaruv.dto.EmployeeResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.PositionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PositionRepository positionRepository;

    @InjectMocks
    private EmployeeService employeeService;

    private EmployeeRequestDto validRequest() {
        EmployeeRequestDto request = new EmployeeRequestDto();
        request.setFirstName("Aziz");
        request.setLastName("Karimov");
        request.setPhone("+998901234567");
        request.setPositionId(1L);
        return request;
    }

    @Test
    void createEmployee_duplicatePhone_throws() {
        EmployeeRequestDto request = validRequest();
        when(employeeRepository.existsByPhone(request.getPhone())).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> employeeService.createEmployee(request));
        verify(positionRepository, never()).findById(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_unknownPosition_throws() {
        EmployeeRequestDto request = validRequest();
        when(employeeRepository.existsByPhone(request.getPhone())).thenReturn(false);
        when(positionRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> employeeService.createEmployee(request));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void createEmployee_valid_saves() {
        EmployeeRequestDto request = validRequest();
        when(employeeRepository.existsByPhone(request.getPhone())).thenReturn(false);

        Position position = new Position();
        position.setId(1L);
        position.setTitle("O'qituvchi");
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));

        Employee saved = new Employee();
        saved.setId(1L);
        saved.setFirstName("Aziz");
        saved.setLastName("Karimov");
        saved.setPhone(request.getPhone());
        saved.setPosition(position);
        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        EmployeeResponseDto result = employeeService.createEmployee(request);

        assertEquals("Aziz Karimov", result.getFullName());
        assertEquals("O'qituvchi", result.getPositionTitle());
    }

    @Test
    void updateEmployee_samePhone_skipsDuplicateCheck() {
        Position position = new Position();
        position.setId(1L);
        position.setTitle("O'qituvchi");

        Employee existing = new Employee();
        existing.setId(1L);
        existing.setPhone("+998901234567");
        existing.setPosition(position);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(positionRepository.findById(1L)).thenReturn(Optional.of(position));
        when(employeeRepository.save(any(Employee.class))).thenReturn(existing);

        employeeService.updateEmployee(1L, validRequest());

        verify(employeeRepository, never()).existsByPhone(any());
    }

    @Test
    void updateEmployee_changedToDuplicatePhone_throws() {
        Position position = new Position();
        position.setId(1L);

        Employee existing = new Employee();
        existing.setId(1L);
        existing.setPhone("+998900000000");
        existing.setPosition(position);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(employeeRepository.existsByPhone("+998901234567")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> employeeService.updateEmployee(1L, validRequest()));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void deleteEmployee_notFound_throws() {
        when(employeeRepository.existsById(1L)).thenReturn(false);

        assertThrows(IllegalStateException.class, () -> employeeService.deleteEmployee(1L));
    }
}
