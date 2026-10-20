package uz.azizbek.maktabboshqaruv.dto;

public class SubjectResponseDto {

    private Long id;
    private String name;
    private Long schoolId;
    private String schoolName;
    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    private boolean active = true;

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    // list page numbers (ListStatsService); null where not computed
    private Integer teacherCount;
    private Integer classCount;
    private Integer weeklyLessons;

    public Integer getTeacherCount() {
        return teacherCount;
    }

    public void setTeacherCount(Integer teacherCount) {
        this.teacherCount = teacherCount;
    }

    public Integer getClassCount() {
        return classCount;
    }

    public void setClassCount(Integer classCount) {
        this.classCount = classCount;
    }

    public Integer getWeeklyLessons() {
        return weeklyLessons;
    }

    public void setWeeklyLessons(Integer weeklyLessons) {
        this.weeklyLessons = weeklyLessons;
    }

    /** Material icon name; null = the page suggests one from the name. */
    private String icon;

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    /** Who teaches it ([{ id, name, lessons }]), quality this quarter and the one before, 30-day average run. */
    private java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Person> teachers;
    private Double qualityNow;
    private Double qualityPrev;
    private java.util.List<Double> averageSpark;

    public java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Person> getTeachers() {
        return teachers;
    }

    public void setTeachers(java.util.List<uz.azizbek.maktabboshqaruv.service.pages.ListStatsService.Person> teachers) {
        this.teachers = teachers;
    }

    public Double getQualityNow() {
        return qualityNow;
    }

    public void setQualityNow(Double qualityNow) {
        this.qualityNow = qualityNow;
    }

    public Double getQualityPrev() {
        return qualityPrev;
    }

    public void setQualityPrev(Double qualityPrev) {
        this.qualityPrev = qualityPrev;
    }

    public java.util.List<Double> getAverageSpark() {
        return averageSpark;
    }

    public void setAverageSpark(java.util.List<Double> averageSpark) {
        this.averageSpark = averageSpark;
    }
}
