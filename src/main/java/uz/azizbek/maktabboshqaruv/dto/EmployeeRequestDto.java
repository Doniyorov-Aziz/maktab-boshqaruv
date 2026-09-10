package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class EmployeeRequestDto {

    @NotBlank(message = "Ism kiritilishi shart")
    @Pattern(regexp = "^[A-Za-zА-Яа-яЎўҚқҒғҲҳ'\\s]+$", message = "Ism faqat harflardan iborat bo'lishi kerak")
    private String firstName;

    @NotBlank(message = "Familiya kiritilishi shart")
    @Pattern(regexp = "^[A-Za-zА-Яа-яЎўҚқҒғҲҳ'\\s]+$", message = "Familiya faqat harflardan iborat bo'lishi kerak")
    private String lastName;

    @NotBlank(message = "Telefon raqam kiritilishi shart")
    @Pattern(regexp = "^\\+?[0-9]{9,15}$", message = "Telefon raqam noto'g'ri formatda")
    private String phone;

    private Long positionId;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Long getPositionId() {
        return positionId;
    }

    public void setPositionId(Long positionId) {
        this.positionId = positionId;
    }
}