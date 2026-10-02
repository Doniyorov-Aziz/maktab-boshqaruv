package uz.azizbek.maktabboshqaruv.telegram;

import java.util.List;

/**
 * The Bot API calls the app needs. Implementations throw
 * {@link TelegramApiException} on failure; the token never appears in its message.
 */
public interface TelegramClient {

    /** Result of sending a photo: the new message id and Telegram's reusable file_id. */
    record SentPhoto(Long messageId, String fileId) {
    }

    /** Sends an HTML-formatted message; {@code replyMarkup} may be null or a pre-serialized JSON string. Returns the message id. */
    Long sendMessage(long chatId, String html, Object replyMarkup);

    /** Long-polls for updates with id >= offset, waiting up to timeoutSeconds. */
    List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds);

    default void editMessageText(long chatId, long messageId, String html, Object replyMarkup) {
        throw new TelegramApiException(0, "editMessageText qo'llab-quvvatlanmaydi", null);
    }

    default SentPhoto sendPhoto(long chatId, byte[] png, String fileName, String captionHtml, Object replyMarkup) {
        throw new TelegramApiException(0, "sendPhoto qo'llab-quvvatlanmaydi", null);
    }

    default SentPhoto sendPhotoById(long chatId, String fileId, String captionHtml, Object replyMarkup) {
        throw new TelegramApiException(0, "sendPhoto qo'llab-quvvatlanmaydi", null);
    }

    /**
     * Replaces the photo of an existing photo message (and its caption/buttons) in place.
     * Uploads {@code png} when {@code fileId} is null; returns the new photo's file_id.
     */
    default SentPhoto editMessageMedia(long chatId, long messageId, String fileId, byte[] png, String fileName,
                                       String captionHtml, Object replyMarkup) {
        throw new TelegramApiException(0, "editMessageMedia qo'llab-quvvatlanmaydi", null);
    }

    /** Changes only the caption and buttons of a photo message. */
    default void editMessageCaption(long chatId, long messageId, String captionHtml, Object replyMarkup) {
        throw new TelegramApiException(0, "editMessageCaption qo'llab-quvvatlanmaydi", null);
    }

    default void deleteMessage(long chatId, long messageId) {
    }

    default void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
    }

    /** "typing", "upload_photo", ... */
    default void sendChatAction(long chatId, String action) {
    }

    default void setMyCommands(List<TelegramModels.BotCommand> commands, String languageCode) {
    }

    default void setMyDescription(String description, String languageCode) {
    }

    default void setMyShortDescription(String description, String languageCode) {
    }

    default void setChatMenuButton(Object menuButton) {
    }

    /** Downloads a file the user sent (photo attachment). */
    default byte[] downloadFile(String fileId) {
        throw new TelegramApiException(404, "Fayl topilmadi", null);
    }
}
