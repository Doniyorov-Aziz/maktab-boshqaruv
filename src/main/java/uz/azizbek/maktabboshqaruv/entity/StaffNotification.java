package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A message inside the app for one staff member (the bell in the top bar), e.g. "1-A · 3-dars ·
 * Algebra — davomat hali olinmagan". One reminder per lesson and day (unique key), so the
 * scheduler running every minute never repeats it.
 */
@Entity
@Table(name = "staff_notification", uniqueConstraints = @UniqueConstraint(
        name = "uk_staff_notification_lesson_day_kind", columnNames = {"lesson_slot_id", "record_date", "kind"}))
public class StaffNotification {

    public enum Kind {
        ATTENDANCE_REMINDER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_slot_id")
    private LessonSlot lessonSlot;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Kind kind;

    @Column(nullable = false, length = 300)
    private String text;

    /** Where a click goes, e.g. "/attendance?lessonSlotId=12&date=2026-10-16". */
    @Column(length = 300)
    private String link;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public Long getId() { return id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LessonSlot getLessonSlot() { return lessonSlot; }
    public void setLessonSlot(LessonSlot lessonSlot) { this.lessonSlot = lessonSlot; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public Kind getKind() { return kind; }
    public void setKind(Kind kind) { this.kind = kind; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}
