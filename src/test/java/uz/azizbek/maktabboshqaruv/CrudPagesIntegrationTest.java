package uz.azizbek.maktabboshqaruv;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearRequestDto;
import uz.azizbek.maktabboshqaruv.dto.AcademicYearResponseDto;
import uz.azizbek.maktabboshqaruv.dto.PositionRequestDto;
import uz.azizbek.maktabboshqaruv.dto.StudentResponseDto;
import uz.azizbek.maktabboshqaruv.repository.*;
import uz.azizbek.maktabboshqaruv.service.AcademicYearService;
import uz.azizbek.maktabboshqaruv.service.EmployeeService;
import uz.azizbek.maktabboshqaruv.service.PositionService;
import uz.azizbek.maktabboshqaruv.service.StudentService;
import uz.azizbek.maktabboshqaruv.service.SubjectService;
import uz.azizbek.maktabboshqaruv.service.pages.ListPagesService;
import uz.azizbek.maktabboshqaruv.support.TestSchool;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The five pages' own rules: school years never overlap (409) and only one is current, terms stay
 * inside their year and apart, the next year is proposed one year on; the students' chips (at
 * risk, Telegram, born today), "Sinf rahbarlari", the Faol / Nofaol tabs, the bulk move (never into
 * another school), a position's default role, and the side-panel extras on real rows.
 */
@SpringBootTest(properties = {
        "telegram.mock=true",
        "telegram.send-initial-delay-ms=3600000",
        "telegram.jobs-initial-delay-ms=3600000",
        "app.demo.enabled=false"
})
@ActiveProfiles("local")
class CrudPagesIntegrationTest {

    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    AcademicYearService yearService;
    @Autowired
    StudentService studentService;
    @Autowired
    EmployeeService employeeService;
    @Autowired
    SubjectService subjectService;
    @Autowired
    PositionService positionService;
    @Autowired
    ListPagesService pages;
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

    TestSchool school;
    TestSchool other;

    TestSchool.Repos repos() {
        return new TestSchool.Repos(schools, years, positions, employees, subjects, buildings, rooms, classes, lessons, students);
    }

    @BeforeEach
    void setUp() {
        school = TestSchool.create(repos(), jdbc, 2, 5);
        other = TestSchool.create(repos(), jdbc, 1, 2);
    }

    @AfterEach
    void cleanUp() {
        school.delete();
        other.delete();
    }

    long yearId() {
        return school.classes.get(0).getAcademicYear().getId();
    }

    AcademicYearRequestDto year(String title, LocalDate start, LocalDate end) {
        AcademicYearRequestDto r = new AcademicYearRequestDto();
        r.setSchoolId(school.id());
        r.setTitle(title);
        r.setStartDate(start);
        r.setEndDate(end);
        return r;
    }

    // ---------------------------------------------------------------- O'quv yillari

    @Test
    void years_neverOverlap_409_andOnlyOneIsCurrent() {
        // the test school's year is 2030-09-01 … 2031-06-30
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> yearService.createAcademicYear(year("Ustma-ust", LocalDate.of(2031, 6, 1), LocalDate.of(2032, 5, 31))));
        assertTrue(e.getMessage().contains("ustma-ust"), e.getMessage());
        AcademicYearResponseDto next = yearService.createAcademicYear(
                year("2031-2032", LocalDate.of(2031, 9, 2), LocalDate.of(2032, 5, 25)));
        // moving it into the first one is refused too
        assertThrows(IllegalStateException.class, () -> yearService.updateAcademicYear(next.getId(),
                year("2031-2032", LocalDate.of(2031, 6, 30), LocalDate.of(2032, 5, 25))));
        // another school's dates do not matter
        AcademicYearRequestDto otherYear = year("Boshqa", LocalDate.of(2031, 9, 2), LocalDate.of(2032, 5, 25));
        otherYear.setSchoolId(other.id());
        assertDoesNotThrow(() -> yearService.createAcademicYear(otherYear));

