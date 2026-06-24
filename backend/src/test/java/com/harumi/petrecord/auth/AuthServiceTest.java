package com.harumi.petrecord.auth;

import com.harumi.petrecord.auth.dto.AuthResponse;
import com.harumi.petrecord.auth.dto.LoginRequest;
import com.harumi.petrecord.auth.dto.RegisterRequest;
import com.harumi.petrecord.common.exception.DuplicateResourceException;
import com.harumi.petrecord.security.CurrentUser;
import com.harumi.petrecord.security.JwtService;
import com.harumi.petrecord.user.User;
import com.harumi.petrecord.user.UserRepository;
import com.harumi.petrecord.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;

    @InjectMocks AuthService authService;

    @BeforeEach
    void stubJwt() {
        lenient().when(jwtService.issueToken(any(CurrentUser.class), anyInt())).thenReturn("jwt-token");
        lenient().when(jwtService.getExpirationSeconds()).thenReturn(3600L);
    }

    @Test
    void registerHashesPasswordAndIssuesToken() {
        RegisterRequest req = new RegisterRequest("harumi", "harumi@example.com", "password123");
        when(userRepository.existsByUsername("harumi")).thenReturn(false);
        when(userRepository.existsByEmail("harumi@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(1L);
            return u;
        });

        AuthResponse response = authService.register(req);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        org.mockito.Mockito.verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getPasswordHash()).isNotEqualTo("password123");
        assertThat(saved.getRole()).isEqualTo(UserRole.USER);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(3600L);
        assertThat(response.user().email()).isEqualTo("harumi@example.com");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("harumi")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("harumi", "harumi@example.com", "password123")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByUsername("harumi")).thenReturn(false);
        when(userRepository.existsByEmail("harumi@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("harumi", "harumi@example.com", "password123")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void loginSuccess() {
        User user = User.builder()
                .id(1L)
                .username("harumi")
                .email("harumi@example.com")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build();
        when(userRepository.findByEmail("harumi@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);

        AuthResponse response = authService.login(new LoginRequest("harumi@example.com", "password123"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(1L);
    }

    @Test
    void loginUnknownEmailThrows() {
        when(userRepository.findByEmail(eq("nobody@example.com"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "pw")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void logoutBumpsTokenVersion() {
        authService.logout(5L);

        verify(userRepository).incrementTokenVersion(5L);
    }

    @Test
    void loginWrongPasswordThrows() {
        User user = User.builder()
                .id(1L)
                .email("harumi@example.com")
                .passwordHash("hashed")
                .username("harumi")
                .role(UserRole.USER)
                .build();
        when(userRepository.findByEmail("harumi@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("harumi@example.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);
    }
}
