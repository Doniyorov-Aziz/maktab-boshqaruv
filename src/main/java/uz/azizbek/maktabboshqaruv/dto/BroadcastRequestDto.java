package uz.azizbek.maktabboshqaruv.dto;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class BroadcastRequestDto {

    @NotNull
    private Long schoolId;

    // may be empty when files are attached (checked in the service)
    @Size(max = 3500)
    private String text;

    @NotNull
    @Pattern(regexp = "^(ALL|CLASSES|PARENTS)$")
    private String audience;

    private List<Long> classIds;

    /** audience = PARENTS: the chosen parents (ParentTelegramLink ids; the chat id itself never leaves the server). */
    private List<Long> linkIds;

    public List<Long> getLinkIds() {
        return linkIds;
    }

    public void setLinkIds(List<Long> linkIds) {
        this.linkIds = linkIds;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public List<Long> getClassIds() {
        return classIds;
    }

    public void setClassIds(List<Long> classIds) {
        this.classIds = classIds;
    }
}
