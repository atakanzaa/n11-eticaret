package com.smartcommerce.common.events.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredPayload {
    private UUID userId;
    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;
}
