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
import java.util.Set;

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

    private static final Set<String> PERIODS = Set.of("quarter", "month", "week");

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

    private static String period(String p) {
        return PERIODS.contains(p) ? p : "quarter";
    }

    /** Which home page the caller gets (ADMIN / TEACHER / VIEWER) — decided here. */
    @GetMapping("/home")
    public TeacherDashboardService.Home homeKind(@RequestParam Long schoolId) {
        return teacher.home(schoolId);
    }

    @GetMapping("/health")
    public Widget<HomeDashboardService.HealthCard> health(@RequestParam Long schoolId) {
        return cache.get(schoolId, "health", () -> home.health(schoolId));
    }

    @GetMapping("/kpis")
    public Widget<List<HomeDashboardService.Kpi>> kpis(@RequestParam Long schoolId,
                                                       @RequestParam(defaultValue = "quarter") String period) {
        String p = period(period);
        return cache.get(schoolId, "kpis:" + p, () -> home.kpis(schoolId, p));
    }

    @GetMapping("/today-map")
    public Widget<HomeDashboardService.TodayMap> todayMap(@RequestParam Long schoolId) {
        return cache.get(schoolId, "map", () -> home.todayMap(schoolId));
    }

    @GetMapping("/class-map")
    public Widget<HomeDashboardService.ClassMap> classMap(@RequestParam Long schoolId) {
        return cache.get(schoolId, "class-map", () -> home.classMap(schoolId));
    }

    /** Students at risk, highest first; limit up to 2000 (the side panel asks for all of them). */
    @GetMapping("/risk")
    public Widget<HomeDashboardService.RiskList> risk(@RequestParam Long schoolId, @RequestParam(defaultValue = "6") int limit) {
        int n = Math.max(1, Math.min(limit, 2000));
        boolean phones = access.caller().role() != Role.VIEWER;
        return cache.get(schoolId, "risk:" + n + ":" + phones, () -> home.riskStudents(schoolId, null, n, phones));
    }

    /**
     * The side panel's list as Excel, with the panel's filter: q (name), classId, level (high ≥ 70 /
     * mid 50–69 / low under 50), sort (score / absent / drop).
     */
    @GetMapping("/risk/export")
    public org.springframework.http.ResponseEntity<byte[]> riskExport(@RequestParam Long schoolId,
                                                                      @RequestParam(required = false) String q,
                                                                      @RequestParam(required = false) Long classId,
                                                                      @RequestParam(required = false) String level,
                                                                      @RequestParam(defaultValue = "score") String sort) {
        boolean phones = access.caller().role() != Role.VIEWER;
        List<HomeDashboardService.RiskRow> rows = RiskFilter.apply(home.riskStudents(schoolId, null, 2000, phones).rows(),
                q, classId, level, sort);
        List<List<Object>> table = new java.util.ArrayList<>();
        for (HomeDashboardService.RiskRow r : rows) {
            table.add(java.util.Arrays.asList(r.name(), r.className(), r.score(), RiskFilter.levelLabel(r.score()), r.absentDays(),
                    r.avgBefore(), r.avgNow(), r.twos(), r.telegram() ? "ha" : "yo'q", r.phone() == null ? "" : r.phone()));
        }
        byte[] file = uz.azizbek.maktabboshqaruv.util.XlsxWriter.write("Xavf ostida",
                List.of("O'quvchi", "Sinf", "Xavf balli", "Daraja", "Kelmagan kunlar (30)", "O'rtacha (oldin)",
                        "O'rtacha (2 hafta)", "«2» lar (30)", "Telegram", "Ota-ona telefoni"),
                List.of(28, 8, 10, 10, 18, 16, 18, 12, 10, 18), table);
        return org.springframework.http.ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        org.springframework.http.ContentDisposition.attachment().filename("xavf-ostidagi-oquvchilar.xlsx").build().toString())
                .body(file);
    }

    @GetMapping("/dynamics")
    public Widget<HomeDashboardService.Dynamics> dynamics(@RequestParam Long schoolId, @RequestParam(defaultValue = "30") int days) {
        return cache.get(schoolId, "dynamics:" + days, () -> home.dynamics(schoolId, days));
    }

    /** The compact "E'tibor talab qiladi" card; VIEWERs get it without phone numbers. */
    @GetMapping("/signals")
    public Widget<List<HomeDashboardService.Signal>> signals(@RequestParam Long schoolId) {
        boolean phones = access.caller().role() != Role.VIEWER;
        return cache.get(schoolId, "signals:" + phones, () -> home.signals(schoolId, null, phones));
    }

    /** The bell's groups (top 7 of each). */
    @GetMapping("/attention")
    public Widget<List<HomeDashboardService.AttentionGroup>> attention(@RequestParam Long schoolId) {
        boolean phones = access.caller().role() != Role.VIEWER;
        return cache.get(schoolId, "attention:" + phones, () -> home.attention(schoolId, null, phones));
    }

    @GetMapping("/live-lessons")
    public Widget<HomeDashboardService.Live> live(@RequestParam Long schoolId) {
        return cache.get(schoolId, "live", () -> home.liveLessons(schoolId));
    }

    @GetMapping("/subject-quality")
    public Widget<List<HomeDashboardService.SubjectRow>> subjects(@RequestParam Long schoolId,
                                                                   @RequestParam(defaultValue = "quarter") String period) {
        String p = period(period);
        return cache.get(schoolId, "subjects:" + p, () -> home.subjectQuality(schoolId, p));
    }

    @GetMapping("/grade-distribution")
    public Widget<List<HomeDashboardService.GradeWeek>> grades(@RequestParam Long schoolId) {
        return cache.get(schoolId, "grades", () -> home.gradeDistribution(schoolId));
    }

    @GetMapping("/journal")
    public Widget<HomeDashboardService.Journal> journal(@RequestParam Long schoolId) {
        return cache.get(schoolId, "journal", () -> home.journal(schoolId));
    }

    @GetMapping("/parents-telegram")
    public Widget<HomeDashboardService.ParentsTelegram> parents(@RequestParam Long schoolId) {
        return cache.get(schoolId, "parents", () -> home.parentsTelegram(schoolId));
    }

    @GetMapping("/events")
    public Widget<List<HomeDashboardService.EventRow>> events(@RequestParam Long schoolId, @RequestParam(defaultValue = "4") int limit) {
        return cache.get(schoolId, "events:" + limit, () -> home.events(schoolId, limit));
    }

    // ------------------------------------------------------------------ teacher's own widgets

    private TeacherDashboardService.Home teacherHome(Long schoolId) {
        TeacherDashboardService.Home h = teacher.home(schoolId);
        if (!"TEACHER".equals(h.kind())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bu bo'lim o'qituvchilar uchun");
        return h;
    }

    @GetMapping("/my-index")
    public Widget<TeacherDashboardService.MyIndex> myIndex(@RequestParam Long schoolId) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        return cache.get(schoolId, "my-index:" + h.employeeId(), () -> teacher.myIndex(schoolId, h.employeeId()));
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

    /** "Sinflarimdagi xavf ostidagi o'quvchilar". */
    @GetMapping("/my-risk")
    public Widget<HomeDashboardService.RiskList> myRisk(@RequestParam Long schoolId, @RequestParam(defaultValue = "6") int limit) {
        TeacherDashboardService.Home h = teacherHome(schoolId);
        int n = Math.max(1, Math.min(limit, 2000));
        return cache.get(schoolId, "my-risk:" + h.employeeId() + ":" + n,
                () -> home.riskStudents(schoolId, new HashSet<>(h.classIds()), n, true));
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
