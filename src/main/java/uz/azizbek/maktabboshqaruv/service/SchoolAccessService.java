package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.repository.SchoolClassRepository;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Which school a signed-in user may work with. A user linked to an employee belongs to
 * that employee's school and sees nothing of other schools (403). An account without an
 * employee link is a platform account (it manages schools) and may switch between them.
 */
@Service
public class SchoolAccessService {

    /** Who is asking: username, role, own school (null = every school), own employee. */
    public record Caller(String username, Role role, Long schoolId, Long employeeId) {
        public boolean isAdmin() {
            return role == Role.ADMIN;
        }
    }

    private final UserRepository users;
    private final SchoolClassRepository classes;

    public SchoolAccessService(UserRepository users, SchoolClassRepository classes) {
        this.users = users;
        this.classes = classes;
    }

    @Transactional(readOnly = true)
    public Caller caller() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) throw new ForbiddenException("Avval tizimga kiring");
        return caller(auth.getName());
    }

    /** Every request asks who the caller is; a minute is short enough for role or school changes. */
    private final com.github.benmanes.caffeine.cache.Cache<String, Caller> callers =
            com.github.benmanes.caffeine.cache.Caffeine.newBuilder()
                    .maximumSize(5_000).expireAfterWrite(java.time.Duration.ofMinutes(1)).build();

    @Transactional(readOnly = true)
    public Caller caller(String username) {
        Caller cached = callers.getIfPresent(username);
        if (cached != null) return cached;
        User u = users.findByUsername(username).orElseThrow(() -> new ForbiddenException("Foydalanuvchi topilmadi"));
        Long schoolId = u.getEmployee() != null && u.getEmployee().getSchool() != null
                ? u.getEmployee().getSchool().getId() : null;
        Caller c = new Caller(u.getUsername(), u.getRole(), schoolId, u.getEmployee() != null ? u.getEmployee().getId() : null);
        callers.put(username, c);
        return c;
    }

    /** After a user's role or employee changes. */
    public void forget(String username) {
        callers.invalidate(username);
    }

    /** 403 unless the caller may work with this school. */
    public void requireSchool(Caller caller, Long schoolId) {
        if (schoolId == null) return;
        if (caller.schoolId() != null && !caller.schoolId().equals(schoolId)) {
            throw new ForbiddenException("Boshqa maktab ma'lumotiga ruxsat yo'q");
        }
    }

    public void requireSchool(Long schoolId) {
        requireSchool(caller(), schoolId);
    }

    /** Classes this caller leads as class teacher (their appeals). */
    @Transactional(readOnly = true)
    public List<Long> classesLed(Caller caller) {
        return caller.employeeId() == null ? List.of() : classes.classIdsLedBy(caller.employeeId());
    }
}
