package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.UserRequestDto;
import uz.azizbek.maktabboshqaruv.dto.UserResponseDto;
import uz.azizbek.maktabboshqaruv.dto.UserUpdateRequestDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
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

    @Autowired
    private SchoolAccessService access;

    /** A school's own admin sees that school's users only. */
    public Page<UserResponseDto> getAllUsers(Pageable pageable) {
        Long school = access.caller().schoolId();
        return (school == null ? userRepository.findAll(pageable) : userRepository.findByEmployeeSchoolId(school, pageable))
                .map(this::toResponseDto);
    }

    public UserResponseDto getUserById(Long id) {
        return toResponseDto(findVisible(id, access.caller().schoolId()));
    }

    @Transactional
    public UserResponseDto createUser(UserRequestDto request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Bu username bilan foydalanuvchi allaqachon mavjud");
        }

        Long school = access.caller().schoolId();
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setEmployee(resolveEmployee(request.getEmployeeId()));
        requireOwnSchool(school, user);

        User saved = userRepository.save(user);
        return toResponseDto(saved);
    }

    @Transactional
    public UserResponseDto updateUser(Long id, UserUpdateRequestDto request) {
        // the caller's school before any change: the caller may be editing their own account
        Long school = access.caller().schoolId();
        User user = findVisible(id, school);
        access.forget(user.getUsername());

        if (!user.getUsername().equals(request.getUsername()) && userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalStateException("Bu username bilan foydalanuvchi allaqachon mavjud");
        }

        user.setUsername(request.getUsername());
        user.setRole(request.getRole());
        user.setEmployee(resolveEmployee(request.getEmployeeId()));
        requireOwnSchool(school, user);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        User updated = userRepository.save(user);
        return toResponseDto(updated);
    }

    /** The user by id; a school's own admin gets 403 for a user outside that school (including platform accounts). */
    private User findVisible(Long id, Long school) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Bunday foydalanuvchi topilmadi: " + id));
        requireOwnSchool(school, user);
        return user;
    }

    /**
     * A school's own admin manages only that school's users, and every user they create or change
     * stays an employee of that school: a user without an employee would be a platform account.
     */
    private void requireOwnSchool(Long school, User user) {
        if (school == null) return;
        Employee e = user.getEmployee();
        if (e == null || e.getSchool() == null || !school.equals(e.getSchool().getId())) {
            throw new ForbiddenException("Faqat o'z maktabingiz xodimlari uchun foydalanuvchi boshqara olasiz");
        }
    }

    private Employee resolveEmployee(Long employeeId) {
        if (employeeId == null) return null;
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalStateException("Bunday xodim mavjud emas"));
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = findVisible(id, access.caller().schoolId());

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
