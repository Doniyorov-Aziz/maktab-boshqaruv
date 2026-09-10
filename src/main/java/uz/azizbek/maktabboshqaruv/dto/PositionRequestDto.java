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
}
