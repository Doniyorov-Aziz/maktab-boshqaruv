package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalTime;

public class LiveLessonDto {

    private Long lessonSlotId;
    private String className;
    private String subjectName;
    private String teacherName;
    private String roomNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private String status;
    private Long minutesUntilOrRemaining;

    public Long getLessonSlotId() {
        return lessonSlotId;
    }

    public void setLessonSlotId(Long lessonSlotId) {
        this.lessonSlotId = lessonSlotId;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getMinutesUntilOrRemaining() {
        return minutesUntilOrRemaining;
    }

    public void setMinutesUntilOrRemaining(Long minutesUntilOrRemaining) {
        this.minutesUntilOrRemaining = minutesUntilOrRemaining;
    }
}
