package uz.azizbek.maktabboshqaruv.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import uz.azizbek.maktabboshqaruv.entity.ActivityLog;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.repository.ActivityLogRepository;
import uz.azizbek.maktabboshqaruv.service.SchoolAccessService;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache;
import uz.azizbek.maktabboshqaruv.service.dashboard.DashboardCache.Widget;
import uz.azizbek.maktabboshqaruv.service.dashboard.HomeDashboardService;
import uz.azizbek.maktabboshqaruv.service.dashboard.TeacherDashboardService;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

/**
 * The home page, one endpoint per widget (each loads, fails and retries on its own). Answers are
 * cached for 90 s per school and dropped when attendance or a grade is saved. Everything is
 * limited to ?schoolId (the school-scope guard checks it). The teacher's own widgets use the
 * caller's employee, never an id from the request.
 */
@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
public class DashboardController {

    private final HomeDashboardService home;
    private final TeacherDashboardService teacher;
    private final DashboardCache cache;
    private final SchoolAccessService access;
    private final ActivityLogRepository activity;

    public DashboardController(HomeDashboardService home, TeacherDashboardService teacher, DashboardCache cache,
                               SchoolAccessService access, ActivityLogRepository activity) {
        this.home = home;
        this.teacher = teacher;
        this.cache = cache;
        this.access = access;
        this.activity = activity;
    }

    /** Which home page the caller gets (ADMIN / TEACHER / VIEWER) — decided here. */
    @GetMapping("/home")
    public TeacherDashboardService.Home homeKind(@RequestParam Long schoolId) {
        return teacher.home(schoolId);
    }

    @GetMapping("/kpis")
    public Widget<List<HomeDashboardService.Kpi>> kpis(@RequestParam Long schoolId) {
        return cache.get(schoolId, "kpis", () -> home.kpis(schoolId));
    }

    @GetMapping("/today-map")
    public Widget<HomeDashboardService.TodayMap> todayMap(@RequestParam Long schoolId) {
        return cache.get(schoolId, "map", () -> home.todayMap(schoolId));
    }

    @GetMapping("/attention")
    public Widget<List<HomeDashboardService.AttentionGroup>> attention(@RequestParam Long schoolId) {
        boolean phones = access.caller().role() != Role.VIEWER;
        return cache.get(schoolId, "attention:" + phones, () -> home.attention(schoolId, null, phones));
    }

    @GetMapping("/live-lessons")
    public Widget<HomeDashboardService.Live> live(@RequestParam Long schoolId) {
        return cache.get(schoolId, "live", () -> home.liveLessons(schoolId));
    }

    @GetMapping("/attendance-trend")
    public Widget<HomeDashboardService.Trend> trend(@RequestParam Long schoolId, @RequestParam(defaultValue = "30") int days) {
        return cache.get(schoolId, "trend:" + days, () -> home.trend(schoolId, days));
    }

    @GetMapping("/class-ranking")
    public Widget<List<HomeDashboardService.RankingRow>> ranking(@RequestParam Long schoolId) {
        return cache.get(schoolId, "ranking", () -> home.classRanking(schoolId));
    }

    @GetMapping("/subject-quality")
    public Widget<List<HomeDashboardService.SubjectRow>> subjects(@RequestParam Long schoolId) {
        return cache.get(schoolId, "subjects", () -> home.subjectQuality(schoolId));
    }

    @GetMapping("/grade-distribution")
    public Widget<List<HomeDashboardService.GradeWeek>> grades(@RequestParam Long schoolId) {
        return cache.get(schoolId, "grades", () -> home.gradeDistribution(schoolId));
    }

    @GetMapping("/teacher-discipline")
    public Widget<List<HomeDashboardService.TeacherRow>> discipline(@RequestParam Long schoolId) {
        return cache.get(schoolId, "discipline", () -> home.teacherDiscipline(schoolId, null));
    }

    @GetMapping("/parents-telegram")
    public Widget<HomeDashboardService.ParentsTelegram> parents(@RequestParam Long schoolId) {
        return cache.get(schoolId, "parents", () -> home.parentsTelegram(schoolId));
    }

    @GetMapping("/events")
    public Widget<List<HomeDashboardService.EventRow>> events(@RequestParam Long schoolId) {
        return cache.get(schoolId, "events", () -> home.events(schoolId));
    }

    // ------------------------------------------------------------------ teacher's own widgets

    private TeacherDashboardService.Home teacherHome(Long schoolId) {
        TeacherDashboardService.Home h = teacher.home(schoolId);
        if (!"TEACHER".equals(h.kind())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu bo'lim o'qituvchilar uchun");
        return h;
    }

    @GetMapping("/my-lessons")
    public Widget<List<TeacherDashboardService.MyLesson>> myLessons(@RequestParam Long schoolId) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        return cache.get(schoolId, "my-lessons:" + h.employeeId(), () -> teacher.myLessons(schoolId, h.employeeId()));
    }

    @GetMapping("/my-missing")
    public Widget<List<TeacherDashboardService.Missing>> myMissing(@RequestParam Long schoolId) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        return cache.get(schoolId, "my-missing:" + h.employeeId(), () -> teacher.myMissing(schoolId, h.employeeId()));
    }

    @GetMapping("/my-attention")
    public Widget<List<HomeDashboardService.AttentionGroup>> myAttention(@RequestParam Long schoolId) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        return cache.get(schoolId, "my-attention:" + h.employeeId(),
                () -> home.attention(schoolId, new HashSet<>(h.classIds()), true));
    }

    @GetMapping("/my-appeals")
    public Widget<List<TeacherDashboardService.AppealRow>> myAppeals(@RequestParam Long schoolId) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        return cache.get(schoolId, "my-appeals:" + h.employeeId(), () -> teacher.myAppeals(schoolId, h.ledClassId()));
    }

    @GetMapping("/my-class")
    public Widget<TeacherDashboardService.MyClass> myClass(@RequestParam Long schoolId) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        if (h.ledClassId() == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Siz sinf rahbari emassiz");
        return cache.get(schoolId, "my-class:" + h.employeeId(), () -> teacher.myClass(schoolId, h.ledClassId()));
    }

    // ------------------------------------------------------------------ "Faollik jurnali"

    public record ActivityRow(long id, String icon, String description, String actor, LocalDateTime occurredAt) {
    }

    @GetMapping("/activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public Page<ActivityRow> activity(@RequestParam Long schoolId, @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "30") int size) {
        return activity.findBySchoolId(schoolId, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "occurredAt")))
                .map((ActivityLog a) -> new ActivityRow(a.getId(), a.getIcon(), a.getDescription(), a.getActorUsername(), a.getOccurredAt()));
    }
}
