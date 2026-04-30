package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record CreateReviewRequest(
    @Min(1) @Max(5) int rating,
    @Size(max = 200) String title,
    @Size(max = 4000) String comment,
    @Size(max = 500) String variantInfo,
    UUID orderId
) {
}
