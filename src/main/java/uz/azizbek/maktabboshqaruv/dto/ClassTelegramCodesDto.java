package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

/** Everything the printable "QR codes for parents" sheet needs for one class. */
public class ClassTelegramCodesDto {

    private Long schoolClassId;
    private String className;
    private String schoolName;
    private String botUsername;
    private List<StudentTelegramDto> students;

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

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public List<StudentTelegramDto> getStudents() {
        return students;
    }

    public void setStudents(List<StudentTelegramDto> students) {
        this.students = students;
    }
}
