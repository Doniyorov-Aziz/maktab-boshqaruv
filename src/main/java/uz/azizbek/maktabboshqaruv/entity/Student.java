package uz.azizbek.maktabboshqaruv.entity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "class_id" , nullable = false)
    private SchoolClass schoolClass;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private LocalDate birthDate;

    private String guardianName;

    private String guardianPhone;

    /** otasi / onasi / boshqa — shown as "Furqat, otasi". Nullable: old records keep working. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private GuardianRelation guardianRelation;

    // ---- tug'ilgan joyi va yashash manzili (all nullable: older records and quick entries have none)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "birth_district_id")
    private District birthDistrict;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "residence_district_id")
    private District residenceDistrict;

    @Column(length = 100)
    private String mahalla;

    /** ko'cha (or massiv / kvartal) */
    @Column(length = 150)
    private String street;

    @Enumerated(EnumType.STRING)
    @Column(name = "housing_type", length = 16)
    private HousingType housingType;

    /** KVARTIRA: dom raqami; HOVLI: uy raqami ("12", "12A") */
    @Column(name = "house_number", length = 20)
    private String houseNumber;

    /** KVARTIRA only */
    @Column(name = "apartment_number", length = 20)
    private String apartmentNumber;

    public District getBirthDistrict() {
        return birthDistrict;
    }

    public void setBirthDistrict(District birthDistrict) {
        this.birthDistrict = birthDistrict;
    }

    public District getResidenceDistrict() {
        return residenceDistrict;
    }

    public void setResidenceDistrict(District residenceDistrict) {
        this.residenceDistrict = residenceDistrict;
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

    public HousingType getHousingType() {
        return housingType;
    }

    public void setHousingType(HousingType housingType) {
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

    /** "Navro'z mahallasi, Bunyodkor ko'chasi, 12-dom, 45-kvartira" / "..., 17-uy"; null when nothing is set */
    public String addressLine() {
        List<String> parts = new ArrayList<>();
        if (mahalla != null && !mahalla.isBlank()) parts.add(mahalla.trim());
        if (street != null && !street.isBlank()) parts.add(street.trim());
        if (houseNumber != null && !houseNumber.isBlank()) {
            if (housingType == HousingType.KVARTIRA) {
                parts.add(houseNumber.trim() + "-dom");
                if (apartmentNumber != null && !apartmentNumber.isBlank()) parts.add(apartmentNumber.trim() + "-kvartira");
            } else {
                parts.add(houseNumber.trim() + "-uy");
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    public GuardianRelation getGuardianRelation() {
        return guardianRelation;
    }

    public void setGuardianRelation(GuardianRelation guardianRelation) {
        this.guardianRelation = guardianRelation;
    }

    // Nullable on purpose: ddl-auto=update can only add a nullable column to a
    // table that already has rows. Existing students get a code from
    // TelegramLinkCodeBackfill at startup; new ones from @PrePersist below.
    @Column(name = "telegram_link_code", unique = true, length = 32)
    private String telegramLinkCode;

    @PrePersist
    void assignTelegramLinkCode() {
        if (telegramLinkCode == null) {
            telegramLinkCode = uz.azizbek.maktabboshqaruv.telegram.LinkCodeGenerator.generate();
        }
    }

    public String getTelegramLinkCode() {
        return telegramLinkCode;
    }

    public void setTelegramLinkCode(String telegramLinkCode) {
        this.telegramLinkCode = telegramLinkCode;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SchoolClass getSchoolClass() {
        return schoolClass;
    }

    public void setSchoolClass(SchoolClass schoolClass) {
        this.schoolClass = schoolClass;
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
}