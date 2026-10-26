package uz.azizbek.maktabboshqaruv.controller;

import jakarta.servlet.http.HttpServletRequest;
import uz.azizbek.maktabboshqaruv.dto.LoginRequest;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.service.AccountStatusService;
import uz.azizbek.maktabboshqaruv.service.DemoVisitService;
import uz.azizbek.maktabboshqaruv.service.GuestAccountService;
import uz.azizbek.maktabboshqaruv.service.RateLimiter;
import uz.azizbek.maktabboshqaruv.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final long GUEST_TOKEN_MS = Duration.ofHours(8).toMillis();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountStatusService accountStatusService;

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private GuestAccountService guestAccounts;

    @Autowired
    private DemoVisitService demoVisits;

    @Value("${app.guest.login-enabled:false}")
    private boolean guestLoginEnabled;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        if (!rateLimiter.tryAcquire("login:" + http.getRemoteAddr(), 10, Duration.ofMinutes(5))) {
            return ResponseEntity.status(429).body(RateLimiter.MESSAGE);
        }
        User user = request.getUsername() == null ? null : userRepository.findByUsername(request.getUsername()).orElse(null);

        // the demo account has no password anyone knows; it is entered only through /guest-login
        if (user == null || user.getRole() == Role.GUEST || request.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Login yoki parol noto'g'ri");
        }
        // checked only after the password, so the 403 never reveals that a username exists
        if (accountStatusService.isBlocked(user)) {
            return ResponseEntity.status(403).body(AccountStatusService.BLOCKED_MESSAGE);
        }
        // staff opening the demo from this browser later are not counted as visitors
        if (request.getVisitorId() != null) {
            demoVisits.exclude(request.getVisitorId(), user.getUsername());
        }

        Long employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), employeeId);
        return ResponseEntity.ok(token);
    }

    /**
     * The public demo link (#/demo): anyone becomes the read-only "mehmon" for 8 hours, without a
     * password. 404 unless GUEST_LOGIN_ENABLED=true.
     */
    @PostMapping("/guest-login")
    public ResponseEntity<?> guestLogin(HttpServletRequest http) {
        if (!guestLoginEnabled) {
            return ResponseEntity.status(404).body(Map.of("message", "Manzil topilmadi"));
        }
        if (!rateLimiter.tryAcquire("guest:" + http.getRemoteAddr(), 30, Duration.ofHours(1))) {
            return ResponseEntity.status(429).body(RateLimiter.MESSAGE);
        }
        User guest = guestAccounts.ensure();
        return ResponseEntity.ok(jwtUtil.generateToken(guest.getUsername(), Role.GUEST.name(), null, GUEST_TOKEN_MS));
    }

    /** For the login page's "Demo sifatida ko'rish" button. */
    @GetMapping("/guest-enabled")
    public Map<String, Boolean> guestEnabled() {
        return Map.of("enabled", guestLoginEnabled);
    }

    /** Tokens are stateless; the browser forgets its own. Here so every role (the guest too) has a sign-out call. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/hash/{raw}")
    public String hashPassword(@PathVariable String raw) {
        return passwordEncoder.encode(raw);
    }
}
