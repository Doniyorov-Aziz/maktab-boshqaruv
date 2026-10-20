package uz.azizbek.maktabboshqaruv.dto;

public class SchoolResponseDto {
    private Long id;
    private String name;
    private String address;
    private Long studentCount;
    private Long teacherCount;
    private Long classCount;
    private String color;
    /** The card's numbers: teachers, today's attendance, quality, Telegram, director, 30-day run. */
    private uz.azizbek.maktabboshqaruv.service.pages.SchoolStatsService.SchoolCard card;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public uz.azizbek.maktabboshqaruv.service.pages.SchoolStatsService.SchoolCard getCard() {
        return card;
    }

    public void setCard(uz.azizbek.maktabboshqaruv.service.pages.SchoolStatsService.SchoolCard card) {
        this.card = card;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Long studentCount) {
        this.studentCount = studentCount;
    }

    public Long getTeacherCount() {
        return teacherCount;
    }

    public void setTeacherCount(Long teacherCount) {
        this.teacherCount = teacherCount;
    }

    public Long getClassCount() {
        return classCount;
    }

    public void setClassCount(Long classCount) {
        this.classCount = classCount;
    }

}