package uz.azizbek.maktabboshqaruv.dto;

public class AbsenteeDto {

    private Long studentId;
    private String studentName;
    private String className;
    private String subjectName;

    public AbsenteeDto() {
    }

    public AbsenteeDto(Long studentId, String studentName, String className, String subjectName) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.className = className;
        this.subjectName = subjectName;
    }

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }
}
