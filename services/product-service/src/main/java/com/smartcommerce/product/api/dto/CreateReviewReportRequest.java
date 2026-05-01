package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateReviewReportRequest(
    @NotBlank @Size(max = 50) String reason,
    @Size(max = 1000) String description
) {}
