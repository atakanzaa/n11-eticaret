package com.smartcommerce.product.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewReplyResponse(
    UUID id,
    UUID sellerId,
    String content,
    Instant createdAt,
    Instant updatedAt
) {}
