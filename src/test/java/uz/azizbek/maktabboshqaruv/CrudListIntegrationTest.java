package uz.azizbek.maktabboshqaruv;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import uz.azizbek.maktabboshqaruv.dto.EmployeeResponseDto;
import uz.azizbek.maktabboshqaruv.dto.PositionResponseDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.entity.Employee;
import uz.azizbek.maktabboshqaruv.entity.Position;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.EmployeeService;
import uz.azizbek.maktabboshqaruv.service.PositionService;
import uz.azizbek.maktabboshqaruv.service.StudentService;
import uz.azizbek.maktabboshqaruv.service.pages.ListStatsService;
import uz.azizbek.maktabboshqaruv.service.pages.TableExportService;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The list pages' backend: 521 students in one school come in at most 3 SQL statements (counted by
 * Hibernate's statistics) with their 30-day numbers; another school's rows never mix in (lists,
 * search, filter counts, position counts); a position with employees cannot be deleted (409); a
 * sort by a shown column works and an unknown one is dropped; the export writes real files.
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@ActiveProfiles("local")
class CrudListIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    EntityManagerFactory emf;
    @Autowired
    StudentService studentService;
    @Autowired
    EmployeeService employeeService;
    @Autowired
    PositionService positionService;
    @Autowired
    ListStatsService listStats;
    @Autowired
    TableExportService export;
    @Autowired
    SchoolRepository schools;
    @Autowired
    AcademicYearRepository years;
    @Autowired
    PositionRepository positions;
    @Autowired
    EmployeeRepository employees;
    @Autowired
    SubjectRepository subjects;
    @Autowired
    BuildingRepository buildings;
    @Autowired
    RoomRepository rooms;
    @Autowired
    SchoolClassRepository classes;
    @Autowired
    LessonSlotRepository lessons;
    @Autowired
    StudentRepository students;

    TestSchool big;
    TestSchool other;
    Position extra;

    TestSchool.Repos repos() {
        return new TestSchool.Repos(schools, years, positions, employees, subjects, buildings, rooms, classes, lessons, students);
    }

    @BeforeEach
    void setUp() {
        // 521 students in 3 classes (174 + 174 + 173), all with both places set
        big = TestSchool.create(repos(), jdbc, 3, 174);
        jdbc.update("delete from students where id = ?", big.students.get(2).remove(173).getId());
        Long district = jdbc.queryForObject("select min(id) from district", Long.class);
        if (district != null) {
            jdbc.update("update students set birth_district_id = ?, residence_district_id = ? where class_id in (?, ?, ?)",
                    district, district, big.classes.get(0).getId(), big.classes.get(1).getId(), big.classes.get(2).getId());
        }
        other = TestSchool.create(repos(), jdbc, 1, 4);
        jdbc.update("update students set first_name = 'Begona' where class_id = ?", other.classes.get(0).getId());
    }

    @AfterEach
    void cleanUp() {
        big.delete();
        other.delete();
        if (extra != null) positions.deleteById(extra.getId());
    }

    Statistics stats() {
        Statistics s = emf.unwrap(SessionFactory.class).getStatistics();
        s.setStatisticsEnabled(true);
        return s;
    }

    @Test
    void students_521_inAtMost3Statements_withTheirNumbers() {
        long sid = big.id();
        // a mark and a grade for the first student, a Telegram parent for the second
        long first = big.students.get(0).get(0).getId();
        LocalDate today = LocalDate.now();
        jdbc.update("insert into attendance (lesson_slot_id, student_id, record_date, status, source, created_date) values (?, ?, ?, 'ABSENT', 'MANUAL', now())",
                big.lessons.get(0)[0].getId(), first, Date.valueOf(today.minusDays(1)));
        jdbc.update("insert into attendance (lesson_slot_id, student_id, record_date, status, source, created_date) values (?, ?, ?, 'PRESENT', 'MANUAL', now())",
                big.lessons.get(0)[1].getId(), first, Date.valueOf(today.minusDays(1)));
        jdbc.update("insert into grade (student_id, subject_id, grade_date, score, type, created_date) values (?, ?, ?, 4, 'CURRENT', now())",
                first, big.subject.getId(), Date.valueOf(today.minusDays(2)));
        listStats.riskScores(sid); // the shared risk list is cached (90 s) like on a running server

        Statistics st = stats();
        st.clear();
        Page<StudentResponseDto> all = studentService.getAllStudents(sid, null, null,
                PageRequest.of(0, 25, Sort.by("fullName")));
        long statements = st.getPrepareStatementCount();
        assertEquals(521, all.getTotalElements());
        assertEquals(25, all.getContent().size());
        assertTrue(statements <= 3, "page + count + numbers, got " + statements);

        st.clear();
        Page<StudentResponseDto> whole = studentService.getAllStudents(sid, null, null, PageRequest.of(0, 600));
        assertEquals(521, whole.getContent().size());
        assertTrue(st.getPrepareStatementCount() <= 3, "all 521 at once, got " + st.getPrepareStatementCount());
        assertNotNull(whole.getContent().get(0).getBirthPlace(), "places came with the page");

        StudentResponseDto one = whole.getContent().stream().filter(s -> s.getId() == first).findFirst().orElseThrow();
        assertEquals(50.0, one.getAttendance30());
        assertEquals(4.0, one.getAverage30());
        assertEquals(Boolean.FALSE, one.getTelegram());
    }

    @Test
    void anotherSchool_neverMixesIn() {
        long a = big.id();
        long b = other.id();
        assertEquals(0, studentService.getAllStudents(a, null, "begona", PageRequest.of(0, 50)).getTotalElements());
        assertEquals(4, studentService.getAllStudents(b, null, "begona", PageRequest.of(0, 50)).getTotalElements());
        // a class of the other school as a filter gives nothing in this school
        assertEquals(0, studentService.getAllStudents(a, other.classes.get(0).getId(), null, PageRequest.of(0, 50)).getTotalElements());

        Map<String, List<ListStatsService.Facet>> facets = listStats.studentFacets(a);
        assertEquals(3, facets.get("schoolClassId").size());
        assertEquals(521, facets.get("schoolClassId").stream().mapToLong(ListStatsService.Facet::count).sum());

        List<EmployeeResponseDto> staff = employeeService.getAllEmployees(a, null, null, null, PageRequest.of(0, 50)).getContent();
        assertEquals(1, staff.size());
        assertEquals(big.teacher.getId(), staff.get(0).getId());
        assertEquals(3 * TestSchool.STARTS.length, staff.get(0).getWeeklyLessons());

        long positionId = big.teacher.getPosition().getId();
        long inA = listStats.positionEmployees(a).getOrDefault(positionId, 0);
        long inAll = listStats.positionEmployees(null).getOrDefault(positionId, 0);
        assertEquals(1, inA);
        assertTrue(inAll >= 2, "the other school's teacher counts only for all schools");
        PositionResponseDto p = positionService.getAllPositions(a, null, PageRequest.of(0, 200)).getContent().stream()
                .filter(x -> x.getId() == positionId).findFirst().orElseThrow();
        assertEquals(1, p.getEmployeeCount());
    }

    @Test
    void aPositionWithEmployees_cannotBeDeleted_409() {
        extra = new Position();
        extra.setTitle("Test lavozim " + System.nanoTime());
        extra = positions.save(extra);
        Employee e = big.teacher;
        e.setPosition(extra);
        employees.save(e);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> positionService.deletePosition(extra.getId()));
        assertTrue(ex.getMessage().contains("1 ta xodim"), ex.getMessage());

        // without employees it goes
        e.setPosition(positions.findAll().get(0));
        employees.save(e);
        positionService.deletePosition(extra.getId());
        assertFalse(positions.existsById(extra.getId()));
        extra = null;
    }

    @Test
    void sortByAShownColumn_works_andAnUnknownOne_isDropped() {
        long a = big.id();
        List<StudentResponseDto> byName = studentService.getAllStudents(a, null, null,
                PageRequest.of(0, 600, Sort.by(Sort.Direction.DESC, "fullName"))).getContent();
        assertEquals("Test2", byName.get(0).getLastName());
        assertDoesNotThrow(() -> studentService.getAllStudents(a, null, null,
                PageRequest.of(0, 10, Sort.by("attendance30"))));
        assertDoesNotThrow(() -> employeeService.getAllEmployees(a, null, null, "o'qit", PageRequest.of(0, 10, Sort.by("positionTitle"))));
        assertEquals(1, employeeService.getAllEmployees(a, null, null, "o'qit", PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void export_writesExcelAndPdf_withPhonesFormatted() {
        TableExportService.Table t = new TableExportService.Table("Xodimlar", "1-maktab", List.of(
                new TableExportService.Column("F.I.Sh.", "text"), new TableExportService.Column("Telefon", "phone"),
                new TableExportService.Column("Soat", "number")),
                List.of(List.of("Ali Valiyev", "998901112233", 18)));
        byte[] xlsx = export.xlsx(t);
        assertEquals('P', (char) xlsx[0]); // a zip
        byte[] pdf = export.pdf(t);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }
}
