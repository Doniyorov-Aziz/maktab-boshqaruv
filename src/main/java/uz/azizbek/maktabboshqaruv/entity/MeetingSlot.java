package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Ota-ona uchrashuvi: a free time a teacher offers ("12-noyabr, 15:00–15:15, 204-xona"). A parent books it in the
 * bot (one booking per slot, taken atomically); the teacher sees who booked it and why, and either side can cancel.
 */
@Entity
@Table(name = "meeting_slot", indexes = {
        @Index(name = "idx_meeting_teacher_date", columnList = "employee_id, slot_date"),
        @Index(name = "idx_meeting_school_date", columnList = "school_id, slot_date")
})
public class MeetingSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "slot_date", nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(length = 80)
    private String place;

    /** Who booked it: the child the meeting is about and the parent's chat (null = free). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id")
    private Student student;

    @Column(name = "chat_id")
    private Long chatId;

    @Column(name = "parent_name", length = 120)
    private String parentName;

    @Column(name = "parent_phone", length = 32)
    private String parentPhone;

    /** What the parent wants to talk about (optional). */
    @Column(length = 300)
    private String topic;

    @Column(name = "booked_at")
    private LocalDateTime bookedAt;

    @Column(name = "created_by", length = 64)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isBooked() {
        return chatId != null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }
    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }
    public Long getChatId() { return chatId; }
    public void setChatId(Long chatId) { this.chatId = chatId; }
    public String getParentName() { return parentName; }
    public void setParentName(String parentName) { this.parentName = parentName; }
    public String getParentPhone() { return parentPhone; }
    public void setParentPhone(String parentPhone) { this.parentPhone = parentPhone; }
    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }
    public LocalDateTime getBookedAt() { return bookedAt; }
    public void setBookedAt(LocalDateTime bookedAt) { this.bookedAt = bookedAt; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
