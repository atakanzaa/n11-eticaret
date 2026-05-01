package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequest(
    @Min(1) @Max(5) Integer rating,
    @Size(max = 200) String title,
    @Size(max = 4000) String comment
) {}
