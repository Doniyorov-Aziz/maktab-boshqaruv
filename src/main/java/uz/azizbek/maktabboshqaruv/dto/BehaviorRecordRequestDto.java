package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.BehaviorType;

import java.time.LocalDate;

public class BehaviorRecordRequestDto {

    @NotNull(message = "O'quvchi tanlanishi shart")
    private Long studentId;

    @NotNull(message = "Sana kiritilishi shart")
    private LocalDate recordDate;

    @NotNull(message = "Turi tanlanishi shart")
    private BehaviorType type;

    @NotBlank(message = "Tavsif kiritilishi shart")
    private String description;

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

    public BehaviorType getType() {
        return type;
    }

    public void setType(BehaviorType type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
