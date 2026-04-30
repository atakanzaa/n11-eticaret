package com.smartcommerce.product.domain;

/**
 * Lifecycle for product reviews. New reviews land as APPROVED in the demo
 * (no moderation queue) but the column exists so we can switch on a moderation
 * pipeline without a migration. PENDING/REJECTED reserve their slots.
 */
public enum ReviewStatus {
    APPROVED,
    PENDING,
    REJECTED
}
