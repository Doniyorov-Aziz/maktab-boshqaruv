package uz.azizbek.maktabboshqaruv.dto;

import uz.azizbek.maktabboshqaruv.entity.AttendanceStatus;

public class AttendanceRosterEntryDto {

    private Long studentId;
    private String studentName;
    private Long attendanceId;
    private AttendanceStatus status;
    private String comment;
    /** MANUAL / AUTO_CARRY / DEMO_SIM / DEVICE; null when not marked. */
    private String source;
    /** Who marked it (MANUAL); for automatic marks autoAt says when. */
    private String createdBy;
    private java.time.LocalDateTime autoAt;
    private String autoNote;
    private Integer lateMinutes;

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

    public Long getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
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

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public java.time.LocalDateTime getAutoAt() { return autoAt; }
    public void setAutoAt(java.time.LocalDateTime autoAt) { this.autoAt = autoAt; }
    public String getAutoNote() { return autoNote; }
    public void setAutoNote(String autoNote) { this.autoNote = autoNote; }
    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
}
