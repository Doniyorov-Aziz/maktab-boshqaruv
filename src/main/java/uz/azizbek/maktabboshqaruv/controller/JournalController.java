package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.service.JournalService;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The class panel above the journal and the teacher's current lesson. Phone numbers are
 * only here, behind login (staff of the same school — the school scope guard checks the class).
 */
@RestController
public class JournalController {

    private final JournalService journal;
    private final SchoolAccessService access;

    public JournalController(JournalService journal, SchoolAccessService access) {
        this.journal = journal;
        this.access = access;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/api/classes/{id}/overview")
    public JournalService.Overview overview(@PathVariable Long id) {
        return journal.overview(id);
    }

    /** The caller's lesson now (or ended ≤ 15 min ago): {classId, subjectId, lessonNo, …}; 204 otherwise. */
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
    @GetMapping("/api/me/current-lesson")
    public ResponseEntity<JournalService.CurrentLesson> currentLesson() {
        return journal.currentLesson(access.caller().employeeId())
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
