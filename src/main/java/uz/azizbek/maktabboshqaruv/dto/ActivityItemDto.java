package uz.azizbek.maktabboshqaruv.dto;

import java.time.LocalDateTime;

public class ActivityItemDto {

    private String description;
    private String icon;
    private LocalDateTime timestamp;
    private String actorUsername;

    public ActivityItemDto() {
    }

    public ActivityItemDto(String description, String icon, LocalDateTime timestamp) {
        this.description = description;
        this.icon = icon;
        this.timestamp = timestamp;
    }

    public ActivityItemDto(String description, String icon, LocalDateTime timestamp, String actorUsername) {
        this.description = description;
        this.icon = icon;
        this.timestamp = timestamp;
        this.actorUsername = actorUsername;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public void setActorUsername(String actorUsername) {
        this.actorUsername = actorUsername;
    }
}
