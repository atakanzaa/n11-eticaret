package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.Size;

public record ModerationActionRequest(
    @Size(max = 500) String rejectionReason
) {}
