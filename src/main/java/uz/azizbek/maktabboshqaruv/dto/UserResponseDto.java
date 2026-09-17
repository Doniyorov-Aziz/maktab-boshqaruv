package uz.azizbek.maktabboshqaruv.dto;

import uz.azizbek.maktabboshqaruv.entity.Role;

public class UserResponseDto {

    private Long id;
    private String username;
    private Role role;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}
