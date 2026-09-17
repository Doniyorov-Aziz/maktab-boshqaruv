package uz.azizbek.maktabboshqaruv.dto;

public class SchoolClassResponseDto {

    private Long id;
    private Integer gradeNumber;
    private String sectionLetter;
    private Integer maxStudents;
    private Long academicYearId;
    private String academicYearTitle;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getGradeNumber() {
        return gradeNumber;
    }

    public void setGradeNumber(Integer gradeNumber) {
        this.gradeNumber = gradeNumber;
    }

    public String getSectionLetter() {
        return sectionLetter;
    }

    public void setSectionLetter(String sectionLetter) {
        this.sectionLetter = sectionLetter;
    }

    public Integer getMaxStudents() {
        return maxStudents;
    }

    public void setMaxStudents(Integer maxStudents) {
        this.maxStudents = maxStudents;
    }

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
    }

    public String getAcademicYearTitle() {
        return academicYearTitle;
    }

    public void setAcademicYearTitle(String academicYearTitle) {
        this.academicYearTitle = academicYearTitle;
    }
}
