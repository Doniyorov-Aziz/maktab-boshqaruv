package uz.azizbek.maktabboshqaruv.telegram;

/**
 * A failed Bot API call. {@code errorCode} mirrors Telegram's error_code
 * (403 = bot blocked by the user, 429 = flood limit with {@code retryAfter}
 * seconds); network failures use code 0. The message is always token-masked.
 */
public class TelegramApiException extends RuntimeException {

    private final int errorCode;
    private final Integer retryAfter;

    public TelegramApiException(int errorCode, String description, Integer retryAfter) {
        super(TokenMasker.mask(description));
        this.errorCode = errorCode;
        this.retryAfter = retryAfter;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Integer getRetryAfter() {
        return retryAfter;
    }

    public boolean isBlocked() {
        return errorCode == 403;
    }

    public boolean isFloodLimit() {
        return errorCode == 429;
    }
}
