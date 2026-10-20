package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;
import uz.azizbek.maktabboshqaruv.controller.AcademicYearController;
import uz.azizbek.maktabboshqaruv.controller.EmployeeController;
import uz.azizbek.maktabboshqaruv.controller.PositionController;
import uz.azizbek.maktabboshqaruv.controller.SchoolClassController;
import uz.azizbek.maktabboshqaruv.controller.SchoolController;
import uz.azizbek.maktabboshqaruv.controller.StudentController;
import uz.azizbek.maktabboshqaruv.controller.SubjectController;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Not a check: writes the WHOLE lists of the CRUD pages (O'quv yillari, Fanlar, O'quvchilar,
 * Lavozimlar, Xodimlar and what their forms and filters read) for school 1 of the TEST database
 * into one JSON file, so the UI tests can show the pages with real rows and no sign-in
 * (tests/ui/crud-mock.js pages, sorts, searches and filters them like the server). Also the
 * facet counts and the first records' details when the backend has them. Refuses any database
 * but maktab_tg_test. Runs only when CRUD_FIXTURES names the file:
 *   DB_URL=jdbc:postgresql://localhost:5432/maktab_tg_test CRUD_FIXTURES=/path/crud.json \
 *     ./gradlew test --tests '*CrudFixtureDump'
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
@EnabledIfEnvironmentVariable(named = "CRUD_FIXTURES", matches = ".+")
class CrudFixtureDump {

    static final long SCHOOL = 1;
    static final PageRequest ALL = PageRequest.of(0, 5000, Sort.by("id"));

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    ObjectMapper mapper;
    @Autowired
    SchoolController schools;
    @Autowired
    SchoolClassController classes;
    @Autowired
    AcademicYearController years;
    @Autowired
    SubjectController subjects;
    @Autowired
    StudentController students;
    @Autowired
    PositionController positions;
    @Autowired
    EmployeeController employees;

    static Map<String, Object> page(Page<?> p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("content", p.getContent());
        m.put("totalElements", p.getTotalElements());
        m.put("totalPages", p.getTotalPages());
        return m;
    }

    /** A controller method that may not exist yet (the "before" dump runs on the old backend). */
    static Object optional(Object controller, String name, Object... args) {
        for (Method m : controller.getClass().getMethods()) {
            if (m.getName().equals(name) && m.getParameterCount() == args.length) {
                try {
                    return m.invoke(controller, args);
                } catch (Exception e) {
                    throw new IllegalStateException(name, e);
                }
            }
        }
        return null;
    }

    @Test
    void dump() throws Exception {
        String db = jdbc.queryForObject("select current_database()", String.class);
        if (!"maktab_tg_test".equals(db)) {
            throw new IllegalStateException("Faqat maktab_tg_test da ishlaydi (DB_URL), hozir: " + db);
        }
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("today", LocalDate.now().toString());

        out.put("/api/schools", page(schools.getAllSchools(ALL)));
        out.put("/api/school-classes", page(classes.getAllSchoolClasses(SCHOOL, PageRequest.of(0, 500, Sort.by("gradeNumber")))));
        out.put("/api/academic-years", page(years.getAllAcademicYears(SCHOOL, null, ALL)));
        out.put("/api/positions", page(positions.getAllPositions(SCHOOL, null, ALL)));
        out.put("/api/subjects", page(subjects.getAllSubjects(SCHOOL, true, null, null, ALL)));
        out.put("/api/students", page(students.getAllStudents(SCHOOL, null, null, false, null, false, ALL)));
        out.put("/api/employees", page(employees.getAllEmployees(SCHOOL, null, null, null, false, ALL)));

        Object f;
        if ((f = optional(students, "facets", SCHOOL)) != null) out.put("/api/students/facets", f);
        if ((f = optional(employees, "facets", SCHOOL)) != null) out.put("/api/employees/facets", f);
        if ((f = optional(subjects, "facets", SCHOOL)) != null) out.put("/api/subjects/facets", f);

        Path file = Path.of(System.getenv("CRUD_FIXTURES"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(out));
    }
}
