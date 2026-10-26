package uz.azizbek.maktabboshqaruv.dto;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "username kiritilishi shart")
    private String username;

    @NotBlank(message = "password kiritilishi shart")
    private String password;

    /** Optional: the browser's demo visitor id, so an administrator's own demo visits are not counted. */
    private String visitorId;

    public String getVisitorId() { return visitorId; }
    public void setVisitorId(String visitorId) { this.visitorId = visitorId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}