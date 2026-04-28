package com.smartcommerce.returns.api.dto;

import com.smartcommerce.returns.domain.ReturnStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReturnResponse(
    UUID id,
    String returnNumber,
    UUID orderId,
    UUID userId,
    UUID paymentId,
    String reasonCode,
    String reasonDescription,
    ReturnStatus status,
    String sagaState,
    BigDecimal refundAmount,
    UUID refundId,
    String rejectionReason,
    Instant requestedAt,
    Instant approvedAt,
    Instant rejectedAt,
    Instant refundedAt,
    Instant completedAt,
    List<ReturnItemResponse> items
) {
    public record ReturnItemResponse(
        UUID id,
        UUID offerId,
        UUID productId,
        UUID sellerId,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal refundAmount,
        String itemStatus
    ) {}
}
