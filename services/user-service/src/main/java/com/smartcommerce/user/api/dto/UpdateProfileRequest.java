package com.smartcommerce.user.api.dto;

import com.smartcommerce.user.domain.Gender;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
    @Size(max = 100) String firstName,
    @Size(max = 100) String lastName,
    @Size(max = 20) String phone,
    LocalDate dateOfBirth,
    Gender gender,
    @Size(max = 500) String avatarUrl,
    @Size(max = 10) String preferredLanguage,
    @Size(max = 3) String preferredCurrency,
    Boolean marketingConsent
) {
}
