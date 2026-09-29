package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

import java.time.LocalDate;

public class AttendanceRequestDto {

    @NotNull(message = "Dars tanlanishi shart")
    private Long lessonSlotId;

    @NotNull(message = "O'quvchi tanlanishi shart")
    private Long studentId;

    @NotNull(message = "Sana kiritilishi shart")
    private LocalDate recordDate;

    @NotNull(message = "Holat tanlanishi shart")
    private AttendanceStatus status;

    private String comment;

    public Long getLessonSlotId() {
        return lessonSlotId;
    }

    public void setLessonSlotId(Long lessonSlotId) {
        this.lessonSlotId = lessonSlotId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
