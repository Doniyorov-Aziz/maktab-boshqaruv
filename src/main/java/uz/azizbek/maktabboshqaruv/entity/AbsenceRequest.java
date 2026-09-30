package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * "🤒 Sababli ariza": a parent tells the school the child will miss school on
 * some days. Approving it marks those days' attendance EXCUSED.
 */
@Entity
@Table(name = "absence_request", indexes = {
        @Index(name = "idx_absence_request_school_status", columnList = "school_id, status"),
        @Index(name = "idx_absence_request_chat", columnList = "chat_id")
})
public class AbsenceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    private String parentName;

    @Column(nullable = false)
    private LocalDate dateFrom;

    @Column(nullable = false)
    private LocalDate dateTo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AbsenceReason reason;

    @Column(columnDefinition = "TEXT")
    private String comment;

    private String photoFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AbsenceStatus status;

    @Column(columnDefinition = "TEXT")
    private String decisionNote;

    private String decidedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime decidedAt;

    /** How many lesson attendance records were set to EXCUSED on approval. */
    private Integer excusedLessons;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Student getStudent() {
        return student;
    }

    public void setStudent(Student student) {
        this.student = student;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }

    public AbsenceReason getReason() {
        return reason;
    }

    public void setReason(AbsenceReason reason) {
        this.reason = reason;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getPhotoFileId() {
        return photoFileId;
    }

    public void setPhotoFileId(String photoFileId) {
        this.photoFileId = photoFileId;
    }

    public AbsenceStatus getStatus() {
        return status;
    }

    public void setStatus(AbsenceStatus status) {
        this.status = status;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(String decidedBy) {
        this.decidedBy = decidedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(LocalDateTime decidedAt) {
        this.decidedAt = decidedAt;
    }

    public Integer getExcusedLessons() {
        return excusedLessons;
    }

    public void setExcusedLessons(Integer excusedLessons) {
        this.excusedLessons = excusedLessons;
    }
}
