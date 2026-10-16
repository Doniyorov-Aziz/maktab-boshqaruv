package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance", uniqueConstraints = @UniqueConstraint(columnNames = {"lesson_slot_id", "student_id", "record_date"}))
@EntityListeners(AuditingEntityListener.class)
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "lesson_slot_id", nullable = false)
    private LessonSlot lessonSlot;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    private String comment;

    /** Existing rows get MANUAL when the column is added (NOT NULL DEFAULT). */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16, columnDefinition = "varchar(16) not null default 'MANUAL'")
    private AttendanceSource source = AttendanceSource.MANUAL;

    /** Username of the staff member for MANUAL marks; null for automatic ones. */
    @Column(name = "created_by", length = 100)
    private String createdBy;

    /** When an automatic process (or a device) set the mark; null for MANUAL. */
    @Column(name = "auto_at")
    private LocalDateTime autoAt;

    /** Minutes late, when known (devices, the demo simulator). */
    @Column(name = "late_minutes")
    private Integer lateMinutes;

    /** Why an automatic process set the mark ("2-darsdan ko'chirildi", "Turniket 08:47"). */
    @Column(name = "auto_note", length = 120)
    private String autoNote;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LessonSlot getLessonSlot() {
        return lessonSlot;
    }

    public void setLessonSlot(LessonSlot lessonSlot) {
        this.lessonSlot = lessonSlot;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
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

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public AttendanceSource getSource() {
        return source;
    }

    public void setSource(AttendanceSource source) {
        this.source = source;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getAutoAt() {
        return autoAt;
    }

    public void setAutoAt(LocalDateTime autoAt) {
        this.autoAt = autoAt;
    }

    public Integer getLateMinutes() {
        return lateMinutes;
    }

    public void setLateMinutes(Integer lateMinutes) {
        this.lateMinutes = lateMinutes;
    }

    public String getAutoNote() {
        return autoNote;
    }

    public void setAutoNote(String autoNote) {
        this.autoNote = autoNote;
    }
}
