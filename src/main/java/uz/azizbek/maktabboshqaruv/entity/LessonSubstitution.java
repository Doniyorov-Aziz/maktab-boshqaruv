package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * "O'qituvchi kelmadi → o'rinbosar": another teacher takes one lesson on ONE date. The weekly
 * timetable (lesson_slot) never changes; the timetable shows the lesson with an "o'rinbosar" mark.
 */
@Entity
@Table(name = "lesson_substitution", uniqueConstraints =
        @UniqueConstraint(name = "uk_substitution_lesson_date", columnNames = {"lesson_slot_id", "sub_date"}))
public class LessonSubstitution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_slot_id", nullable = false)
    private LessonSlot lessonSlot;

    @Column(name = "sub_date", nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "substitute_id", nullable = false)
    private Employee substitute;

    /** The teacher who did not come (the slot's teacher at that time). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "absent_teacher_id", nullable = false)
    private Employee absentTeacher;

    @Column(length = 200)
    private String reason;

    @Column(length = 100)
    private String createdBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public LessonSlot getLessonSlot() {
        return lessonSlot;
    }

    public void setLessonSlot(LessonSlot lessonSlot) {
        this.lessonSlot = lessonSlot;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Employee getSubstitute() {
        return substitute;
    }

    public void setSubstitute(Employee substitute) {
        this.substitute = substitute;
    }

    public Employee getAbsentTeacher() {
        return absentTeacher;
    }

    public void setAbsentTeacher(Employee absentTeacher) {
        this.absentTeacher = absentTeacher;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
