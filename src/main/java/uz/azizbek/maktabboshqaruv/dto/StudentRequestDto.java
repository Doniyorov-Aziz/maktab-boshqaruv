package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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

    private String guardianName;

    private String guardianPhone;

    private uz.azizbek.maktabboshqaruv.entity.GuardianRelation guardianRelation;

    // ---- tug'ilgan joyi va yashash manzili (all optional). The region ids drive the pickers and are
    // checked against the district; only the districts are stored.
    private Long birthRegionId;
    private Long birthDistrictId;
    private Long residenceRegionId;
    private Long residenceDistrictId;

    @Size(max = 100, message = "Mahalla nomi 100 belgidan oshmasin")
    private String mahalla;

    @Size(max = 150, message = "Ko'cha nomi 150 belgidan oshmasin")
    private String street;

    private uz.azizbek.maktabboshqaruv.entity.HousingType housingType;

    @Size(max = 20, message = "Dom / uy raqami 20 belgidan oshmasin")
    private String houseNumber;

    @Size(max = 20, message = "Kvartira raqami 20 belgidan oshmasin")
    private String apartmentNumber;

    public Long getBirthRegionId() {
        return birthRegionId;
    }

    public void setBirthRegionId(Long birthRegionId) {
        this.birthRegionId = birthRegionId;
    }

    public Long getBirthDistrictId() {
        return birthDistrictId;
    }

    public void setBirthDistrictId(Long birthDistrictId) {
        this.birthDistrictId = birthDistrictId;
    }

    public Long getResidenceRegionId() {
        return residenceRegionId;
    }

    public void setResidenceRegionId(Long residenceRegionId) {
        this.residenceRegionId = residenceRegionId;
    }

    public Long getResidenceDistrictId() {
        return residenceDistrictId;
    }

    public void setResidenceDistrictId(Long residenceDistrictId) {
        this.residenceDistrictId = residenceDistrictId;
    }

    public String getMahalla() {
        return mahalla;
    }

    public void setMahalla(String mahalla) {
        this.mahalla = mahalla;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public uz.azizbek.maktabboshqaruv.entity.HousingType getHousingType() {
        return housingType;
    }

    public void setHousingType(uz.azizbek.maktabboshqaruv.entity.HousingType housingType) {
        this.housingType = housingType;
    }

    public String getHouseNumber() {
        return houseNumber;
    }

    public void setHouseNumber(String houseNumber) {
        this.houseNumber = houseNumber;
    }

    public String getApartmentNumber() {
        return apartmentNumber;
    }

    public void setApartmentNumber(String apartmentNumber) {
        this.apartmentNumber = apartmentNumber;
    }

    public uz.azizbek.maktabboshqaruv.entity.GuardianRelation getGuardianRelation() {
        return guardianRelation;
    }

    public void setGuardianRelation(uz.azizbek.maktabboshqaruv.entity.GuardianRelation guardianRelation) {
        this.guardianRelation = guardianRelation;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianPhone() {
        return guardianPhone;
    }

    public void setGuardianPhone(String guardianPhone) {
        this.guardianPhone = guardianPhone;
    }

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
