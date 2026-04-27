package com.smartcommerce.auth.api.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    Long expiresInSeconds,
    UserDto user
) {}
