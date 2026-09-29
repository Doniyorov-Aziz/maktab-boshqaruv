package uz.azizbek.maktabboshqaruv.controller;

import uz.azizbek.maktabboshqaruv.dto.*;
import uz.azizbek.maktabboshqaruv.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'EDITOR', 'VIEWER')")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/overview")
    public DashboardOverviewDto getOverview(@RequestParam Long schoolId,
                                             @RequestParam(defaultValue = "30") int trendDays,
                                             @RequestParam(defaultValue = "8") int activityLimit) {
        return dashboardService.getOverview(schoolId, trendDays, activityLimit);
    }

    @GetMapping("/summary")
    public DashboardSummaryDto getSummary(@RequestParam Long schoolId) {
        return dashboardService.getSummary(schoolId);
    }

    @GetMapping("/live-lessons")
    public List<LiveLessonDto> getLiveLessons(@RequestParam Long schoolId) {
        return dashboardService.getLiveLessons(schoolId);
    }

    @GetMapping("/live-lessons-summary")
    public LiveLessonsSummaryDto getLiveLessonsSummary(@RequestParam Long schoolId) {
        return dashboardService.getLiveLessonsSummary(schoolId);
    }

    @GetMapping("/attendance-trend")
    public List<AttendanceTrendPointDto> getAttendanceTrend(@RequestParam Long schoolId,
                                                              @RequestParam(defaultValue = "30") int days) {
        return dashboardService.getAttendanceTrend(schoolId, days);
    }

    @GetMapping("/class-rankings")
    public List<ClassRankingDto> getClassRankings(@RequestParam Long schoolId) {
        return dashboardService.getClassRankings(schoolId);
    }

    @GetMapping("/subject-averages")
    public List<SubjectAverageDto> getSubjectAverages(@RequestParam Long schoolId) {
        return dashboardService.getSubjectAverages(schoolId);
    }

    @GetMapping("/absentees-today")
    public List<AbsenteeDto> getAbsenteesToday(@RequestParam Long schoolId) {
        return dashboardService.getAbsenteesToday(schoolId);
    }

    @GetMapping("/attention")
    public List<AttentionItemDto> getAttentionItems(@RequestParam Long schoolId) {
        return dashboardService.getAttentionItems(schoolId);
    }

    @GetMapping("/activity")
    public List<ActivityItemDto> getRecentActivity(@RequestParam Long schoolId, @RequestParam(defaultValue = "8") int limit) {
        return dashboardService.getRecentActivity(schoolId, limit);
    }
}
