package uz.azizbek.maktabboshqaruv.telegram;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Real Bot API client on top of Spring's RestClient — no third-party Telegram
 * library. HTTP error statuses are not thrown by RestClient here; the JSON
 * body (ok/error_code/description/retry_after) is parsed instead, because
 * that is where Telegram puts the details callers need.
 */
public class HttpTelegramClient implements TelegramClient {

    private static final ParameterizedTypeReference<TelegramModels.ApiResponse<Object>> ANY_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<TelegramModels.ApiResponse<TelegramModels.Message>> MESSAGE_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<TelegramModels.ApiResponse<List<TelegramModels.Update>>> UPDATES_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<TelegramModels.ApiResponse<TelegramModels.TgFile>> FILE_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final RestClient fileClient;
    private final String token;

    public HttpTelegramClient(TelegramProperties properties) {
        this.token = properties.getBotToken();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        // Must outlast the long-poll timeout, or every idle getUpdates would fail.
        factory.setReadTimeout(Duration.ofSeconds(properties.getPollTimeoutSeconds() + 15L));
        this.restClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl() + "/bot" + token)
                .requestFactory(factory)
                .build();
        this.fileClient = RestClient.builder()
                .baseUrl(properties.getApiBaseUrl() + "/file/bot" + token)
                .requestFactory(factory)
                .build();
    }

    @Override
    public Long sendMessage(long chatId, String html, Object replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", html);
        body.put("parse_mode", "HTML");
        body.put("disable_web_page_preview", true);
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);
        TelegramModels.Message m = postJson("/sendMessage", body, MESSAGE_TYPE).result();
        return m != null ? m.messageId() : null;
    }

    @Override
    public void editMessageText(long chatId, long messageId, String html, Object replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("message_id", messageId);
        body.put("text", html);
        body.put("parse_mode", "HTML");
        body.put("disable_web_page_preview", true);
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);
        postJson("/editMessageText", body, ANY_TYPE);
    }

    @Override
    public SentPhoto sendPhoto(long chatId, byte[] png, String fileName, String captionHtml, Object replyMarkup) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        form.add("chat_id", String.valueOf(chatId));
        form.add("photo", new ByteArrayResource(png) {
            @Override
            public String getFilename() {
                return fileName;
            }
        });
        if (captionHtml != null) {
            form.add("caption", captionHtml);
            form.add("parse_mode", "HTML");
        }
        if (replyMarkup != null) form.add("reply_markup", TelegramJson.write(replyMarkup));
        TelegramModels.Message m = call(() -> restClient.post().uri("/sendPhoto")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { /* parsed from body */ })
                .body(MESSAGE_TYPE)).result();
        return toSentPhoto(m);
    }

    @Override
    public SentPhoto sendPhotoById(long chatId, String fileId, String captionHtml, Object replyMarkup) {
        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("photo", fileId);
        if (captionHtml != null) {
            body.put("caption", captionHtml);
            body.put("parse_mode", "HTML");
        }
        if (replyMarkup != null) body.put("reply_markup", replyMarkup);
        return toSentPhoto(postJson("/sendPhoto", body, MESSAGE_TYPE).result());
    }

    @Override
    public void deleteMessage(long chatId, long messageId) {
        postJson("/deleteMessage", Map.of("chat_id", chatId, "message_id", messageId), ANY_TYPE);
    }

    @Override
    public void answerCallbackQuery(String callbackQueryId, String text, boolean showAlert) {
        Map<String, Object> body = new HashMap<>();
        body.put("callback_query_id", callbackQueryId);
        if (text != null) body.put("text", text);
        body.put("show_alert", showAlert);
        postJson("/answerCallbackQuery", body, ANY_TYPE);
    }

    @Override
    public void sendChatAction(long chatId, String action) {
        postJson("/sendChatAction", Map.of("chat_id", chatId, "action", action), ANY_TYPE);
    }

    @Override
    public void setMyCommands(List<TelegramModels.BotCommand> commands, String languageCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("commands", commands);
        if (languageCode != null) body.put("language_code", languageCode);
        postJson("/setMyCommands", body, ANY_TYPE);
    }

    @Override
    public void setMyDescription(String description, String languageCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("description", description);
        if (languageCode != null) body.put("language_code", languageCode);
        postJson("/setMyDescription", body, ANY_TYPE);
    }

    @Override
    public void setMyShortDescription(String description, String languageCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("short_description", description);
        if (languageCode != null) body.put("language_code", languageCode);
        postJson("/setMyShortDescription", body, ANY_TYPE);
    }

    @Override
    public void setChatMenuButton(Object menuButton) {
        postJson("/setChatMenuButton", Map.of("menu_button", menuButton), ANY_TYPE);
    }

    @Override
    public byte[] downloadFile(String fileId) {
        TelegramModels.TgFile file = postJson("/getFile", Map.of("file_id", fileId), FILE_TYPE).result();
        if (file == null || file.filePath() == null) {
            throw new TelegramApiException(404, "Fayl topilmadi", null);
        }
        try {
            return fileClient.get().uri("/" + file.filePath()).retrieve().body(byte[].class);
        } catch (RuntimeException e) {
            throw new TelegramApiException(0, TokenMasker.mask(e.getMessage(), token), null);
        }
    }

    @Override
    public List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds) {
        Map<String, Object> body = Map.of(
                "offset", offset,
                "timeout", timeoutSeconds,
                "allowed_updates", List.of("message", "callback_query"));
        TelegramModels.ApiResponse<List<TelegramModels.Update>> response = postJson("/getUpdates", body, UPDATES_TYPE);
        return response.result() == null ? List.of() : response.result();
    }

    private static SentPhoto toSentPhoto(TelegramModels.Message m) {
        if (m == null) return new SentPhoto(null, null);
        return new SentPhoto(m.messageId(), m.largestPhotoId());
    }

    private <T> TelegramModels.ApiResponse<T> postJson(String method, Object body,
                                                      ParameterizedTypeReference<TelegramModels.ApiResponse<T>> type) {
        return call(() -> restClient.post().uri(method)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { /* parsed from body below */ })
                .body(type));
    }

    private <T> TelegramModels.ApiResponse<T> call(Supplier<TelegramModels.ApiResponse<T>> request) {
        TelegramModels.ApiResponse<T> response;
        try {
            response = request.get();
        } catch (RuntimeException e) {
            // Network / parse failure. The exception text can quote the URL,
            // which contains the token — mask before it goes anywhere.
            throw new TelegramApiException(0, TokenMasker.mask(e.getMessage(), token), null);
        }
        if (response == null) {
            throw new TelegramApiException(0, "Telegram bo'sh javob qaytardi", null);
        }
        if (!response.ok()) {
            Integer retryAfter = response.parameters() != null ? response.parameters().retryAfter() : null;
            int code = response.errorCode() != null ? response.errorCode() : 0;
            throw new TelegramApiException(code, TokenMasker.mask(response.description(), token), retryAfter);
        }
        return response;
    }
}
