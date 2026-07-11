package org.example.quoraproject.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.quoraproject.dto.request.LoginRequest;
import org.example.quoraproject.dto.request.RefreshTokenRequest;
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
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication workflows: signup, login, token refresh and logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("Email is already registered");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setEmail(request.email());
        // Passwords are only ever stored as BCrypt hashes.
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setRole(Role.USER);

        User saved = userRepository.save(user);
        log.info("New user registered with id {}", saved.getId());
        return userMapper.toResponse(saved);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Delegates credential verification (BCrypt comparison) to Spring
        // Security; throws BadCredentialsException on failure, translated to
        // a generic 401 by the GlobalExceptionHandler.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));

        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByUsername(principal.getUsername()).orElseThrow();

        String accessToken = jwtService.generateAccessToken(principal);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User id {} logged in", user.getId());
        return AuthResponse.of(accessToken, refreshToken.getToken(),
                jwtService.getAccessTokenExpirationMs(), userMapper.toResponse(user));
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken current = refreshTokenService.validate(request.refreshToken());
        RefreshToken rotated = refreshTokenService.rotate(current);

        User user = rotated.getUser();
        String accessToken = jwtService.generateAccessToken(new CustomUserDetails(user));

        return AuthResponse.of(accessToken, rotated.getToken(),
                jwtService.getAccessTokenExpirationMs(), userMapper.toResponse(user));
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.refreshToken());
        log.info("Refresh token revoked on logout");
    }
}
