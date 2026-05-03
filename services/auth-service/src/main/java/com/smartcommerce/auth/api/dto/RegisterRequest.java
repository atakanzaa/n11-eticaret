package com.smartcommerce.auth.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Public registration payload. Always creates a CUSTOMER. Any client-side
 * attempt to inject privileged fields like "roles" or "isAdmin" is silently
 * ignored at the JSON layer — see AuthServiceIT#register_whenSellerRoleSubmitted_thenCreatesCustomerOnly.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    String phone
) {}
