package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDate;
import java.util.List;

public class StudentProfileDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate birthDate;
    private Integer age;
    private Long schoolClassId;
    private String className;
    private String guardianName;
    private String guardianPhone;

    private Double attendanceRate;
    private Double averageGrade;
    private Long rewardCount;
    private Long warningCount;

    private List<SubjectAverageDto> subjectAverages;
    private List<HeatmapDayDto> attendanceHeatmap;
    private List<BehaviorRecordResponseDto> behaviorRecords;
    private List<TimetableEntryDto> weeklySchedule;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

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

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public void setGuardianPhone(String guardianPhone) {
        this.guardianPhone = guardianPhone;
    }

    public Double getAttendanceRate() {
        return attendanceRate;
    }

    public void setAttendanceRate(Double attendanceRate) {
        this.attendanceRate = attendanceRate;
    }

    public Double getAverageGrade() {
        return averageGrade;
    }

    public void setAverageGrade(Double averageGrade) {
        this.averageGrade = averageGrade;
    }

    public Long getRewardCount() {
        return rewardCount;
    }

    public void setRewardCount(Long rewardCount) {
        this.rewardCount = rewardCount;
    }

    public Long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Long warningCount) {
        this.warningCount = warningCount;
    }

    public List<SubjectAverageDto> getSubjectAverages() {
        return subjectAverages;
    }

    public void setSubjectAverages(List<SubjectAverageDto> subjectAverages) {
        this.subjectAverages = subjectAverages;
    }

    public List<HeatmapDayDto> getAttendanceHeatmap() {
        return attendanceHeatmap;
    }

    public void setAttendanceHeatmap(List<HeatmapDayDto> attendanceHeatmap) {
        this.attendanceHeatmap = attendanceHeatmap;
    }

    public List<BehaviorRecordResponseDto> getBehaviorRecords() {
        return behaviorRecords;
    }

    public void setBehaviorRecords(List<BehaviorRecordResponseDto> behaviorRecords) {
        this.behaviorRecords = behaviorRecords;
    }

    public List<TimetableEntryDto> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(List<TimetableEntryDto> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }
}
