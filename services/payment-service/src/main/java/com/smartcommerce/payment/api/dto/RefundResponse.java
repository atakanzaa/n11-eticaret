package com.smartcommerce.payment.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record RefundResponse(
    UUID id,
    BigDecimal amount,
    String status
) {
}