        yearService.makeCurrent(yearId());
        yearService.makeCurrent(next.getId());
        Integer current = jdbc.queryForObject("select count(*) from academic_year where school_id = ? and is_current",
                Integer.class, school.id());
        assertEquals(1, current);
        assertTrue(yearService.getAcademicYearById(next.getId()).isCurrent());
        assertFalse(yearService.getAcademicYearById(yearId()).isCurrent());
    }

    @Test
    void terms_stayInsideTheirYear_andApart() {
        long y = yearId();
        var q1 = new AcademicYearService.Term(null, "1-chorak", "QUARTER", LocalDate.of(2030, 9, 2), LocalDate.of(2030, 11, 2));
        var v1 = new AcademicYearService.Term(null, "Kuzgi ta'til", "VACATION", LocalDate.of(2030, 11, 3), LocalDate.of(2030, 11, 9));
        var q2 = new AcademicYearService.Term(null, "2-chorak", "QUARTER", LocalDate.of(2030, 11, 10), LocalDate.of(2030, 12, 28));
        assertEquals(3, yearService.saveTerms(y, List.of(q2, q1, v1)).size());
        assertEquals("1-chorak", yearService.terms(y).get(0).name());

        var outside = new AcademicYearService.Term(null, "Yozgi", "VACATION", LocalDate.of(2031, 6, 1), LocalDate.of(2031, 8, 31));
        assertThrows(IllegalStateException.class, () -> yearService.saveTerms(y, List.of(q1, outside)));
        var overlapping = new AcademicYearService.Term(null, "X", "QUARTER", LocalDate.of(2030, 11, 1), LocalDate.of(2030, 11, 20));
        assertThrows(IllegalStateException.class, () -> yearService.saveTerms(y, List.of(q1, overlapping)));
        // a refused save keeps what was there
        assertEquals(3, yearService.terms(y).size());

        // the cards: status, school days, terms
        ListPagesService.YearCard card = pages.years(school.id()).get(0);
        assertEquals(3, card.terms().size());
        assertTrue(card.workDays() > 200, "about 250 school days from September to June: " + card.workDays());
        assertEquals(card.workDays(), card.passedDays() + card.leftDays());
    }

    @Test
    void theNextYear_isProposedOneYearOn_andCreatedWithItsTerms() {
        long y = yearId();
        yearService.saveTerms(y, List.of(new AcademicYearService.Term(null, "1-chorak", "QUARTER",
                LocalDate.of(2030, 9, 2), LocalDate.of(2030, 11, 2))));
        AcademicYearService.NextYear next = yearService.nextTemplate(school.id());
        assertEquals("2031–2032", next.title());
        assertTrue(next.startDate().isAfter(LocalDate.of(2031, 6, 30)));
        assertEquals(LocalDate.of(2031, 9, 2), next.terms().get(0).startDate());
        AcademicYearResponseDto created = yearService.createNext(school.id(), next);
        assertEquals(1, yearService.terms(created.getId()).size());
        // a school without years gets the usual calendar: four quarters, three vacations
        jdbc.update("delete from academic_term where academic_year_id = ?", created.getId());
        assertEquals(7, yearService.nextTemplate(Long.MAX_VALUE).terms().size());
    }

    // ---------------------------------------------------------------- O'quvchilar, Xodimlar, Fanlar, Lavozimlar

    @Test
    void studentChips_telegram_bornToday_andTheBulkMove() {
        long sid = school.id();
        var first = school.students.get(0).get(0);
        jdbc.update("insert into parent_telegram_link (student_id, chat_id, active, linked_at) values (?, ?, true, now())",
                first.getId(), 900_000_000L + first.getId());
        LocalDate today = LocalDate.now();
        jdbc.update("update students set birth_date = ? where id = ?", Date.valueOf(today.minusYears(8)),
                school.students.get(0).get(1).getId());

        assertEquals(1, studentService.getAllStudents(sid, null, null, false, true, false, PageRequest.of(0, 50)).getTotalElements());
        assertEquals(9, studentService.getAllStudents(sid, null, null, false, false, false, PageRequest.of(0, 50)).getTotalElements());
        List<StudentResponseDto> born = studentService.getAllStudents(sid, null, null, false, null, true, PageRequest.of(0, 50)).getContent();
        assertEquals(1, born.size());
        assertEquals(school.students.get(0).get(1).getId(), born.get(0).getId());
        // nobody is at risk in a fresh school: the chip gives nothing, never everyone
        assertEquals(0, studentService.getAllStudents(sid, null, null, true, null, false, PageRequest.of(0, 50)).getTotalElements());

        long target = school.classes.get(1).getId();
        var moved = pages.moveStudents(List.of(first.getId(), school.students.get(0).get(2).getId()), target);
        assertEquals(2, moved.moved());
        assertEquals(7, studentService.getAllStudents(sid, target, null, PageRequest.of(0, 50)).getTotalElements());
        // never into another school
        long foreign = other.students.get(0).get(0).getId();
        assertThrows(IllegalStateException.class, () -> pages.moveStudents(List.of(foreign), target));

        ListPagesService.StudentExtra extra = pages.studentExtra(first.getId());
        assertTrue(extra.telegram());
        assertNotNull(extra.days());
    }

    @Test
    void classTeachers_chip_tabs_role_andTheExtras() {
        long sid = school.id();
        // the test teacher leads the first class
        assertEquals(1, employeeService.getAllEmployees(sid, null, null, null, true, PageRequest.of(0, 10)).getTotalElements());
        jdbc.update("update school_class set class_teacher_id = null where id = ?", school.classes.get(0).getId());
        assertEquals(0, employeeService.getAllEmployees(sid, null, null, null, true, PageRequest.of(0, 10)).getTotalElements());

        // Faol | Nofaol
        jdbc.update("update subject set active = false where id = ?", school.subject.getId());
        assertEquals(1, subjectService.getAllSubjects(sid, false, "INACTIVE", null, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, subjectService.getAllSubjects(sid, false, "ACTIVE", null, PageRequest.of(0, 10)).getTotalElements());
        jdbc.update("update subject set active = true where id = ?", school.subject.getId());

        // a position's default role: known roles only
        PositionRequestDto bad = new PositionRequestDto();
        bad.setTitle("Test rol " + System.nanoTime());
        bad.setDefaultRole("KING");
        assertThrows(ResponseStatusException.class, () -> positionService.createPosition(bad));
        bad.setDefaultRole("editor");
        bad.setDescription("  Dars beradi  ");
        var created = positionService.createPosition(bad);
        assertEquals("EDITOR", created.getDefaultRole());
        assertEquals("Dars beradi", created.getDescription());
        positionService.deletePosition(created.getId());

        // side panels on real rows
        ListPagesService.EmployeeExtra emp = pages.employeeExtra(school.teacher.getId());
        assertEquals(2 * TestSchool.STARTS.length, emp.weeklyLessons());
        assertEquals(TestSchool.STARTS.length, emp.lessonsPerDay());
        assertEquals(2, emp.heat()[0][0], "Monday, first lesson: both classes");
        ListPagesService.SubjectExtra sub = pages.subjectExtra(school.subject.getId());
        assertEquals(2, sub.classes().size());
        assertEquals(TestSchool.STARTS.length, sub.classes().get(0).lessons());
        assertEquals(1, sub.teachers().size());
        // the teacher's classes in this subject, hours a week each
        var mine = sub.byTeacher().get(school.teacher.getId());
        assertEquals(2, mine.size());
        assertEquals(TestSchool.STARTS.length, mine.get(0).lessons());
        assertEquals(4, pages.studentsHero(sid).size());
        assertEquals(4, pages.employeesHero(sid).size());
    }
}
