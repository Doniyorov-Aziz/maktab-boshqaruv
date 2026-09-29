package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.UserRequestDto;
import uz.azizbek.maktabboshqaruv.dto.UserResponseDto;
import uz.azizbek.maktabboshqaruv.dto.UserUpdateRequestDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toResponseDto);
    }

    public UserResponseDto getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday foydalanuvchi topilmadi: " + id));
        return toResponseDto(user);
    }

    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Bu username bilan foydalanuvchi allaqachon mavjud");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmployee(resolveEmployee(request.getEmployeeId()));

        User saved = userRepository.save(user);
        return toResponseDto(saved);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserUpdateRequestDto request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday foydalanuvchi topilmadi: " + id));

        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Bu username bilan foydalanuvchi allaqachon mavjud");
        }

        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setEmployee(resolveEmployee(request.getEmployeeId()));

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        return toResponseDto(updated);
    }

    private Employee resolveEmployee(Long employeeId) {
        if (employeeId == null) return null;
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim mavjud emas"));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday foydalanuvchi topilmadi: " + id));

        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        if (user.getUsername().equals(currentUsername)) {
            throw new IllegalStateException("O'zingizni o'chira olmaysiz");
        }

        userRepository.deleteById(id);
    }

    private UserResponseDto toResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        if (user.getEmployee() != null) {
            dto.setEmployeeId(user.getEmployee().getId());
            dto.setEmployeeName(user.getEmployee().getFirstName() + " " + user.getEmployee().getLastName());
        }
        return dto;
    }
}
