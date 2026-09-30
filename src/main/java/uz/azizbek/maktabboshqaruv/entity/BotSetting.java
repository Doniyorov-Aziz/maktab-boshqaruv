package uz.azizbek.maktabboshqaruv.entity;

import jakarta.persistence.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * School-level settings of the parent bot ("Bot sozlamalari"): contacts shown
 * in "🏫 Maktab haqida", teacher-phone visibility and the times of automatic
 * messages. A school without a row uses {@link #defaults}.
 */
@Entity
@Table(name = "bot_setting")
public class BotSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "school_id", nullable = false, unique = true)
    private School school;

    private String phone;

    private String directorName;

    /** Free text, e.g. "Dushanba, Chorshanba 14:00–16:00". */
    @Column(columnDefinition = "TEXT")
    private String receptionHours;

    /** Optional override of the bell schedule; empty = derived from the timetable. */
    @Column(columnDefinition = "TEXT")
    private String bellScheduleNote;

    @Column(nullable = false)
    private Boolean showTeacherPhones;

    @Column(nullable = false)
    private LocalTime tomorrowScheduleTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private DayOfWeek weeklyReportDay;

    @Column(nullable = false)
    private LocalTime weeklyReportTime;

    @Column(nullable = false)
    private LocalTime eventReminderTime;

    /** Grades at or below this score trigger the gentle "past baho" message. */
    @Column(nullable = false)
    private Integer lowGradeThreshold;

    public static BotSetting defaults(School school) {
        BotSetting s = new BotSetting();
        s.setSchool(school);
        s.setShowTeacherPhones(false);
        s.setTomorrowScheduleTime(LocalTime.of(19, 0));
        s.setWeeklyReportDay(DayOfWeek.SATURDAY);
        s.setWeeklyReportTime(LocalTime.of(18, 0));
        s.setEventReminderTime(LocalTime.of(18, 0));
        s.setLowGradeThreshold(2);
        return s;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDirectorName() {
        return directorName;
    }

    public void setDirectorName(String directorName) {
        this.directorName = directorName;
    }

    public String getReceptionHours() {
        return receptionHours;
    }

    public void setReceptionHours(String receptionHours) {
        this.receptionHours = receptionHours;
    }

    public String getBellScheduleNote() {
        return bellScheduleNote;
    }

    public void setBellScheduleNote(String bellScheduleNote) {
        this.bellScheduleNote = bellScheduleNote;
    }

    public Boolean getShowTeacherPhones() {
        return showTeacherPhones;
    }

    public void setShowTeacherPhones(Boolean showTeacherPhones) {
        this.showTeacherPhones = showTeacherPhones;
    }

    public LocalTime getTomorrowScheduleTime() {
        return tomorrowScheduleTime;
    }

    public void setTomorrowScheduleTime(LocalTime tomorrowScheduleTime) {
        this.tomorrowScheduleTime = tomorrowScheduleTime;
    }

    public DayOfWeek getWeeklyReportDay() {
        return weeklyReportDay;
    }

    public void setWeeklyReportDay(DayOfWeek weeklyReportDay) {
        this.weeklyReportDay = weeklyReportDay;
    }

    public LocalTime getWeeklyReportTime() {
        return weeklyReportTime;
    }

    public void setWeeklyReportTime(LocalTime weeklyReportTime) {
        this.weeklyReportTime = weeklyReportTime;
    }

    public LocalTime getEventReminderTime() {
        return eventReminderTime;
    }

    public void setEventReminderTime(LocalTime eventReminderTime) {
        this.eventReminderTime = eventReminderTime;
    }

    public Integer getLowGradeThreshold() {
        return lowGradeThreshold;
    }

    public void setLowGradeThreshold(Integer lowGradeThreshold) {
        this.lowGradeThreshold = lowGradeThreshold;
    }
}
