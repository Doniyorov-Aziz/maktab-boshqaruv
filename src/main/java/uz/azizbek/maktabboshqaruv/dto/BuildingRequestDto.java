package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BuildingRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

    @NotBlank(message = "Bino nomi kiritilishi shart")
    private String name;

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
