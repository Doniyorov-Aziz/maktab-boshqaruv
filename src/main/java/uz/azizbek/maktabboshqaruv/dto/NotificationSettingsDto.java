package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class NotificationSettingsDto {

    @NotNull
    private Boolean attendanceEnabled;

    @NotNull
    private Boolean gradeEnabled;

    @NotNull
    private Boolean announcementEnabled;

    @NotNull
    private Boolean quietHoursEnabled;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:00)?$", message = "HH:mm formatida bo'lishi kerak")
    private String quietHoursStart;

    @NotNull
    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d(:00)?$", message = "HH:mm formatida bo'lishi kerak")
    private String quietHoursEnd;

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

    public String getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(String quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public String getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(String quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
}
