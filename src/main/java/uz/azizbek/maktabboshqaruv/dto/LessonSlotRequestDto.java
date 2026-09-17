package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public class LessonSlotRequestDto {

    @NotNull(message = "Sinf tanlanishi shart")
    private Long schoolClassId;

    @NotNull(message = "Fan tanlanishi shart")
    private Long subjectId;

    @NotNull(message = "O'qituvchi tanlanishi shart")
    private Long employeeId;

    @NotNull(message = "Xona tanlanishi shart")
    private Long roomId;

    @NotBlank(message = "Hafta kuni kiritilishi shart")
    private String weekday;

    @NotNull(message = "Boshlanish vaqti kiritilishi shart")
    private LocalTime startTime;

    @NotNull(message = "Tugash vaqti kiritilishi shart")
    private LocalTime endTime;

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getWeekday() {
        return weekday;
    }

    public void setWeekday(String weekday) {
        this.weekday = weekday;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }
}
