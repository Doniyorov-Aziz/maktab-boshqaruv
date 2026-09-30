package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BotSettingDto {

    @Size(max = 64)
    private String phone;

    @Size(max = 128)
    private String directorName;

    @Size(max = 500)
    private String receptionHours;

    @Size(max = 1000)
    private String bellScheduleNote;

    @NotNull
    private Boolean showTeacherPhones;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:00)?$", message = "HH:mm formatida bo'lishi kerak")
    private String tomorrowScheduleTime;

    @NotNull
    @Pattern(regexp = "^(MONDAY|TUESDAY|WEDNESDAY|THURSDAY|FRIDAY|SATURDAY|SUNDAY)$")
    private String weeklyReportDay;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:00)?$", message = "HH:mm formatida bo'lishi kerak")
    private String weeklyReportTime;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:00)?$", message = "HH:mm formatida bo'lishi kerak")
    private String eventReminderTime;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer lowGradeThreshold;

    /** Read-only: bell schedule derived from the timetable, e.g. ["1 · 08:30–09:15", ...]. */
    private java.util.List<String> derivedBells;

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

    public String getTomorrowScheduleTime() {
        return tomorrowScheduleTime;
    }

    public void setTomorrowScheduleTime(String tomorrowScheduleTime) {
        this.tomorrowScheduleTime = tomorrowScheduleTime;
    }

    public String getWeeklyReportDay() {
        return weeklyReportDay;
    }

    public void setWeeklyReportDay(String weeklyReportDay) {
        this.weeklyReportDay = weeklyReportDay;
    }

    public String getWeeklyReportTime() {
        return weeklyReportTime;
    }

    public void setWeeklyReportTime(String weeklyReportTime) {
        this.weeklyReportTime = weeklyReportTime;
    }

    public String getEventReminderTime() {
        return eventReminderTime;
    }

    public void setEventReminderTime(String eventReminderTime) {
        this.eventReminderTime = eventReminderTime;
    }

    public Integer getLowGradeThreshold() {
        return lowGradeThreshold;
    }

    public void setLowGradeThreshold(Integer lowGradeThreshold) {
        this.lowGradeThreshold = lowGradeThreshold;
    }

    public java.util.List<String> getDerivedBells() {
        return derivedBells;
    }

    public void setDerivedBells(java.util.List<String> derivedBells) {
        this.derivedBells = derivedBells;
    }
}
