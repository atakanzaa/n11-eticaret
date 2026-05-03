package com.smartcommerce.payment.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

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
    REFUNDED;

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(PaymentStatus.class);
        ALLOWED.put(INITIATED, EnumSet.of(THREEDS_PENDING, CAPTURING, SUCCEEDED, FAILED, CANCELLED));
        ALLOWED.put(THREEDS_PENDING, EnumSet.of(THREEDS_AUTHENTICATED, FAILED, CANCELLED));
        ALLOWED.put(THREEDS_AUTHENTICATED, EnumSet.of(CAPTURING, FAILED, CANCELLED));
        ALLOWED.put(CAPTURING, EnumSet.of(SUCCEEDED, FAILED));
        ALLOWED.put(SUCCEEDED, EnumSet.of(REFUND_REQUESTED, PARTIALLY_REFUNDED, REFUNDED));
        ALLOWED.put(FAILED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        ALLOWED.put(REFUND_REQUESTED, EnumSet.of(PARTIALLY_REFUNDED, REFUNDED, FAILED));
        ALLOWED.put(PARTIALLY_REFUNDED, EnumSet.of(REFUNDED, REFUND_REQUESTED));
        ALLOWED.put(REFUNDED, EnumSet.noneOf(PaymentStatus.class));
    }

    public boolean canTransitionTo(PaymentStatus target) {
        if (this == target) return true;
        return ALLOWED.getOrDefault(this, Set.of()).contains(target);
    }
}
