package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.LoginRequest;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import uz.azizbek.maktabboshqaruv.service.AccountStatusService;
import uz.azizbek.maktabboshqaruv.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountStatusService accountStatusService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body("Login yoki parol noto'g'ri");
        }
        // checked only after the password, so the 403 never reveals that a username exists
        if (accountStatusService.isBlocked(user)) {
            return ResponseEntity.status(403).body(AccountStatusService.BLOCKED_MESSAGE);
        }

        Long employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name(), employeeId);
        return ResponseEntity.ok(token);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/hash/{raw}")
    public String hashPassword(@PathVariable String raw) {
        return passwordEncoder.encode(raw);
    }
}