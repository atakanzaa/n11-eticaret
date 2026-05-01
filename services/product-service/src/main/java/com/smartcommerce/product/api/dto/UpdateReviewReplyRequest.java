package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateReviewReplyRequest(
    @NotBlank @Size(max = 4000) String content
) {}
