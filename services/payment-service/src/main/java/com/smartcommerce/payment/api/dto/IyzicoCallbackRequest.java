package com.smartcommerce.payment.api.dto;

public record IyzicoCallbackRequest(
    String status,
    String paymentId,
    String conversationData,
    String conversationId,
    String mdStatus
) {
}
