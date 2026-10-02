package uz.azizbek.maktabboshqaruv.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * The slice of the Bot API this project uses, as plain records. Unknown
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
    public record Update(@JsonProperty("update_id") long updateId, Message message,
                         @JsonProperty("callback_query") CallbackQuery callbackQuery) {
        public Update(long updateId, Message message) {
            this(updateId, message, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("message_id") Long messageId,
            User from,
            Chat chat,
            String text,
            Contact contact,
            List<PhotoSize> photo,
            String caption) {
        public Message(Long messageId, User from, Chat chat, String text, Contact contact) {
            this(messageId, from, chat, text, contact, null, null);
        }

        /** file_id of the largest photo size, or null. */
        public String largestPhotoId() {
            if (photo == null || photo.isEmpty()) return null;
            return photo.get(photo.size() - 1).fileId();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CallbackQuery(String id, User from, Message message, String data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(Long id, @JsonProperty("is_bot") Boolean isBot,
                       @JsonProperty("first_name") String firstName, String username,
                       @JsonProperty("language_code") String languageCode) {
        public User(Long id, Boolean isBot, String firstName, String username) {
            this(id, isBot, firstName, username, null);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Chat(Long id, String type) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Contact(@JsonProperty("phone_number") String phoneNumber,
                          @JsonProperty("first_name") String firstName,
                          @JsonProperty("user_id") Long userId) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PhotoSize(@JsonProperty("file_id") String fileId, Integer width, Integer height) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TgFile(@JsonProperty("file_id") String fileId, @JsonProperty("file_path") String filePath) {
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

    /**
     * Bottom-keyboard button. {@code style} (Bot API 9.4): "primary" (blue), "success" (green) or
     * "danger" (red); older Telegram apps ignore it and show a plain button.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record KeyboardButton(String text, @JsonProperty("request_contact") Boolean requestContact,
                                 @JsonProperty("web_app") WebAppInfo webApp, String style) {
        public KeyboardButton(String text, Boolean requestContact) {
            this(text, requestContact, null, null);
        }

        public static KeyboardButton of(String text) {
            return new KeyboardButton(text, null);
        }

        public static KeyboardButton styled(String text, String style) {
            return new KeyboardButton(text, null, null, style);
        }

        public static KeyboardButton webApp(String text, String url) {
            return new KeyboardButton(text, null, new WebAppInfo(url), "primary");
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ReplyKeyboardMarkup(
            List<List<KeyboardButton>> keyboard,
            @JsonProperty("resize_keyboard") Boolean resizeKeyboard,
            @JsonProperty("one_time_keyboard") Boolean oneTimeKeyboard,
            @JsonProperty("is_persistent") Boolean isPersistent,
            @JsonProperty("input_field_placeholder") String inputFieldPlaceholder) {
        public ReplyKeyboardMarkup(List<List<KeyboardButton>> keyboard, Boolean resizeKeyboard, Boolean oneTimeKeyboard) {
            this(keyboard, resizeKeyboard, oneTimeKeyboard, null, null);
        }
    }

    public record ReplyKeyboardRemove(@JsonProperty("remove_keyboard") Boolean removeKeyboard) {
    }

    public record WebAppInfo(String url) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InlineButton(String text,
                               @JsonProperty("callback_data") String callbackData,
                               String url,
                               @JsonProperty("web_app") WebAppInfo webApp,
                               String style) {
        public InlineButton(String text, String callbackData, String url, WebAppInfo webApp) {
            this(text, callbackData, url, webApp, null);
        }

        public static InlineButton callback(String text, String data) {
            return new InlineButton(text, null == data ? "noop" : data, null, null);
        }

        public static InlineButton webApp(String text, String url) {
            return new InlineButton(text, null, null, new WebAppInfo(url), "primary");
        }

        /** Same button with a Bot API 9.4 color style ("primary", "success", "danger"). */
        public InlineButton styled(String style) {
            return new InlineButton(text, callbackData, url, webApp, style);
        }
    }

    public record InlineKeyboardMarkup(@JsonProperty("inline_keyboard") List<List<InlineButton>> inlineKeyboard) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private final List<List<InlineButton>> rows = new ArrayList<>();

            public Builder row(InlineButton... buttons) {
                List<InlineButton> row = new ArrayList<>();
                for (InlineButton b : buttons) {
                    if (b != null) row.add(b);
                }
                if (!row.isEmpty()) rows.add(row);
                return this;
            }

            public Builder rows(List<List<InlineButton>> more) {
                for (List<InlineButton> r : more) {
                    if (!r.isEmpty()) rows.add(r);
                }
                return this;
            }

            /** Lays buttons out in rows of {@code perRow}. */
            public Builder grid(List<InlineButton> buttons, int perRow) {
                List<InlineButton> row = new ArrayList<>();
                for (InlineButton b : buttons) {
                    row.add(b);
                    if (row.size() == perRow) {
                        rows.add(row);
                        row = new ArrayList<>();
                    }
                }
                if (!row.isEmpty()) rows.add(row);
                return this;
            }

            public InlineKeyboardMarkup build() {
                return new InlineKeyboardMarkup(rows);
            }
        }
    }

    public record BotCommand(String command, String description) {
    }

    public record MenuButtonWebApp(String type, String text, @JsonProperty("web_app") WebAppInfo webApp) {
        public MenuButtonWebApp(String text, String url) {
            this("web_app", text, new WebAppInfo(url));
        }
    }

    public record MenuButtonCommands(String type) {
        public MenuButtonCommands() {
            this("commands");
        }
    }

    public static ReplyKeyboardMarkup shareContactKeyboard(String label) {
        return new ReplyKeyboardMarkup(
                List.of(List.of(new KeyboardButton(label, true))), true, true);
    }

    public static ReplyKeyboardMarkup shareContactKeyboard() {
        return shareContactKeyboard("📱 Raqamni ulashish");
    }

    public static ReplyKeyboardRemove removeKeyboard() {
        return new ReplyKeyboardRemove(true);
    }
}
