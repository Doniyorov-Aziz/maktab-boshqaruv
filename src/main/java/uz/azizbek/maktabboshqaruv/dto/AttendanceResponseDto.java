package uz.azizbek.maktabboshqaruv.dto;

import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

import java.time.LocalDate;

public class AttendanceResponseDto {

    private Long id;
    private Long lessonSlotId;
    private String className;
    private String subjectName;
    private Long studentId;
    private String studentName;
    private LocalDate recordDate;
    private AttendanceStatus status;
    private String comment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLessonSlotId() {
        return lessonSlotId;
    }

    public void setLessonSlotId(Long lessonSlotId) {
        this.lessonSlotId = lessonSlotId;
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

    public LocalDate getRecordDate() {
        return recordDate;
    }

    public void setRecordDate(LocalDate recordDate) {
        this.recordDate = recordDate;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
