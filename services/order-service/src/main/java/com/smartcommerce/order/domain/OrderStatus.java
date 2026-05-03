package com.smartcommerce.order.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
    REFUNDED;

    /**
     * Allowed forward transitions enforced by Order.transitionTo. Anything not
     * listed here is rejected — for example, CANCELLED → CONFIRMED would be a
     * data-integrity bug and is impossible by construction.
     */
    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(OrderStatus.class);
        ALLOWED.put(CREATED, EnumSet.of(FRAUD_FLAGGED, PAYMENT_PENDING, CANCELLED, EXPIRED));
        ALLOWED.put(FRAUD_FLAGGED, EnumSet.of(CANCELLED));
        ALLOWED.put(PAYMENT_PENDING, EnumSet.of(CONFIRMED, PAYMENT_FAILED, CANCELLED, EXPIRED));
        ALLOWED.put(PAYMENT_FAILED, EnumSet.of(CANCELLED));
        ALLOWED.put(EXPIRED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CONFIRMED, EnumSet.of(PROCESSING, SHIPPED, CANCELLED, REFUNDED));
        ALLOWED.put(PROCESSING, EnumSet.of(SHIPPED, CANCELLED, REFUNDED));
        ALLOWED.put(SHIPPED, EnumSet.of(DELIVERED, RETURN_REQUESTED));
        ALLOWED.put(DELIVERED, EnumSet.of(COMPLETED, RETURN_REQUESTED));
        ALLOWED.put(COMPLETED, EnumSet.of(RETURN_REQUESTED));
        ALLOWED.put(RETURN_REQUESTED, EnumSet.of(REFUNDED, COMPLETED));
        ALLOWED.put(REFUNDED, EnumSet.noneOf(OrderStatus.class));
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (this == target) return true;
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
