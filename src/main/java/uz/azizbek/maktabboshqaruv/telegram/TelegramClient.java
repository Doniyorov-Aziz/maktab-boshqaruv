package uz.azizbek.maktabboshqaruv.telegram;

import java.util.List;

/** The two Bot API calls the app needs. Implementations throw {@link TelegramApiException} on failure. */
public interface TelegramClient {

    /** Sends an HTML-formatted message; {@code replyMarkup} may be null. */
    void sendMessage(long chatId, String html, Object replyMarkup);

    /** Long-polls for updates with id >= offset, waiting up to timeoutSeconds. */
    List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds);
}
