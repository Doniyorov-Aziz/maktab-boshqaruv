package uz.azizbek.maktabboshqaruv.dto;

import java.util.List;

public class StudentTelegramDto {

    private Long studentId;
    private String studentName;
    private String className;
    private String linkCode;
    /** t.me deep link, or null while the bot username is not configured. */
    private String deepLink;
    private Integer linkedCount;
    private List<ParentLinkDto> links;

    public Long getStudentId() {
        return studentId;
    }

    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getLinkCode() {
        return linkCode;
    }

    public void setLinkCode(String linkCode) {
        this.linkCode = linkCode;
    }

    public String getDeepLink() {
        return deepLink;
    }

    public void setDeepLink(String deepLink) {
        this.deepLink = deepLink;
    }

    public Integer getLinkedCount() {
        return linkedCount;
    }

    public void setLinkedCount(Integer linkedCount) {
        this.linkedCount = linkedCount;
    }

    public List<ParentLinkDto> getLinks() {
        return links;
    }

    public void setLinks(List<ParentLinkDto> links) {
        this.links = links;
    }
}
