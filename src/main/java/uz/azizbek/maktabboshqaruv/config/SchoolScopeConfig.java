package uz.azizbek.maktabboshqaruv.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Every staff request that names a school (?schoolId=…) is checked against the
 * caller's own school: asking for another school's lists is a 403. Requests for
 * one record by id are checked in the services that need it (appeals and files).
 */
@Configuration
public class SchoolScopeConfig implements WebMvcConfigurer {

    private final SchoolAccessService access;

    public SchoolScopeConfig(SchoolAccessService access) {
        this.access = access;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                String schoolId = request.getParameter("schoolId");
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (schoolId == null || auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
                    return true;
                }
                try {
                    access.requireSchool(access.caller(auth.getName()), Long.valueOf(schoolId));
                    return true;
                } catch (NumberFormatException e) {
                    return true; // the controller answers 400 for a malformed id
                } catch (ForbiddenException e) {
                    response.setStatus(403);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write(e.getMessage());
                    return false;
                }
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**", "/api/parent/**", "/api/health");
    }
}
