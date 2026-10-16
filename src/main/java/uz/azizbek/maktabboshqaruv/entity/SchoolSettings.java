package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * "Maktab sozlamalari" (ADMIN): attendance automation and the thresholds the home page uses.
 * A school without a row uses the defaults from application.properties (attendance.*, dashboard.*).
 * The demo simulator is not here on purpose: it can only be switched on in application.properties.
 */
@Entity
@Table(name = "school_settings")
public class SchoolSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    /** Copy the student's status from the previous lesson of the day when a lesson has no mark. */
    @Column(nullable = false)
    private boolean autoCarryEnabled;

    /** Minutes after a lesson starts before AUTO_CARRY fills it. */
    @Column(nullable = false)
    private int autoCarryDelayMinutes;

    /** Minutes after a lesson starts before the teacher is reminded. */
    @Column(nullable = false)
    private int reminderMinutes;

    /** Minutes after a lesson starts before an unmarked lesson goes to "E'tibor talab qiladi". */
    @Column(nullable = false)
    private int escalationMinutes;

    /** Tell parents about ABSENT / LATE that AUTO_CARRY set (off by default). */
    @Column(nullable = false)
    private boolean notifyOnAuto;

    /** Attendance target, % (the dashed line on the trend chart). */
    @Column(nullable = false)
    private int attendanceTarget;

    /** Consecutive school days absent that put a student on the attention list. */
    @Column(nullable = false)
    private int absentStreakDays;

    /** A teacher whose unmarked lessons over 7 school days reach this %, is listed. */
    @Column(nullable = false)
    private int disciplineThreshold;

    /** An appeal unanswered for this many hours is overdue. */
    @Column(nullable = false)
    private int appealOverdueHours;

    /** Classes whose parents' Telegram coverage is below this % are listed. */
    @Column(nullable = false)
    private int telegramLowPercent;

    /** Attendance map colours: ≥ good dark green, ≥ ok green, ≥ warn yellow, below red. */
    @Column(nullable = false)
    private int heatGood;

    @Column(nullable = false)
    private int heatOk;

    @Column(nullable = false)
    private int heatWarn;

    // the home page health index: weights of its four parts (they add up to 100)
    @Column(name = "health_weight_attendance", nullable = false, columnDefinition = "integer not null default 40")
    private int healthWeightAttendance = 40;

    @Column(name = "health_weight_quality", nullable = false, columnDefinition = "integer not null default 30")
    private int healthWeightQuality = 30;

    @Column(name = "health_weight_journal", nullable = false, columnDefinition = "integer not null default 20")
    private int healthWeightJournal = 20;

    @Column(name = "health_weight_telegram", nullable = false, columnDefinition = "integer not null default 10")
    private int healthWeightTelegram = 10;

    private LocalDateTime updatedAt;

    private String updatedBy;

    public Long getId() { return id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public boolean isAutoCarryEnabled() { return autoCarryEnabled; }
    public void setAutoCarryEnabled(boolean v) { this.autoCarryEnabled = v; }
    public int getAutoCarryDelayMinutes() { return autoCarryDelayMinutes; }
    public void setAutoCarryDelayMinutes(int v) { this.autoCarryDelayMinutes = v; }
    public int getReminderMinutes() { return reminderMinutes; }
    public void setReminderMinutes(int v) { this.reminderMinutes = v; }
    public int getEscalationMinutes() { return escalationMinutes; }
    public void setEscalationMinutes(int v) { this.escalationMinutes = v; }
    public boolean isNotifyOnAuto() { return notifyOnAuto; }
    public void setNotifyOnAuto(boolean v) { this.notifyOnAuto = v; }
    public int getAttendanceTarget() { return attendanceTarget; }
    public void setAttendanceTarget(int v) { this.attendanceTarget = v; }
    public int getAbsentStreakDays() { return absentStreakDays; }
    public void setAbsentStreakDays(int v) { this.absentStreakDays = v; }
    public int getDisciplineThreshold() { return disciplineThreshold; }
    public void setDisciplineThreshold(int v) { this.disciplineThreshold = v; }
    public int getAppealOverdueHours() { return appealOverdueHours; }
    public void setAppealOverdueHours(int v) { this.appealOverdueHours = v; }
    public int getTelegramLowPercent() { return telegramLowPercent; }
    public void setTelegramLowPercent(int v) { this.telegramLowPercent = v; }
    public int getHeatGood() { return heatGood; }
    public void setHeatGood(int v) { this.heatGood = v; }
    public int getHeatOk() { return heatOk; }
    public void setHeatOk(int v) { this.heatOk = v; }
    public int getHeatWarn() { return heatWarn; }
    public void setHeatWarn(int v) { this.heatWarn = v; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v) { this.updatedAt = v; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String v) { this.updatedBy = v; }
    public int getHealthWeightAttendance() {
        return healthWeightAttendance;
    }

    public void setHealthWeightAttendance(int healthWeightAttendance) {
        this.healthWeightAttendance = healthWeightAttendance;
    }

    public int getHealthWeightQuality() {
        return healthWeightQuality;
    }

    public void setHealthWeightQuality(int healthWeightQuality) {
        this.healthWeightQuality = healthWeightQuality;
    }

    public int getHealthWeightJournal() {
        return healthWeightJournal;
    }

    public void setHealthWeightJournal(int healthWeightJournal) {
        this.healthWeightJournal = healthWeightJournal;
    }

    public int getHealthWeightTelegram() {
        return healthWeightTelegram;
    }

    public void setHealthWeightTelegram(int healthWeightTelegram) {
        this.healthWeightTelegram = healthWeightTelegram;
    }

}
