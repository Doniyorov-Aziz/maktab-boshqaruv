package uz.azizbek.maktabboshqaruv.dto;

public class EmployeeResponseDto {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;
    private Long positionId;
    private String positionTitle;
    private Long schoolId;
    private String schoolName;
    private String status;
    private java.time.LocalDate leaveFrom;
    private java.time.LocalDate leaveTo;
    /** "Matematika, Fizika" — subjects the employee teaches (from the timetable). */
    private String subjects;
    /** "5-A, 6-B" — classes the employee teaches. */
    private String classes;
    /** "7-A" — the class(es) this employee leads as class teacher. */
    private String classTeacherOf;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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

    public String getSubjects() {
        return subjects;
    }

    public void setSubjects(String subjects) {
        this.subjects = subjects;
    }

    public String getClasses() {
        return classes;
    }

    public void setClasses(String classes) {
        this.classes = classes;
    }

    public String getClassTeacherOf() {
        return classTeacherOf;
    }

    public void setClassTeacherOf(String classTeacherOf) {
        this.classTeacherOf = classTeacherOf;
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

    public String getPositionTitle() {
        return positionTitle;
    }

    public void setPositionTitle(String positionTitle) {
        this.positionTitle = positionTitle;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }
}