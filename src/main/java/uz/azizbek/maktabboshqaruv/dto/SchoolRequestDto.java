package uz.azizbek.maktabboshqaruv.dto;
import jakarta.validation.constraints.NotBlank;

public class SchoolRequestDto {
    @NotBlank(message = "Maktab nomi kiritilishi shart")
    private String name;

    @NotBlank(message = "Manzil kiritilishi shart")
    private String address;

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

