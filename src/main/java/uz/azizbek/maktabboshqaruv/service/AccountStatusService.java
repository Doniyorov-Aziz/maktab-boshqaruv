package uz.azizbek.maktabboshqaruv.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.EmployeeStatus;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.EmployeeRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Whether a staff account may be used right now. A user linked to an employee who is
 * on leave or dismissed cannot sign in (403), and an already issued token stops working
 * on the next request (401). The answer is cached for a minute per username, so the JWT
 * filter does not hit the database on every request; a status change clears the cache.
 */
@Service
public class AccountStatusService {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusService.class);
    public static final String BLOCKED_MESSAGE =
            "Hisobingiz vaqtincha o'chirilgan (ta'til). Administratorga murojaat qiling.";

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;
    private final Cache<String, Boolean> usable = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .maximumSize(10_000)
            .build();

    public AccountStatusService(UserRepository userRepository, EmployeeRepository employeeRepository, Clock clock) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.clock = clock;
    }

    public boolean isBlocked(User user) {
        Employee e = user.getEmployee();
        return e != null && e.isAwayOn(LocalDate.now(clock));
    }

    /** For the JWT filter: false when the user is gone or blocked. Cached for one minute. */
    public boolean isUsable(String username) {
        return usable.get(username, name -> userRepository.findByUsername(name)
                .map(u -> !isBlocked(u))
                .orElse(false));
    }

    public void evictAll() {
        usable.invalidateAll();
    }

    /**
     * Guards a status change of an employee: an admin cannot block themselves, and the
     * last ADMIN who can still sign in cannot be blocked.
     */
    public void checkCanSetStatus(Employee employee, EmployeeStatus newStatus, LocalDate leaveFrom, LocalDate leaveTo) {
        Employee probe = new Employee();
        probe.setStatus(newStatus);
        probe.setLeaveFrom(leaveFrom);
        probe.setLeaveTo(leaveTo);
        if (employee.getId() == null || !probe.isAwayOn(LocalDate.now(clock))) return;

        List<User> linked = userRepository.findByEmployeeId(employee.getId());
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String me = auth == null ? null : auth.getName();
        if (me != null && linked.stream().anyMatch(u -> u.getUsername().equals(me))) {
            throw new IllegalStateException("O'zingizni bloklay olmaysiz");
        }
        if (linked.stream().anyMatch(u -> u.getRole() == Role.ADMIN)) {
            long otherUsableAdmins = userRepository.findByRole(Role.ADMIN).stream()
                    .filter(u -> u.getEmployee() == null || !u.getEmployee().getId().equals(employee.getId()))
                    .filter(u -> !isBlocked(u))
                    .count();
            if (otherUsableAdmins == 0) {
                throw new IllegalStateException("Oxirgi faol administratorni bloklab bo'lmaydi");
            }
        }
    }

    /** Every morning (Tashkent): leaves that ended yesterday or earlier go back to "Ishda". */
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Tashkent")
    @Transactional
    public int returnFromLeave() {
        LocalDate today = LocalDate.now(clock);
        List<Employee> back = employeeRepository.findByStatusAndLeaveToBefore(EmployeeStatus.ON_LEAVE, today);
        for (Employee e : back) {
            e.setStatus(EmployeeStatus.ACTIVE);
            e.setLeaveFrom(null);
            e.setLeaveTo(null);
        }
        if (!back.isEmpty()) {
            employeeRepository.saveAll(back);
            evictAll();
            log.info("Ta'tildan qaytdi: {} ta xodim", back.size());
        }
        return back.size();
    }
}
