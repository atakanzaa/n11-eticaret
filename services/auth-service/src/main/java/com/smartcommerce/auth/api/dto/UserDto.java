package com.smartcommerce.auth.api.dto;

import java.util.Set;
import java.util.UUID;

public record UserDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    Set<String> roles
) {}
