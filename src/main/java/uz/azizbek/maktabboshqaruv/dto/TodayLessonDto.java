package uz.azizbek.maktabboshqaruv.dto;

public class TodayLessonDto {

    private Long lessonSlotId;
    private Long schoolClassId;
    private String className;
    private String subjectName;
    private String teacherName;
    private String startTime;
    private String endTime;
    private String status;
    private boolean attendanceTaken;

    public Long getLessonSlotId() {
        return lessonSlotId;
    }

    public void setLessonSlotId(Long lessonSlotId) {
        this.lessonSlotId = lessonSlotId;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
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

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isAttendanceTaken() {
        return attendanceTaken;
    }

    public void setAttendanceTaken(boolean attendanceTaken) {
        this.attendanceTaken = attendanceTaken;
    }
}
