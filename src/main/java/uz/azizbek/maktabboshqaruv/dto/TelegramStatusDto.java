package uz.azizbek.maktabboshqaruv.dto;

public class TelegramStatusDto {

    /** DISABLED, NO_TOKEN, MOCK or LIVE. */
    private String mode;
    private boolean enabled;
    private boolean mock;
    private boolean tokenConfigured;
    /** True when messages are actually delivered (LIVE or MOCK). */
    private boolean active;
    private String botUsername;
    private String lastError;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
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

    public boolean isTokenConfigured() {
        return tokenConfigured;
    }

    public void setTokenConfigured(boolean tokenConfigured) {
        this.tokenConfigured = tokenConfigured;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public String getBotUsername() {
        return botUsername;
    }

    public void setBotUsername(String botUsername) {
        this.botUsername = botUsername;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }
}
