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
    /** Simulates any other file: VIDEO, VOICE, AUDIO, DOCUMENT or VIDEO_NOTE (text becomes its caption). */
    private String fileKind;
    private String fileBase64;
    private String fileName;
    private String mimeType;
    private Integer duration;
    /** The same value on several updates = one album. */
    private String mediaGroupId;

    public String getFileKind() {
        return fileKind;
    }

    public void setFileKind(String fileKind) {
        this.fileKind = fileKind;
    }

    public String getFileBase64() {
        return fileBase64;
    }

    public void setFileBase64(String fileBase64) {
        this.fileBase64 = fileBase64;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getMediaGroupId() {
        return mediaGroupId;
    }

    public void setMediaGroupId(String mediaGroupId) {
        this.mediaGroupId = mediaGroupId;
    }

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
