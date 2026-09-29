package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.AnnouncementAudience;
import uz.azizbek.maktabboshqaruv.entity.AnnouncementPriority;

import java.time.LocalDate;

public class AnnouncementRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

    @NotBlank(message = "Sarlavha kiritilishi shart")
    private String title;

    @NotBlank(message = "Matn kiritilishi shart")
    private String content;

    @NotNull(message = "Auditoriya tanlanishi shart")
    private AnnouncementAudience audience;

    private Long schoolClassId;

    @NotNull(message = "Muhimlik darajasi tanlanishi shart")
    private AnnouncementPriority priority;

    private LocalDate deadline;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public AnnouncementAudience getAudience() {
        return audience;
    }

    public void setAudience(AnnouncementAudience audience) {
        this.audience = audience;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public AnnouncementPriority getPriority() {
        return priority;
    }

    public void setPriority(AnnouncementPriority priority) {
        this.priority = priority;
    }

    public LocalDate getDeadline() {
        return deadline;
    }

    public void setDeadline(LocalDate deadline) {
        this.deadline = deadline;
    }
}
