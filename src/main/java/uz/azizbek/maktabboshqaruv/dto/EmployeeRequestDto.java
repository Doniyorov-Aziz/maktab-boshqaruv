package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class EmployeeRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

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

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
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

    /** ACTIVE / ON_LEAVE / DISMISSED; null keeps the current status (ACTIVE for a new employee). */
    private uz.azizbek.maktabboshqaruv.entity.EmployeeStatus status;
    private java.time.LocalDate leaveFrom;
    private java.time.LocalDate leaveTo;

    public uz.azizbek.maktabboshqaruv.entity.EmployeeStatus getStatus() {
        return status;
    }

    public void setStatus(uz.azizbek.maktabboshqaruv.entity.EmployeeStatus status) {
        this.status = status;
    }

    public java.time.LocalDate getLeaveFrom() {
        return leaveFrom;
    }

    public void setLeaveFrom(java.time.LocalDate leaveFrom) {
        this.leaveFrom = leaveFrom;
    }

    public java.time.LocalDate getLeaveTo() {
        return leaveTo;
    }

    public void setLeaveTo(java.time.LocalDate leaveTo) {
        this.leaveTo = leaveTo;
    }
}