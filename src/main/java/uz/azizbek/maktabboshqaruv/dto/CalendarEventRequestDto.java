package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.CalendarEventType;

import java.time.LocalDate;

public class CalendarEventRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

    @NotBlank(message = "Sarlavha kiritilishi shart")
    private String title;

    private String description;

    @NotNull(message = "Turi tanlanishi shart")
    private CalendarEventType type;

    @NotNull(message = "Boshlanish sanasi kiritilishi shart")
    private LocalDate startDate;

    @NotNull(message = "Tugash sanasi kiritilishi shart")
    private LocalDate endDate;

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CalendarEventType getType() {
        return type;
    }

    public void setType(CalendarEventType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
