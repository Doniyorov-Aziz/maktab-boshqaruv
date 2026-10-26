package uz.azizbek.maktabboshqaruv.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Runs right after {@link JwtAuthFilter}: a GUEST request that {@link GuestPolicy} does not
 * allow ends here with 403 {"message": …} — before method security, controllers or services.
 */
@Component
public class GuestGuardFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (GuestPolicy.isGuest(SecurityContextHolder.getContext().getAuthentication())) {
            String path = request.getRequestURI().substring(request.getContextPath().length());
            GuestPolicy.Denial denial = GuestPolicy.check(request.getMethod(), path);
            if (denial != null) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"" + denial.message.replace("\"", "\\\"")
                        + "\",\"reason\":\"" + denial.name() + "\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
