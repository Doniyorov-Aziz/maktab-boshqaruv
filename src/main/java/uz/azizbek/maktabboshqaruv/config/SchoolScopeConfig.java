package uz.azizbek.maktabboshqaruv.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import uz.azizbek.maktabboshqaruv.exception.ForbiddenException;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.SchoolOwnership;
import uz.azizbek.maktabboshqaruv.service.SchoolOwnership.Kind;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Map;

/**
 * Keeps every staff user inside their own school. A user whose employee belongs to a
 * school (the usual case) gets 403 for another school's data, however it is named:
 * <ul>
 *   <li>a record by id in the path — /api/students/12, /api/profiles/class/5, …;</li>
 *   <li>a query parameter — ?schoolId=, ?schoolClassId=, ?studentId=, …;</li>
 *   <li>the same fields in a JSON body (create / update).</li>
 * </ul>
 * Users without a school (the super admin) are not limited. Owners are looked up by
 * {@link SchoolOwnership} (cached), so the check costs no more than one small query.
 */
@Configuration
public class SchoolScopeConfig implements WebMvcConfigurer {

    /** Path segment (followed by a numeric id) → what the id names. */
    static final Map<String, Kind> PATH = Map.ofEntries(
            Map.entry("schools", Kind.SCHOOL), Map.entry("school-classes", Kind.CLASS), Map.entry("class", Kind.CLASS),
            Map.entry("students", Kind.STUDENT), Map.entry("student", Kind.STUDENT),
            Map.entry("employees", Kind.EMPLOYEE), Map.entry("teacher", Kind.EMPLOYEE),
            Map.entry("academic-years", Kind.YEAR), Map.entry("buildings", Kind.BUILDING), Map.entry("rooms", Kind.ROOM),
            Map.entry("subjects", Kind.SUBJECT), Map.entry("lesson-slots", Kind.LESSON_SLOT),
            Map.entry("announcements", Kind.ANNOUNCEMENT), Map.entry("calendar-events", Kind.CALENDAR_EVENT),
            Map.entry("absence-requests", Kind.ABSENCE), Map.entry("behavior-records", Kind.BEHAVIOR),
            Map.entry("grades", Kind.GRADE), Map.entry("attendance", Kind.ATTENDANCE), Map.entry("users", Kind.USER),
            Map.entry("appeals", Kind.APPEAL), Map.entry("broadcasts", Kind.BROADCAST)
    );

    /** Query parameter / body field → what it names. */
    static final Map<String, Kind> FIELDS = Map.ofEntries(
            Map.entry("schoolId", Kind.SCHOOL), Map.entry("schoolClassId", Kind.CLASS), Map.entry("classId", Kind.CLASS),
            Map.entry("studentId", Kind.STUDENT), Map.entry("employeeId", Kind.EMPLOYEE), Map.entry("teacherId", Kind.EMPLOYEE),
            Map.entry("classTeacherId", Kind.EMPLOYEE), Map.entry("academicYearId", Kind.YEAR),
            Map.entry("buildingId", Kind.BUILDING), Map.entry("roomId", Kind.ROOM), Map.entry("subjectId", Kind.SUBJECT),
            Map.entry("lessonSlotId", Kind.LESSON_SLOT)
    );

    private final SchoolAccessService access;
    private final SchoolOwnership ownership;

    public SchoolScopeConfig(SchoolAccessService access, SchoolOwnership ownership) {
        this.access = access;
        this.ownership = ownership;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                Long own = ownSchool();
                if (own == null) return true;
                try {
                    String[] parts = request.getRequestURI().split("/");
                    for (int i = 0; i + 1 < parts.length; i++) {
                        Kind kind = PATH.get(parts[i]);
                        if (kind != null && isId(parts[i + 1])) require(own, kind, Long.parseLong(parts[i + 1]));
                    }
                    for (Map.Entry<String, Kind> f : FIELDS.entrySet()) {
                        String value = request.getParameter(f.getKey());
                        if (value != null && isId(value)) require(own, f.getValue(), Long.parseLong(value));
                    }
                    return true;
                } catch (ForbiddenException e) {
                    response.setStatus(403);
                    response.setContentType("text/plain;charset=UTF-8");
                    response.getWriter().write(e.getMessage());
                    return false;
                }
            }
        }).addPathPatterns("/api/**").excludePathPatterns("/api/auth/**", "/api/parent/**", "/api/health");
    }

    /** The caller's school, or null when the caller is not limited (anonymous, or a user without a school). */
    Long ownSchool() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return null;
        try {
            return access.caller(auth.getName()).schoolId();
        } catch (ForbiddenException e) {
            return null;
        }
    }

    void require(Long own, Kind kind, long id) {
        ownership.schoolOf(kind, id).ifPresent(school -> {
            if (!school.equals(own)) throw new ForbiddenException("Boshqa maktab ma'lumotiga ruxsat yo'q");
        });
    }

    private static boolean isId(String s) {
        if (s.isEmpty() || s.length() > 18) return false;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return false;
        return true;
    }

    /** The same check for ids inside a JSON body (getters or record accessors named like the fields above). */
    @ControllerAdvice
    static class BodyScope extends RequestBodyAdviceAdapter {

        private final SchoolScopeConfig scope;

        BodyScope(SchoolScopeConfig scope) {
            this.scope = scope;
        }

        @Override
        public boolean supports(MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
            return true;
        }

        @Override
        public Object afterBodyRead(Object body, HttpInputMessage inputMessage, MethodParameter parameter, Type targetType,
                                    Class<? extends HttpMessageConverter<?>> converterType) {
            if (body == null || body instanceof String || body instanceof Map || body instanceof Iterable) return body;
            Long own = scope.ownSchool();
            if (own == null) return body;
            for (Map.Entry<String, Kind> f : FIELDS.entrySet()) {
                Object value = read(body, f.getKey());
                if (value instanceof Number n) scope.require(own, f.getValue(), n.longValue());
            }
            return body;
        }

        private static Object read(Object body, String field) {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            for (String name : new String[]{getter, field}) {
                try {
                    Method m = body.getClass().getMethod(name);
                    if (m.getParameterCount() == 0) return m.invoke(body);
                } catch (NoSuchMethodException ignored) {
                    // not this naming style
                } catch (ReflectiveOperationException e) {
                    return null;
                }
            }
            return null;
        }
    }
}
