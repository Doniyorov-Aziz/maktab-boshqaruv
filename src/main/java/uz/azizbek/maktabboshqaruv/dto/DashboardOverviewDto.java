package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

/**
 * Bundles every dashboard widget's data into a single response so the
 * frontend can render the whole page from one HTTP round trip instead of
 * six parallel ones.
 */
public class DashboardOverviewDto {

    private DashboardSummaryDto summary;
    private List<LiveLessonDto> liveLessons;
    private LiveLessonsSummaryDto liveLessonsSummary;
    private List<AttendanceTrendPointDto> attendanceTrend;
    private List<ClassRankingDto> classRankings;
    private List<SubjectAverageDto> subjectAverages;
    private List<AttentionItemDto> attention;
    private List<ActivityItemDto> activity;

    public DashboardSummaryDto getSummary() {
        return summary;
    }

    public void setSummary(DashboardSummaryDto summary) {
        this.summary = summary;
    }

    public List<LiveLessonDto> getLiveLessons() {
        return liveLessons;
    }

    public void setLiveLessons(List<LiveLessonDto> liveLessons) {
        this.liveLessons = liveLessons;
    }

    public LiveLessonsSummaryDto getLiveLessonsSummary() {
        return liveLessonsSummary;
    }

    public void setLiveLessonsSummary(LiveLessonsSummaryDto liveLessonsSummary) {
        this.liveLessonsSummary = liveLessonsSummary;
    }

    public List<AttendanceTrendPointDto> getAttendanceTrend() {
        return attendanceTrend;
    }

    public void setAttendanceTrend(List<AttendanceTrendPointDto> attendanceTrend) {
        this.attendanceTrend = attendanceTrend;
    }

    public List<ClassRankingDto> getClassRankings() {
        return classRankings;
    }

    public void setClassRankings(List<ClassRankingDto> classRankings) {
        this.classRankings = classRankings;
    }

    public List<SubjectAverageDto> getSubjectAverages() {
        return subjectAverages;
    }

    public void setSubjectAverages(List<SubjectAverageDto> subjectAverages) {
        this.subjectAverages = subjectAverages;
    }

    public List<AttentionItemDto> getAttention() {
        return attention;
    }

    public void setAttention(List<AttentionItemDto> attention) {
        this.attention = attention;
    }

    public List<ActivityItemDto> getActivity() {
        return activity;
    }

    public void setActivity(List<ActivityItemDto> activity) {
        this.activity = activity;
    }
}
