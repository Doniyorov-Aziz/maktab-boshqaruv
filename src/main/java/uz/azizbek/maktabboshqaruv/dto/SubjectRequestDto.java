package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SubjectRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

    @NotBlank(message = "Fan nomi kiritilishi shart")
    private String name;

    /** "#rrggbb", or empty for the automatic colour */
    @jakarta.validation.constraints.Pattern(regexp = "^(#[0-9a-fA-F]{6})?$", message = "Rang #rrggbb ko'rinishida bo'lishi kerak")
    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
