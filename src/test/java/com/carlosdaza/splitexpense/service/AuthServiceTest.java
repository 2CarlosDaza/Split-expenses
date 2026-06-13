package com.carlosdaza.splitexpense.service;

import com.carlosdaza.splitexpense.domain.dto.SplitDto;
import com.carlosdaza.splitexpense.domain.entity.User;
import com.carlosdaza.splitexpense.exception.BusinessException;
import com.carlosdaza.splitexpense.repository.UserRepository;
import com.carlosdaza.splitexpense.security.jwt.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @InjectMocks private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(UUID.randomUUID())
                .name("Carlos Daza")
                .email("carlos@example.com")
                .password("hashed_password")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Should register a new user and return auth response")
    void register_shouldCreateUserAndReturnToken() {
        SplitDto.RegisterRequest request = new SplitDto.RegisterRequest(
                "Carlos Daza", "carlos@example.com", "password123");

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(existingUser);
        when(jwtUtil.generateToken(existingUser.getEmail())).thenReturn("jwt_token");

        SplitDto.AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("jwt_token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.email()).isEqualTo("carlos@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should throw BusinessException when email already registered")
    void register_shouldThrowWhenEmailExists() {
        SplitDto.RegisterRequest request = new SplitDto.RegisterRequest(
                "Carlos Daza", "carlos@example.com", "password123");

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should login and return token for valid credentials")
    void login_shouldReturnTokenForValidCredentials() {
        SplitDto.LoginRequest request = new SplitDto.LoginRequest(
                "carlos@example.com", "password123");

        when(userRepository.findByEmail(request.email())).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken(existingUser.getEmail())).thenReturn("jwt_token");

        SplitDto.AuthResponse response = authService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt_token");
        assertThat(response.email()).isEqualTo("carlos@example.com");
        verify(authenticationManager).authenticate(any());
    }
}
