package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;

public class SubjectRequestDto {

    @NotBlank(message = "Fan nomi kiritilishi shart")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
