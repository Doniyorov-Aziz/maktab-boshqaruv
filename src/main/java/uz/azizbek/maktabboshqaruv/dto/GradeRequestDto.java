package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.GradeType;

import java.time.LocalDate;

public class GradeRequestDto {

    @NotNull(message = "O'quvchi tanlanishi shart")
    private Long studentId;

    @NotNull(message = "Fan tanlanishi shart")
    private Long subjectId;

    @NotNull(message = "Sana kiritilishi shart")
    private LocalDate gradeDate;

    @NotNull(message = "Baho kiritilishi shart")
    @Min(value = 2, message = "Baho 2 dan kam bo'lmasligi kerak")
    @Max(value = 5, message = "Baho 5 dan katta bo'lmasligi kerak")
    private Integer score;

    @NotNull(message = "Turi tanlanishi shart")
    private GradeType type;

    private String comment;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public LocalDate getGradeDate() {
        return gradeDate;
    }

    public void setGradeDate(LocalDate gradeDate) {
        this.gradeDate = gradeDate;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public GradeType getType() {
        return type;
    }

    public void setType(GradeType type) {
        this.type = type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
