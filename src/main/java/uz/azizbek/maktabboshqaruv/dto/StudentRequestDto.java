package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class StudentRequestDto {

    @NotNull(message = "Sinf tanlanishi shart")
    private Long schoolClassId;

    @NotBlank(message = "Ism kiritilishi shart")
    @Pattern(regexp = "^[A-Za-zА-Яа-яЎўҚқҒғҲҳ'\\s]+$", message = "Ism faqat harflardan iborat bo'lishi kerak")
    private String firstName;

    @NotBlank(message = "Familiya kiritilishi shart")
    @Pattern(regexp = "^[A-Za-zА-Яа-яЎўҚқҒғҲҳ'\\s]+$", message = "Familiya faqat harflardan iborat bo'lishi kerak")
    private String lastName;

    @NotNull(message = "Tug'ilgan sana kiritilishi shart")
    @Past(message = "Tug'ilgan sana o'tmishda bo'lishi kerak")
    private LocalDate birthDate;

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}
