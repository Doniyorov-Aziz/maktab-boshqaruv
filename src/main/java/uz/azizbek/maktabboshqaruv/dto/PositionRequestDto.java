package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;

public class PositionRequestDto {

    @NotBlank(message = "Lavozim nomi kiritilishi shart")
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @jakarta.validation.constraints.Size(max = 500, message = "Tavsif 500 belgidan oshmasin")
    private String description;
    /** ADMIN / EDITOR / VIEWER — offered to a new user of an employee in this position. */
    private String defaultRole;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultRole() {
        return defaultRole;
    }

    public void setDefaultRole(String defaultRole) {
        this.defaultRole = defaultRole;
    }
}
