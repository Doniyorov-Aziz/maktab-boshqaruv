package uz.azizbek.maktabboshqaruv.dto;

public class ClassRankingDto {

    private Long schoolClassId;
    private String className;
    private String homeroomTeacherName;
    private Double attendanceRate;
    private Double attendanceRateDelta;
    private Double averageGrade;
    private Double averageGradeDelta;
    private Double overallScore;
    private Double overallScoreDelta;

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getHomeroomTeacherName() {
        return homeroomTeacherName;
    }

    public void setHomeroomTeacherName(String homeroomTeacherName) {
        this.homeroomTeacherName = homeroomTeacherName;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public Double getAttendanceRateDelta() {
        return attendanceRateDelta;
    }

    public void setAttendanceRateDelta(Double attendanceRateDelta) {
        this.attendanceRateDelta = attendanceRateDelta;
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

    public Double getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(Double overallScore) {
        this.overallScore = overallScore;
    }

    public Double getOverallScoreDelta() {
        return overallScoreDelta;
    }

    public void setOverallScoreDelta(Double overallScoreDelta) {
        this.overallScoreDelta = overallScoreDelta;
    }
}
