package com.smartcommerce.payment.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RefundRequest(
    @Positive BigDecimal amount,
    @NotBlank String reason
) {
}
