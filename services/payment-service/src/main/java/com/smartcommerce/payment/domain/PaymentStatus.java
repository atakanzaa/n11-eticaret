package com.smartcommerce.payment.domain;

public enum PaymentStatus {
    INITIATED,
    THREEDS_PENDING,
    THREEDS_AUTHENTICATED,
    CAPTURING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    REFUND_REQUESTED,
    PARTIALLY_REFUNDED,
    REFUNDED
}
