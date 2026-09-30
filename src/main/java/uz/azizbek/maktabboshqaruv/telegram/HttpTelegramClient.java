package uz.azizbek.maktabboshqaruv.telegram;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Real Bot API client on top of Spring's RestClient — no third-party Telegram
 * library. HTTP error statuses are not thrown by RestClient here; the JSON
 * body (ok/error_code/description/retry_after) is parsed instead, because
 * that is where Telegram puts the details the sender needs.
 */
public class HttpTelegramClient implements TelegramClient {

    private static final ParameterizedTypeReference<TelegramModels.ApiResponse<Object>> SEND_TYPE =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<TelegramModels.ApiResponse<List<TelegramModels.Update>>> UPDATES_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
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
    }

    @Override
    public void sendMessage(long chatId, String html, Object replyMarkup) {
        TelegramModels.SendMessageRequest body =
                new TelegramModels.SendMessageRequest(chatId, html, "HTML", true, replyMarkup);
        call(() -> restClient.post().uri("/sendMessage")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { /* parsed from body below */ })
                .body(SEND_TYPE));
    }

    @Override
    public List<TelegramModels.Update> getUpdates(long offset, int timeoutSeconds) {
        Map<String, Object> body = Map.of(
                "offset", offset,
                "timeout", timeoutSeconds,
                "allowed_updates", List.of("message"));
        TelegramModels.ApiResponse<List<TelegramModels.Update>> response = call(() -> restClient.post().uri("/getUpdates")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { /* parsed from body below */ })
                .body(UPDATES_TYPE));
        return response.result() == null ? List.of() : response.result();
    }

    private <T> TelegramModels.ApiResponse<T> call(java.util.function.Supplier<TelegramModels.ApiResponse<T>> request) {
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
