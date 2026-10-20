package uz.azizbek.maktabboshqaruv.dto;
import jakarta.validation.constraints.NotBlank;

public class SchoolRequestDto {
    @NotBlank(message = "Maktab nomi kiritilishi shart")
    private String name;

    @NotBlank(message = "Manzil kiritilishi shart")
    private String address;

    @jakarta.validation.constraints.Pattern(regexp = "^$|^#[0-9a-fA-F]{6}$", message = "Rang #RRGGBB ko'rinishida bo'lishi kerak")
    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}

