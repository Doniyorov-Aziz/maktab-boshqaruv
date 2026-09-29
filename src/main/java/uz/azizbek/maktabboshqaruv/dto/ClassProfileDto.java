package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

public class ClassProfileDto {

    private Long id;
    private String className;
    private String academicYearTitle;
    private Long classTeacherId;
    private String classTeacherName;
    private Long studentCount;
    private Double attendanceRate;
    private Double averageGrade;

    private List<StudentResponseDto> students;
    private List<TimetableEntryDto> weeklySchedule;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getAcademicYearTitle() {
        return academicYearTitle;
    }

    public void setAcademicYearTitle(String academicYearTitle) {
        this.academicYearTitle = academicYearTitle;
    }

    public Long getClassTeacherId() {
        return classTeacherId;
    }

    public void setClassTeacherId(Long classTeacherId) {
        this.classTeacherId = classTeacherId;
    }

    public String getClassTeacherName() {
        return classTeacherName;
    }

    public void setClassTeacherName(String classTeacherName) {
        this.classTeacherName = classTeacherName;
    }

    public Long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Long studentCount) {
        this.studentCount = studentCount;
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

    public List<StudentResponseDto> getStudents() {
        return students;
    }

    public void setStudents(List<StudentResponseDto> students) {
        this.students = students;
    }

    public List<TimetableEntryDto> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(List<TimetableEntryDto> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }
}
