package com.smartcommerce.user.api.dto;

import com.smartcommerce.user.domain.Gender;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record UserProfileDto(
    UUID id,
    UUID userId,
    String email,
    String firstName,
    String lastName,
    String phone,
    LocalDate dateOfBirth,
    Gender gender,
    String avatarUrl,
    String preferredLanguage,
    String preferredCurrency,
    boolean marketingConsent,
    Instant marketingConsentAt,
    boolean kvkkConsent,
    Instant kvkkConsentAt,
    Long version
) {
}
