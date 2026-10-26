package uz.azizbek.maktabboshqaruv.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * The one shared account behind every demo visitor ("mehmon", role GUEST, no employee — so it
 * sees every school, like the platform administrator). Its password is random and never told to
 * anyone: the account is entered only through /api/auth/guest-login.
 */
@Service
public class GuestAccountService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final String username;

    public GuestAccountService(UserRepository users, PasswordEncoder encoder,
                               @Value("${app.guest.username:mehmon}") String username) {
        this.users = users;
        this.encoder = encoder;
        this.username = username;
    }

    @Transactional
    public synchronized User ensure() {
        return users.findByUsername(username).map(u -> {
            if (u.getRole() != Role.GUEST) {
                throw new IllegalStateException("'" + username + "' foydalanuvchisi allaqachon bor va u mehmon emas");
            }
            return u;
        }).orElseGet(() -> {
            byte[] random = new byte[32];
            new SecureRandom().nextBytes(random);
            User u = new User();
            u.setUsername(username);
            u.setPassword(encoder.encode(Base64.getEncoder().encodeToString(random)));
            u.setRole(Role.GUEST);
            return users.save(u);
        });
    }
}
