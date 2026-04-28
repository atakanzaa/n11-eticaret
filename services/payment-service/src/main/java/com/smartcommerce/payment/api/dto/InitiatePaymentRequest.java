package com.smartcommerce.payment.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InitiatePaymentRequest(
    @NotNull UUID orderId,
    @NotNull @Valid CardDto card,
    @Min(1) Integer installment,
    String userIp
) {
    public InitiatePaymentRequest withUserIp(String ip) {
        return new InitiatePaymentRequest(orderId, card, installment, ip);
    }
}
