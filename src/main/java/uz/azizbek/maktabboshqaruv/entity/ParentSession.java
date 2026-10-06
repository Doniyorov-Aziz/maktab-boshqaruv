package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Per-chat bot state: which child is selected, the parent's language and
 * notification preferences, and an in-progress conversation (writing to the
 * school, filing an absence request). Preference columns are nullable on
 * purpose — null means "default" (on / school default time), which keeps
 * ddl-auto=update happy and lets defaults change without a data migration.
 */
@Entity
@Table(name = "parent_session")
public class ParentSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    private Long selectedStudentId;

    @Column(length = 8)
    private String language;

    private String firstName;

    private String username;

    @Column(length = 32)
    private String pendingAction;

    @Column(columnDefinition = "TEXT")
    private String pendingData;

    /** Comma-separated ids of announcements the parent has opened (most recent last, capped). */
    @Column(columnDefinition = "TEXT")
    private String readAnnouncements;

    private Boolean notifyAbsence;
    private Boolean notifyGrades;
    private Boolean notifyLowGrade;
    private Boolean notifyAnnouncements;
    private Boolean notifyTomorrowSchedule;
    private Boolean notifyWeeklyReport;
    private Boolean notifyEventReminder;
    private Boolean notifyMorningDigest;

    private LocalTime scheduleTime;

    private Boolean quietHoursEnabled;
    private LocalTime quietStart;
    private LocalTime quietEnd;

    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;

    public String lang() {
        return language == null || language.isBlank() ? "uz" : language;
    }

    /** Whether this parent wants this kind of message (null preference = on). */
    public boolean wants(NotificationType type) {
        Boolean flag = switch (type) {
            case ATTENDANCE_ABSENT, ATTENDANCE_LATE -> notifyAbsence;
            case GRADE_NEW, GRADE_UPDATED -> notifyGrades;
            case GRADE_LOW -> notifyLowGrade;
            case ANNOUNCEMENT -> notifyAnnouncements;
            case TOMORROW_SCHEDULE -> notifyTomorrowSchedule;
            case WEEKLY_REPORT -> notifyWeeklyReport;
            case EVENT_REMINDER -> notifyEventReminder;
            case MORNING_DIGEST -> notifyMorningDigest;
            // Replies, decisions and school broadcasts are personal/official — always delivered.
            case MESSAGE_REPLY, ABSENCE_DECISION, BROADCAST -> Boolean.TRUE;
        };
        return !Boolean.FALSE.equals(flag);
    }

    public boolean hasRead(Long announcementId) {
        if (readAnnouncements == null || announcementId == null) return false;
        for (String part : readAnnouncements.split(",")) {
            if (part.equals(announcementId.toString())) return true;
        }
        return false;
    }

    public void markRead(Long announcementId) {
        if (announcementId == null || hasRead(announcementId)) return;
        String joined = readAnnouncements == null || readAnnouncements.isBlank()
                ? announcementId.toString() : readAnnouncements + "," + announcementId;
        String[] parts = joined.split(",");
        if (parts.length > 300) {
            joined = String.join(",", java.util.Arrays.copyOfRange(parts, parts.length - 300, parts.length));
        }
        readAnnouncements = joined;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public Long getSelectedStudentId() {
        return selectedStudentId;
    }

    public void setSelectedStudentId(Long selectedStudentId) {
        this.selectedStudentId = selectedStudentId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPendingAction() {
        return pendingAction;
    }

    public void setPendingAction(String pendingAction) {
        this.pendingAction = pendingAction;
    }

    public String getPendingData() {
        return pendingData;
    }

    public void setPendingData(String pendingData) {
        this.pendingData = pendingData;
    }

    public String getReadAnnouncements() {
        return readAnnouncements;
    }

    public void setReadAnnouncements(String readAnnouncements) {
        this.readAnnouncements = readAnnouncements;
    }

    public Boolean getNotifyAbsence() {
        return notifyAbsence;
    }

    public void setNotifyAbsence(Boolean notifyAbsence) {
        this.notifyAbsence = notifyAbsence;
    }

    public Boolean getNotifyGrades() {
        return notifyGrades;
    }

    public void setNotifyGrades(Boolean notifyGrades) {
        this.notifyGrades = notifyGrades;
    }

    public Boolean getNotifyLowGrade() {
        return notifyLowGrade;
    }

    public void setNotifyLowGrade(Boolean notifyLowGrade) {
        this.notifyLowGrade = notifyLowGrade;
    }

    public Boolean getNotifyAnnouncements() {
        return notifyAnnouncements;
    }

    public void setNotifyAnnouncements(Boolean notifyAnnouncements) {
        this.notifyAnnouncements = notifyAnnouncements;
    }

    public Boolean getNotifyTomorrowSchedule() {
        return notifyTomorrowSchedule;
    }

    public void setNotifyTomorrowSchedule(Boolean notifyTomorrowSchedule) {
        this.notifyTomorrowSchedule = notifyTomorrowSchedule;
    }

    public Boolean getNotifyWeeklyReport() {
        return notifyWeeklyReport;
    }

    public void setNotifyWeeklyReport(Boolean notifyWeeklyReport) {
        this.notifyWeeklyReport = notifyWeeklyReport;
    }

    public Boolean getNotifyMorningDigest() {
        return notifyMorningDigest;
    }

    public void setNotifyMorningDigest(Boolean notifyMorningDigest) {
        this.notifyMorningDigest = notifyMorningDigest;
    }

    public Boolean getNotifyEventReminder() {
        return notifyEventReminder;
    }

    public void setNotifyEventReminder(Boolean notifyEventReminder) {
        this.notifyEventReminder = notifyEventReminder;
    }

    public LocalTime getScheduleTime() {
        return scheduleTime;
    }

    public void setScheduleTime(LocalTime scheduleTime) {
        this.scheduleTime = scheduleTime;
    }

    public Boolean getQuietHoursEnabled() {
        return quietHoursEnabled;
    }

    public void setQuietHoursEnabled(Boolean quietHoursEnabled) {
        this.quietHoursEnabled = quietHoursEnabled;
    }

    public LocalTime getQuietStart() {
        return quietStart;
    }

    public void setQuietStart(LocalTime quietStart) {
        this.quietStart = quietStart;
    }

    public LocalTime getQuietEnd() {
        return quietEnd;
    }

    public void setQuietEnd(LocalTime quietEnd) {
        this.quietEnd = quietEnd;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }
}
