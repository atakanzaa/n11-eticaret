package com.smartcommerce.auth.service;

import com.smartcommerce.auth.api.dto.*;
import com.smartcommerce.auth.api.mapper.UserMapper;
import com.smartcommerce.auth.domain.*;
import com.smartcommerce.auth.event.outbox.OutboxService;
import com.smartcommerce.auth.repository.*;
import com.smartcommerce.common.errors.*;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.common.events.payload.UserRegisteredPayload;
import com.smartcommerce.common.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OutboxService outboxService;
    private final UserMapper userMapper;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE,
                "Email already registered: " + request.email());
        }

        var roles = request.roles().stream()
            .map(name -> roleRepository.findByName(name.name())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                    "Role not found: " + name)))
            .collect(Collectors.toSet());

        var user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .firstName(request.firstName())
            .lastName(request.lastName())
            .phone(request.phone())
            .status(UserStatus.ACTIVE)
            .roles(roles)
            .build();
        user = userRepository.save(user);
        log.info("User registered: {}", user.getId());

        var payload = UserRegisteredPayload.builder()
            .userId(user.getId())
            .email(user.getEmail())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .roles(roles.stream().map(Role::getName).collect(Collectors.toSet()))
            .build();
        outboxService.publish(Topics.USER_REGISTERED, EventType.USER_REGISTERED,
            user.getId().toString(), "USER", payload);

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS,
                HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, HttpStatus.FORBIDDEN,
                "Account is " + user.getStatus());
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, HttpStatus.FORBIDDEN,
                "Account locked until " + user.getLockedUntil());
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            handleFailedLogin(user);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                "Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        log.info("User logged in: {}", user.getId());

        return generateAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        if (!jwtService.isTokenValid(request.refreshToken())) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED,
                "Invalid refresh token");
        }

        var claims = jwtService.parseToken(request.refreshToken());
        if (!"REFRESH".equals(claims.get("type"))) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED,
                "Not a refresh token");
        }

        var familyId = UUID.fromString((String) claims.get("familyId"));
        var tokenHash = hashToken(request.refreshToken());

        var existing = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> {
                refreshTokenRepository.revokeFamily(familyId, Instant.now());
                return new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED, HttpStatus.UNAUTHORIZED,
                    "Refresh token reuse detected, family revoked");
            });

        if (existing.isUsed() || existing.isRevoked() || existing.isExpired()) {
            refreshTokenRepository.revokeFamily(familyId, Instant.now());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_REUSED, HttpStatus.UNAUTHORIZED,
                "Refresh token already used or revoked");
        }

        var user = userRepository.findById(existing.getUserId())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        existing.setUsedAt(Instant.now());
        var newRefreshTokenId = UUID.randomUUID();
        existing.setReplacedBy(newRefreshTokenId);
        refreshTokenRepository.save(existing);

        return generateAuthResponseWithFamily(user, familyId, newRefreshTokenId);
    }

    @Transactional
    public void logout(String refreshToken) {
        var tokenHash = hashToken(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED,
                "Mevcut şifre hatalı");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.BAD_REQUEST,
                "Yeni şifre mevcut şifre ile aynı olamaz");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        log.info("Password changed for user {}", userId);
    }

    @Transactional
    public AuthResponse becomeSeller(UUID userId) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "User not found"));

        var alreadySeller = user.getRoles().stream()
            .anyMatch(r -> RoleName.SELLER.name().equals(r.getName()));
        if (alreadySeller) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.CONFLICT,
                "Zaten satıcı hesabınız var");
        }

        var sellerRole = roleRepository.findByName(RoleName.SELLER.name())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,
                "SELLER role not configured"));
        user.getRoles().add(sellerRole);
        user = userRepository.save(user);
        log.info("User {} promoted to SELLER", userId);

        return generateAuthResponse(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        return generateAuthResponseWithFamily(user, UUID.randomUUID(), UUID.randomUUID());
    }

    private AuthResponse generateAuthResponseWithFamily(User user, UUID familyId, UUID refreshTokenId) {
        var roleNames = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        var accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), roleNames);
        var refreshTokenStr = jwtService.generateRefreshToken(user.getId(), refreshTokenId, familyId);

        var refreshTokenEntity = RefreshToken.builder()
            .id(refreshTokenId)
            .userId(user.getId())
            .familyId(familyId)
            .tokenHash(hashToken(refreshTokenStr))
            .expiresAt(Instant.now().plus(Duration.ofDays(7)))
            .build();
        refreshTokenRepository.save(refreshTokenEntity);

        return new AuthResponse(accessToken, refreshTokenStr, 900L, userMapper.toDto(user));
    }

    private void handleFailedLogin(User user) {
        user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);
        if (user.getFailedLoginAttempts() >= 5) {
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
            log.warn("Account locked due to brute force: {}", user.getEmail());
        }
        userRepository.save(user);
    }

    private String hashToken(String token) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
