package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateReviewRequest(
    @Min(1) @Max(5) int rating,
    @Size(max = 200) String title,
    @Size(max = 4000) String comment,
    @Size(max = 500) String variantInfo,
    UUID orderId,
    @Size(max = 5) List<@NotBlank @Size(max = 1000) String> imageUrls
) {}
