package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

public class TeacherProfileDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private String positionTitle;
    private String homeroomClassName;
    private Long weeklyLoadCount;

    private List<String> subjects;
    private List<String> classes;
    private List<TimetableEntryDto> todayLessons;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public String getHomeroomClassName() {
        return homeroomClassName;
    }

    public void setHomeroomClassName(String homeroomClassName) {
        this.homeroomClassName = homeroomClassName;
    }

    public Long getWeeklyLoadCount() {
        return weeklyLoadCount;
    }

    public void setWeeklyLoadCount(Long weeklyLoadCount) {
        this.weeklyLoadCount = weeklyLoadCount;
    }

    public List<String> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<String> subjects) {
        this.subjects = subjects;
    }

    public List<String> getClasses() {
        return classes;
    }

    public void setClasses(List<String> classes) {
        this.classes = classes;
    }

    public List<TimetableEntryDto> getTodayLessons() {
        return todayLessons;
    }

    public void setTodayLessons(List<TimetableEntryDto> todayLessons) {
        this.todayLessons = todayLessons;
    }

    public List<TimetableEntryDto> getWeeklySchedule() {
        return weeklySchedule;
    }

    public void setWeeklySchedule(List<TimetableEntryDto> weeklySchedule) {
        this.weeklySchedule = weeklySchedule;
    }
}
