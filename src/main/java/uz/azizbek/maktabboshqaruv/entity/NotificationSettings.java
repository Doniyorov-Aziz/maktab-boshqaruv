package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

/**
 * Per-school switches for parent notifications. A school without a row uses
 * {@link #defaults}, so nothing has to be migrated for existing schools.
 */
@Entity
@Table(name = "notification_settings")
public class NotificationSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    @Column(nullable = false)
    private Boolean attendanceEnabled;

    @Column(nullable = false)
    private Boolean gradeEnabled;

    @Column(nullable = false)
    private Boolean announcementEnabled;

    @Column(nullable = false)
    private Boolean quietHoursEnabled;

    @Column(nullable = false)
    private LocalTime quietHoursStart;

    @Column(nullable = false)
    private LocalTime quietHoursEnd;

    public static NotificationSettings defaults(School school, LocalTime quietStart, LocalTime quietEnd) {
        NotificationSettings s = new NotificationSettings();
        s.setSchool(school);
        s.setAttendanceEnabled(true);
        s.setGradeEnabled(true);
        s.setAnnouncementEnabled(true);
        s.setQuietHoursEnabled(true);
        s.setQuietHoursStart(quietStart);
        s.setQuietHoursEnd(quietEnd);
        return s;
    }

    public boolean isTypeEnabled(NotificationType type) {
        return switch (type) {
            case ATTENDANCE_ABSENT, ATTENDANCE_LATE -> Boolean.TRUE.equals(attendanceEnabled);
            case GRADE_NEW, GRADE_UPDATED, GRADE_LOW -> Boolean.TRUE.equals(gradeEnabled);
            case ANNOUNCEMENT -> Boolean.TRUE.equals(announcementEnabled);
            // Scheduled digests are switched per parent in the bot; replies,
            // decisions and admin broadcasts are always delivered.
            case TOMORROW_SCHEDULE, WEEKLY_REPORT, EVENT_REMINDER, MESSAGE_REPLY, ABSENCE_DECISION, BROADCAST, MORNING_DIGEST -> true;
        };
    }

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

    public Boolean getAttendanceEnabled() {
        return attendanceEnabled;
    }

    public void setAttendanceEnabled(Boolean attendanceEnabled) {
        this.attendanceEnabled = attendanceEnabled;
    }

    public Boolean getGradeEnabled() {
        return gradeEnabled;
    }

    public void setGradeEnabled(Boolean gradeEnabled) {
        this.gradeEnabled = gradeEnabled;
    }

    public Boolean getAnnouncementEnabled() {
        return announcementEnabled;
    }

    public void setAnnouncementEnabled(Boolean announcementEnabled) {
        this.announcementEnabled = announcementEnabled;
    }

    public Boolean getQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(Boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
}
