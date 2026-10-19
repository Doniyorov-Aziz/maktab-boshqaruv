package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import uz.azizbek.maktabboshqaruv.entity.CalendarEventType;

import java.time.LocalDate;

public class CalendarEventRequestDto {

    @NotNull(message = "Maktab tanlanishi shart")
    private Long schoolId;

    @NotBlank(message = "Sarlavha kiritilishi shart")
    private String title;

    private String description;

    @NotNull(message = "Turi tanlanishi shart")
    private CalendarEventType type;

    @NotNull(message = "Boshlanish sanasi kiritilishi shart")
    private LocalDate startDate;

    @NotNull(message = "Tugash sanasi kiritilishi shart")
    private LocalDate endDate;

    /** Time of day (both or neither); null = whole day. */
    private java.time.LocalTime startTime;
    private java.time.LocalTime endTime;
    /** The room it takes (booked automatically; a conflict is a 409). */
    private Long roomId;
    /** A place outside the rooms ("Hovli"). */
    private String location;
    /** For whom: class ids; empty = the whole school. */
    private java.util.List<Long> classIds;
    /** Tell the parents on Telegram (when created and the day before). */
    private Boolean notifyParents;

    public java.time.LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(java.time.LocalTime startTime) {
        this.startTime = startTime;
    }

    public java.time.LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(java.time.LocalTime endTime) {
        this.endTime = endTime;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public java.util.List<Long> getClassIds() {
        return classIds;
    }

    public void setClassIds(java.util.List<Long> classIds) {
        this.classIds = classIds;
    }

    public Boolean getNotifyParents() {
        return notifyParents;
    }

    public void setNotifyParents(Boolean notifyParents) {
        this.notifyParents = notifyParents;
    }

    /** NONE (default) / WEEKLY / MONTHLY — a new event only: the copies are created up to recurrenceUntil. */
    private String recurrence;
    private LocalDate recurrenceUntil;

    public String getRecurrence() {
        return recurrence;
    }

    public void setRecurrence(String recurrence) {
        this.recurrence = recurrence;
    }

    public LocalDate getRecurrenceUntil() {
        return recurrenceUntil;
    }

    public void setRecurrenceUntil(LocalDate recurrenceUntil) {
        this.recurrenceUntil = recurrenceUntil;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public CalendarEventType getType() {
        return type;
    }

    public void setType(CalendarEventType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
