package uz.azizbek.maktabboshqaruv.dto;

public class AttentionItemDto {

    private String type;
    private String description;
    private String linkModule;
    private Long linkId;

    public AttentionItemDto() {
    }

    public AttentionItemDto(String type, String description, String linkModule, Long linkId) {
        this.type = type;
        this.description = description;
        this.linkModule = linkModule;
        this.linkId = linkId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLinkModule() {
        return linkModule;
    }

    public void setLinkModule(String linkModule) {
        this.linkModule = linkModule;
    }

    public Long getLinkId() {
        return linkId;
    }

    public void setLinkId(Long linkId) {
        this.linkId = linkId;
    }
}
