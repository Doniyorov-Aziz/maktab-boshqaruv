package uz.azizbek.maktabboshqaruv.entity;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee")
@EntityListeners(AuditingEntityListener.class)
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String phone;

    /** Nullable column (ddl-auto=update on an existing table): null reads as ACTIVE. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private EmployeeStatus status;

    /** Optional leave period; after leaveTo the daily job returns the employee to ACTIVE. */
    private java.time.LocalDate leaveFrom;

    private java.time.LocalDate leaveTo;

    public EmployeeStatus getStatus() {
        return status == null ? EmployeeStatus.ACTIVE : status;
    }

    public void setStatus(EmployeeStatus status) {
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

    /**
     * Whether the employee is away on {@code today}: dismissed, or on leave whose period
     * (if given) covers today. A leave planned for later does not block yet.
     */
    public boolean isAwayOn(java.time.LocalDate today) {
        EmployeeStatus s = getStatus();
        if (s == EmployeeStatus.DISMISSED) return true;
        if (s != EmployeeStatus.ON_LEAVE) return false;
        return (leaveFrom == null || !today.isBefore(leaveFrom)) && (leaveTo == null || !today.isAfter(leaveTo));
    }

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }
}