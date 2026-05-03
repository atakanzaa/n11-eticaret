package com.smartcommerce.user.api.dto;

import java.time.Instant;
import java.util.UUID;

public record FavouriteResponse(
    UUID id,
    UUID productId,
    Instant addedAt
) {}
