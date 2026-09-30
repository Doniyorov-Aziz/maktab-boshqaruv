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
    /** Simulates tapping an inline button with this callback_data. */
    private String callbackData;
    /** The message the tapped button belongs to (as returned by a previous simulate call). */
    private Long messageId;
    /** Simulates sending a photo (base64 PNG/JPEG); text becomes its caption. */
    private String photoBase64;
    private String languageCode;

    public String getCallbackData() {
        return callbackData;
    }

    public void setCallbackData(String callbackData) {
        this.callbackData = callbackData;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getPhotoBase64() {
        return photoBase64;
    }

    public void setPhotoBase64(String photoBase64) {
        this.photoBase64 = photoBase64;
    }

    public String getLanguageCode() {
        return languageCode;
    }

    public void setLanguageCode(String languageCode) {
        this.languageCode = languageCode;
    }

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
