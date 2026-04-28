package com.smartcommerce.payment.api.dto;

import java.util.UUID;

public record InitiatePaymentResponse(
    UUID paymentId,
    String threeDsHtmlContent,
    String status
) {
}
