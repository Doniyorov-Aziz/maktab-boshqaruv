package uz.azizbek.maktabboshqaruv.config;

import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.service.AccountStatusService;
import uz.azizbek.maktabboshqaruv.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AccountStatusService accountStatusService;

    @Value("${app.guest.login-enabled:false}")
    private boolean guestLoginEnabled;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            if (jwtUtil.isTokenValid(token)) {
                String username = jwtUtil.extractUsername(token);
                String role = jwtUtil.extractRole(token);

                // a demo visitor's token stops working as soon as the demo is switched off
                boolean guest = Role.GUEST.name().equals(role);
                if (guest && !guestLoginEnabled) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // a signed token is not enough: the account may have been put on leave since
                // (cached for a minute, so this is not a database query per request)
                if (!accountStatusService.isUsable(username)) {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setHeader("X-Account-Blocked", "1");
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write(AccountStatusService.BLOCKED_MESSAGE);
                    return;
                }

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                username,
                                null,
                                // the guest reads everything an ADMIN reads; GuestGuardFilter stops the rest
                                guest
                                        ? List.of(new SimpleGrantedAuthority(GuestPolicy.ROLE), new SimpleGrantedAuthority("ROLE_ADMIN"))
                                        : List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );

                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
