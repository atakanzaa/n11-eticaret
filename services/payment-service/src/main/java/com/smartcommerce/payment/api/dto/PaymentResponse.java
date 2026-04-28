package com.smartcommerce.payment.api.dto;

import com.smartcommerce.payment.domain.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
    UUID id,
    UUID orderId,
    UUID userId,
    BigDecimal amount,
    BigDecimal paidAmount,
    BigDecimal refundedAmount,
    String currency,
    Integer installment,
    String provider,
    PaymentStatus status,
    String cardLastFour,
    String cardBrand,
    Instant initiatedAt,
    Instant succeededAt,
    Instant failedAt,
    String failureCode,
    String failureMessage
) {
}
