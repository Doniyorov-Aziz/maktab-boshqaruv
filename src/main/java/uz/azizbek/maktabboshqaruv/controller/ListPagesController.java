package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import uz.azizbek.maktabboshqaruv.service.pages.ListPagesService;

import java.util.List;

/**
 * The list pages' extras: hero numbers, side-panel details, school-year cards and the bulk move.
 * The school of an id in the path (students/12, employees/3, subjects/7) or of ?schoolId is
 * checked by SchoolScopeConfig like everywhere else.
 */
@RestController
@RequestMapping("/api/pages/lists")
public class ListPagesController {

    private final ListPagesService lists;

    public ListPagesController(ListPagesService lists) {
        this.lists = lists;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/students/hero")
    public List<ListPagesService.Kpi> studentsHero(@RequestParam Long schoolId) {
        return lists.studentsHero(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/students/{id}/extra")
    public ListPagesService.StudentExtra studentExtra(@PathVariable Long id) {
        return lists.studentExtra(id);
    }

    /** "Boshqa sinfga ko'chirish" for the checked students. */
    public record MoveRequest(List<Long> studentIds, Long schoolClassId) {
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    @PostMapping("/students/move")
    public ListPagesService.MoveResult move(@RequestBody MoveRequest request) {
        if (request.studentIds() == null || request.studentIds().isEmpty() || request.schoolClassId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "O'quvchilar va sinf tanlanishi kerak");
        }
        return lists.moveStudents(request.studentIds(), request.schoolClassId());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/employees/hero")
    public List<ListPagesService.Kpi> employeesHero(@RequestParam Long schoolId) {
        return lists.employeesHero(schoolId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/employees/{id}/extra")
    public ListPagesService.EmployeeExtra employeeExtra(@PathVariable Long id) {
        return lists.employeeExtra(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/subjects/{id}/extra")
    public ListPagesService.SubjectExtra subjectExtra(@PathVariable Long id) {
        return lists.subjectExtra(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/years")
    public List<ListPagesService.YearCard> years(@RequestParam Long schoolId) {
        return lists.years(schoolId);
    }

    /** The year cards and, when no year holds today, the year to create (its usual calendar). */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/years-page")
    public ListPagesService.YearsPage yearsPage(@RequestParam Long schoolId) {
        return lists.yearsPage(schoolId);
    }
}
