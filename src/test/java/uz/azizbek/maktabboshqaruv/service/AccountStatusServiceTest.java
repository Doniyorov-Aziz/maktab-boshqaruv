package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.EmployeeStatus;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Who may be blocked: never yourself, never the last admin who can still sign in. */
class AccountStatusServiceTest {

    private final UserRepository users = mock(UserRepository.class);
    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-07T06:00:00Z"), ZoneId.of("Asia/Tashkent"));
    private AccountStatusService service;

    private Employee adminEmployee;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new AccountStatusService(users, employees, clock);
        adminEmployee = new Employee();
        adminEmployee.setId(7L);
        admin = new User();
        admin.setUsername("direktor");
        admin.setRole(Role.ADMIN);
        admin.setEmployee(adminEmployee);
        when(users.findByEmployeeId(7L)).thenReturn(List.of(admin));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("boshqa_admin", null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void lastAdmin_cannotBeBlocked() {
        when(users.findByRole(Role.ADMIN)).thenReturn(List.of(admin));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.checkCanSetStatus(adminEmployee, EmployeeStatus.ON_LEAVE, null, null));
        assertTrue(e.getMessage().contains("Oxirgi faol administrator"));
    }

    @Test
    void adminWithAnotherActiveAdmin_canBeBlocked() {
        User other = new User();
        other.setUsername("boshqa_admin");
        other.setRole(Role.ADMIN);
        when(users.findByRole(Role.ADMIN)).thenReturn(List.of(admin, other));
        assertDoesNotThrow(() -> service.checkCanSetStatus(adminEmployee, EmployeeStatus.DISMISSED, null, null));
    }

    @Test
    void cannotBlockYourself() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("direktor", null, List.of()));
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> service.checkCanSetStatus(adminEmployee, EmployeeStatus.ON_LEAVE, null, null));
        assertTrue(e.getMessage().contains("O'zingizni"));
    }

    @Test
    void returningToWork_isNeverRefused() {
        assertDoesNotThrow(() -> service.checkCanSetStatus(adminEmployee, EmployeeStatus.ACTIVE, null, null));
        verify(users, never()).findByRole(any());
    }

    @Test
    void isUsable_isCachedForAMinute() {
        when(users.findByUsername("direktor")).thenReturn(Optional.of(admin));
        assertTrue(service.isUsable("direktor"));
        assertTrue(service.isUsable("direktor"));
        verify(users, times(1)).findByUsername("direktor");
    }
}
