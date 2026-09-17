package uz.azizbek.maktabboshqaruv.service;

import uz.azizbek.maktabboshqaruv.dto.UserRequestDto;
import uz.azizbek.maktabboshqaruv.dto.UserResponseDto;
import uz.azizbek.maktabboshqaruv.dto.UserUpdateRequestDto;
import uz.azizbek.maktabboshqaruv.entity.Role;
import uz.azizbek.maktabboshqaruv.entity.User;
import uz.azizbek.maktabboshqaruv.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createUser_duplicateUsername_throws() {
        UserRequestDto request = new UserRequestDto();
        request.setUsername("admin");
        request.setPassword("password1");
        request.setRole(Role.ADMIN);
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_valid_hashesPasswordAndSaves() {
        UserRequestDto request = new UserRequestDto();
        request.setUsername("admin");
        request.setPassword("password1");
        request.setRole(Role.ADMIN);
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("password1")).thenReturn("hashed");

        User saved = new User();
        saved.setId(1L);
        saved.setUsername("admin");
        saved.setPassword("hashed");
        saved.setRole(Role.ADMIN);
        when(userRepository.save(any(User.class))).thenReturn(saved);

        UserResponseDto result = userService.createUser(request);

        assertEquals("admin", result.getUsername());
        assertEquals(Role.ADMIN, result.getRole());
        verify(passwordEncoder).encode("password1");
    }

    @Test
    void updateUser_omittedPassword_keepsExisting() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setPassword("oldHash");
        existing.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenReturn(existing);

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("admin");
        request.setRole(Role.EDITOR);

        userService.updateUser(1L, request);

        assertEquals("oldHash", existing.getPassword());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void updateUser_newPassword_reHashes() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setPassword("oldHash");
        existing.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");
        when(userRepository.save(any(User.class))).thenReturn(existing);

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("admin");
        request.setRole(Role.ADMIN);
        request.setPassword("newPassword");

        userService.updateUser(1L, request);

        assertEquals("newHash", existing.getPassword());
    }

    @Test
    void updateUser_changedToDuplicateUsername_throws() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("admin");
        existing.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsername("editor")).thenReturn(true);

        UserUpdateRequestDto request = new UserUpdateRequestDto();
        request.setUsername("editor");
        request.setRole(Role.ADMIN);

        assertThrows(IllegalStateException.class, () -> userService.updateUser(1L, request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_self_throws() {
        User existing = new User();
        existing.setId(1L);
        existing.setUsername("admin");
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        assertThrows(IllegalStateException.class, () -> userService.deleteUser(1L));
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void deleteUser_otherUser_deletes() {
        User existing = new User();
        existing.setId(2L);
        existing.setUsername("editor");
        when(userRepository.findById(2L)).thenReturn(Optional.of(existing));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null, List.of()));

        userService.deleteUser(2L);

        verify(userRepository).deleteById(2L);
    }

    @Test
    void deleteUser_notFound_throws() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> userService.deleteUser(1L));
    }
}
