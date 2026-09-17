package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class SchoolClassRequestDto {

    @NotNull(message = "O'quv yili tanlanishi shart")
    private Long academicYearId;

    @NotNull(message = "Sinf raqami kiritilishi shart")
    @Min(value = 1, message = "Sinf raqami 1 dan kichik bo'lishi mumkin emas")
    @Max(value = 11, message = "Sinf raqami 11 dan katta bo'lishi mumkin emas")
    private Integer gradeNumber;

    @NotBlank(message = "Sinf harfi kiritilishi shart")
    private String sectionLetter;

    @NotNull(message = "Maksimal o'quvchilar soni kiritilishi shart")
    @Positive(message = "Maksimal o'quvchilar soni musbat son bo'lishi kerak")
    private Integer maxStudents;

    public Long getAcademicYearId() {
        return academicYearId;
    }

    public void setAcademicYearId(Long academicYearId) {
        this.academicYearId = academicYearId;
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
}
