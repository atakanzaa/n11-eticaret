package com.smartcommerce.returns.domain;

public enum ReturnStatus {
    REQUESTED,
    APPROVED,
    REJECTED,
    REFUND_PROCESSING,
    REFUND_FAILED,
    REFUNDED,
    INVENTORY_RESTOCKED,
    COMPLETED,
    CANCELLED
}
