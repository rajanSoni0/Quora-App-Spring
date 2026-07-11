package org.example.quoraproject.service;

import org.example.quoraproject.dto.request.LoginRequest;
import org.example.quoraproject.dto.request.SignupRequest;
import org.example.quoraproject.dto.response.AuthResponse;
import org.example.quoraproject.dto.response.UserResponse;
import org.example.quoraproject.entity.RefreshToken;
import org.example.quoraproject.entity.Role;
import org.example.quoraproject.entity.User;
import org.example.quoraproject.exception.DuplicateResourceException;
import org.example.quoraproject.jwt.JwtService;
import org.example.quoraproject.mapper.UserMapper;
import org.example.quoraproject.repository.UserRepository;
import org.example.quoraproject.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@example.com");
        user.setPassword("$2a$10$hash");
        user.setRole(Role.USER);
    }

    @Test
    @DisplayName("signup hashes the password and never persists the raw value")
    void signupHashesPassword() {
        SignupRequest request = new SignupRequest("alice", "alice@example.com", "Str0ng!Pass");
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Str0ng!Pass")).thenReturn("$2a$10$hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class)))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", "USER", Instant.now()));

        UserResponse response = authService.signup(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$10$hash");
        assertThat(captor.getValue().getPassword()).isNotEqualTo("Str0ng!Pass");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(response.username()).isEqualTo("alice");
    }

    @Test
    @DisplayName("signup rejects duplicate usernames without touching the database")
    void signupRejectsDuplicateUsername() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() ->
                authService.signup(new SignupRequest("alice", "alice@example.com", "Str0ng!Pass")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login returns access + refresh tokens for valid credentials")
    void loginReturnsTokens() {
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(CustomUserDetails.class))).thenReturn("access-token");
        when(jwtService.getAccessTokenExpirationMs()).thenReturn(900_000L);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token");
        refreshToken.setUser(user);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshToken);
        when(userMapper.toResponse(user))
                .thenReturn(new UserResponse(1L, "alice", "alice@example.com", "USER", Instant.now()));

        AuthResponse response = authService.login(new LoginRequest("alice", "Str0ng!Pass"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.refreshToken()).isEqualTo("refresh-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
    }
}
