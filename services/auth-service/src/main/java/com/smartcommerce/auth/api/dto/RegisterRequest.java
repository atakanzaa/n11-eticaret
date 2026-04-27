package com.smartcommerce.auth.api.dto;

import com.smartcommerce.auth.domain.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank String firstName,
    @NotBlank String lastName,
    String phone,
    @NotEmpty Set<RoleName> roles
) {}
