package com.freelanceflow.service;

import com.freelanceflow.dto.AuthResponse;
import com.freelanceflow.dto.LoginRequest;
import com.freelanceflow.dto.RegisterRequest;
import com.freelanceflow.dto.UserSummaryResponse;
import com.freelanceflow.exception.BadRequestException;
import com.freelanceflow.exception.ConflictException;
import com.freelanceflow.exception.ForbiddenException;
import com.freelanceflow.exception.UnauthorizedException;
import com.freelanceflow.model.RefreshToken;
import com.freelanceflow.model.Role;
import com.freelanceflow.model.User;
import com.freelanceflow.repository.RefreshTokenRepository;
import com.freelanceflow.repository.UserRepository;
import com.freelanceflow.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == Role.ADMIN) {
            throw new BadRequestException("Registration as ADMIN is not allowed.");
        }

        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new ConflictException("User with email " + request.getEmail() + " already exists.");
        }

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().toLowerCase().trim())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .skills(request.getSkills() != null ? request.getSkills() : java.util.Collections.emptyList())
                .bio(request.getBio())
                .companyName(request.getCompanyName())
                .walletBalance(BigDecimal.ZERO)
                .ratingAvg(0.0)
                .ratingCount(0)
                .isBanned(false)
                .createdAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtUtils.generateAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole());
        RefreshToken refreshToken = createRefreshToken(savedUser.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(UserSummaryResponse.fromUser(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new ForbiddenException("Your account has been suspended by an administrator.");
        }

        String accessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        RefreshToken refreshToken = createRefreshToken(user.getId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .user(UserSummaryResponse.fromUser(user))
                .build();
    }

    public AuthResponse refreshAccessToken(String refreshTokenString) {
        RefreshToken token = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new UnauthorizedException("Refresh token has expired. Please login again.");
        }

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        if (Boolean.TRUE.equals(user.getIsBanned())) {
            throw new ForbiddenException("Your account has been suspended.");
        }

        String newAccessToken = jwtUtils.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(token.getToken())
                .user(UserSummaryResponse.fromUser(user))
                .build();
    }

    public void logout(String refreshTokenString) {
        if (refreshTokenString != null && !refreshTokenString.isBlank()) {
            refreshTokenRepository.deleteByToken(refreshTokenString);
        }
    }

    private RefreshToken createRefreshToken(String userId) {
        refreshTokenRepository.deleteByUserId(userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }
}
