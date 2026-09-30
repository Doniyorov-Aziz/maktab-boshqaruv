package uz.azizbek.maktabboshqaruv.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * The small slice of the Bot API this project uses, as plain records. Unknown
 * fields are ignored so new Telegram fields never break deserialization.
 */
public final class TelegramModels {

    private TelegramModels() {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiResponse<T>(
            boolean ok,
            T result,
            @JsonProperty("error_code") Integer errorCode,
            String description,
            ResponseParameters parameters) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ResponseParameters(@JsonProperty("retry_after") Integer retryAfter) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Update(@JsonProperty("update_id") long updateId, Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("message_id") Long messageId,
            User from,
            Chat chat,
            String text,
            Contact contact) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(Long id, @JsonProperty("is_bot") Boolean isBot,
                       @JsonProperty("first_name") String firstName, String username) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(Long id, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(@JsonProperty("phone_number") String phoneNumber,
                          @JsonProperty("first_name") String firstName,
                          @JsonProperty("user_id") Long userId) {
    }

    // ---- outgoing ----

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SendMessageRequest(
            @JsonProperty("chat_id") long chatId,
            String text,
            @JsonProperty("parse_mode") String parseMode,
            @JsonProperty("disable_web_page_preview") Boolean disableWebPagePreview,
            @JsonProperty("reply_markup") Object replyMarkup) {
    }

    public record KeyboardButton(String text, @JsonProperty("request_contact") Boolean requestContact) {
    }

    public record ReplyKeyboardMarkup(
            List<List<KeyboardButton>> keyboard,
            @JsonProperty("resize_keyboard") Boolean resizeKeyboard,
            @JsonProperty("one_time_keyboard") Boolean oneTimeKeyboard) {
    }

    public record ReplyKeyboardRemove(@JsonProperty("remove_keyboard") Boolean removeKeyboard) {
    }

    public static ReplyKeyboardMarkup shareContactKeyboard() {
        return new ReplyKeyboardMarkup(
                List.of(List.of(new KeyboardButton("📱 Raqamni ulashish", true))), true, true);
    }

    public static ReplyKeyboardRemove removeKeyboard() {
        return new ReplyKeyboardRemove(true);
    }
}
