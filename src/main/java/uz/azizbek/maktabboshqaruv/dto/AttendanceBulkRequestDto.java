package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

import java.time.LocalDate;
import java.util.List;

public class AttendanceBulkRequestDto {

    @NotNull(message = "Dars tanlanishi shart")
    private Long lessonSlotId;

    @NotNull(message = "Sana kiritilishi shart")
    private LocalDate recordDate;

    @NotEmpty(message = "Kamida bitta o'quvchi bo'lishi kerak")
    @Valid
    private List<Entry> entries;

    public Long getLessonSlotId() {
        return lessonSlotId;
    }

    public void setLessonSlotId(Long lessonSlotId) {
        this.lessonSlotId = lessonSlotId;
    }

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public void setEntries(List<Entry> entries) {
        this.entries = entries;
    }

    public static class Entry {
        @NotNull(message = "O'quvchi tanlanishi shart")
        private Long studentId;

        @NotNull(message = "Holat tanlanishi shart")
        private AttendanceStatus status;

        private String comment;

        public Long getStudentId() {
            return studentId;
        }

        public void setStudentId(Long studentId) {
            this.studentId = studentId;
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
}
