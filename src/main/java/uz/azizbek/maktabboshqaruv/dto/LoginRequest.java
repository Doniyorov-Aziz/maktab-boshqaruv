package uz.azizbek.maktabboshqaruv.dto;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank(message = "username kiritilishi shart")
    private String username;

    @NotBlank(message = "password kiritilishi shart")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}