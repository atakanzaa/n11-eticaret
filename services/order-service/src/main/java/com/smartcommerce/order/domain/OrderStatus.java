package com.smartcommerce.order.domain;

public enum OrderStatus {
    CREATED,
    FRAUD_FLAGGED,
    PAYMENT_PENDING,
    PAYMENT_FAILED,
    EXPIRED,
    CANCELLED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    COMPLETED,
    RETURN_REQUESTED,
    REFUNDED
}
