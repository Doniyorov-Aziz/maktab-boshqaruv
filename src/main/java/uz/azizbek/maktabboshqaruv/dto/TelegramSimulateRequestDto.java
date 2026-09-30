package uz.azizbek.maktabboshqaruv.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Mock mode only: a fake incoming Telegram message, so the whole linking flow
 * (deep-link code or phone contact) can be exercised without a real bot.
 */
public class TelegramSimulateRequestDto {

    @NotNull
    private Long chatId;
    private String firstName;
    private String username;
    private String text;
    private String contactPhone;
    /** Owner of the shared contact; defaults to chatId (i.e. the sender's own number). */
    private Long contactUserId;

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public Long getContactUserId() {
        return contactUserId;
    }

    public void setContactUserId(Long contactUserId) {
        this.contactUserId = contactUserId;
    }
}
