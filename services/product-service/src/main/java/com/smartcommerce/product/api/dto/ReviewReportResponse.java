package com.smartcommerce.product.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ReviewReportResponse(
    UUID id,
    UUID reviewId,
    UUID userId,
    String reason,
    String description,
    String status,
    Instant createdAt
) {}
