package uz.azizbek.maktabboshqaruv.telegram;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.LocalTime;

/**
 * All "telegram.*" settings. The token only ever comes from
 * application-local.properties or the TELEGRAM_BOT_TOKEN environment variable;
 * {@link #toString()} is overridden so it can never be printed by accident.
 */
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {

    public enum Mode { DISABLED, NO_TOKEN, MOCK, LIVE }

    private boolean enabled = false;
    private boolean mock = false;
    private String botToken = "";
    private String botUsername = "";
    private String apiBaseUrl = "https://api.telegram.org";
    private int pollTimeoutSeconds = 25;
    private int maxPerSecond = 25;
    private int maxAttempts = 3;
    private LocalTime quietHoursStart = LocalTime.of(22, 0);
    private LocalTime quietHoursEnd = LocalTime.of(7, 0);

    public Mode mode() {
        if (mock) return Mode.MOCK;
        if (!enabled) return Mode.DISABLED;
        if (!hasToken()) return Mode.NO_TOKEN;
        return Mode.LIVE;
    }

    /** True when messages can actually leave the outbox (real bot or mock). */
    public boolean isActive() {
        Mode m = mode();
        return m == Mode.LIVE || m == Mode.MOCK;
    }

    public boolean hasToken() {
        return botToken != null && !botToken.isBlank();
    }

    public String deepLink(String code) {
        if (botUsername == null || botUsername.isBlank() || code == null) return null;
        String name = botUsername.startsWith("@") ? botUsername.substring(1) : botUsername;
        return "https://t.me/" + name + "?start=" + code;
    }

    @Override
    public String toString() {
        return "TelegramProperties{mode=" + mode() + ", botUsername=" + botUsername
                + ", token=" + (hasToken() ? "***" : "<none>") + "}";
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMock() {
        return mock;
    }

    public void setMock(boolean mock) {
        this.mock = mock;
    }

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken == null ? "" : botToken.trim();
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername == null ? "" : botUsername.trim();
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public int getPollTimeoutSeconds() {
        return pollTimeoutSeconds;
    }

    public void setPollTimeoutSeconds(int pollTimeoutSeconds) {
        this.pollTimeoutSeconds = pollTimeoutSeconds;
    }

    public int getMaxPerSecond() {
        return maxPerSecond;
    }

    public void setMaxPerSecond(int maxPerSecond) {
        this.maxPerSecond = maxPerSecond;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalTime getQuietHoursStart() {
        return quietHoursStart;
    }

    public void setQuietHoursStart(LocalTime quietHoursStart) {
        this.quietHoursStart = quietHoursStart;
    }

    public LocalTime getQuietHoursEnd() {
        return quietHoursEnd;
    }

    public void setQuietHoursEnd(LocalTime quietHoursEnd) {
        this.quietHoursEnd = quietHoursEnd;
    }
}
