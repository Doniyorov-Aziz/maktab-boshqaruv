package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

public class DashboardSummaryDto {

    private Double todayAttendanceRate;
    private Double attendanceRateDelta;
    private List<Double> attendanceTrend14;

    private Long todayAbsentCount;
    private Long absentCountDelta;
    private List<Long> absentTrend14;

    private Double averageGrade;
    private Double averageGradeDelta;
    private List<Double> gradeTrend14;

    private Long totalStudents;
    private Long totalTeachers;

    private Long classesWithAttendanceToday;
    private Long totalClasses;

    public Double getTodayAttendanceRate() {
        return todayAttendanceRate;
    }

    public void setTodayAttendanceRate(Double todayAttendanceRate) {
        this.todayAttendanceRate = todayAttendanceRate;
    }

    public Double getAttendanceRateDelta() {
        return attendanceRateDelta;
    }

    public void setAttendanceRateDelta(Double attendanceRateDelta) {
        this.attendanceRateDelta = attendanceRateDelta;
    }

    public List<Double> getAttendanceTrend14() {
        return attendanceTrend14;
    }

    public void setAttendanceTrend14(List<Double> attendanceTrend14) {
        this.attendanceTrend14 = attendanceTrend14;
    }

    public Long getTodayAbsentCount() {
        return todayAbsentCount;
    }

    public void setTodayAbsentCount(Long todayAbsentCount) {
        this.todayAbsentCount = todayAbsentCount;
    }

    public Long getAbsentCountDelta() {
        return absentCountDelta;
    }

    public void setAbsentCountDelta(Long absentCountDelta) {
        this.absentCountDelta = absentCountDelta;
    }

    public List<Long> getAbsentTrend14() {
        return absentTrend14;
    }

    public void setAbsentTrend14(List<Long> absentTrend14) {
        this.absentTrend14 = absentTrend14;
    }

    public Double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(Double averageGrade) {
        this.averageGrade = averageGrade;
    }

    public Double getAverageGradeDelta() {
        return averageGradeDelta;
    }

    public void setAverageGradeDelta(Double averageGradeDelta) {
        this.averageGradeDelta = averageGradeDelta;
    }

    public List<Double> getGradeTrend14() {
        return gradeTrend14;
    }

    public void setGradeTrend14(List<Double> gradeTrend14) {
        this.gradeTrend14 = gradeTrend14;
    }

    public Long getTotalStudents() {
        return totalStudents;
    }

    public void setTotalStudents(Long totalStudents) {
        this.totalStudents = totalStudents;
    }

    public Long getTotalTeachers() {
        return totalTeachers;
    }

    public void setTotalTeachers(Long totalTeachers) {
        this.totalTeachers = totalTeachers;
    }

    public Long getClassesWithAttendanceToday() {
        return classesWithAttendanceToday;
    }

    public void setClassesWithAttendanceToday(Long classesWithAttendanceToday) {
        this.classesWithAttendanceToday = classesWithAttendanceToday;
    }

    public Long getTotalClasses() {
        return totalClasses;
    }

    public void setTotalClasses(Long totalClasses) {
        this.totalClasses = totalClasses;
    }
}
