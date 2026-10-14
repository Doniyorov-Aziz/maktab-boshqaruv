package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDate;

public class StudentResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private LocalDate birthDate;
    private Long schoolClassId;
    private String className;
    private String guardianName;
    private String guardianPhone;
    private String guardianRelation;

    // tug'ilgan joyi va manzil: ids for the edit form, ready-made text for lists and the profile
    private Long birthRegionId;
    private Long birthDistrictId;
    private Long residenceRegionId;
    private Long residenceDistrictId;
    private String mahalla;
    private String street;
    private String housingType;
    private String houseNumber;
    private String apartmentNumber;
    private String birthPlace;
    private String residencePlace;
    private String address;

    public String getGuardianRelation() {
        return guardianRelation;
    }

    public void setGuardianRelation(String guardianRelation) {
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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

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

    public String getHousingType() {
        return housingType;
    }

    public void setHousingType(String housingType) {
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

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getResidencePlace() {
        return residencePlace;
    }

    public void setResidencePlace(String residencePlace) {
        this.residencePlace = residencePlace;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
