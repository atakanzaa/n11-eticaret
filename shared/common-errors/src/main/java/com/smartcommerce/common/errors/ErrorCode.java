package com.smartcommerce.common.errors;

public enum ErrorCode {
    // Generic (1xxx)
    INTERNAL_ERROR("ERR_1000"),
    VALIDATION_FAILED("ERR_1001"),
    RESOURCE_NOT_FOUND("ERR_1002"),
    DUPLICATE_RESOURCE("ERR_1003"),
    BUSINESS_RULE_VIOLATION("ERR_1004"),
    DEPENDENCY_UNAVAILABLE("ERR_1005"),

    // Auth (2xxx)
    INVALID_CREDENTIALS("ERR_2000"),
    TOKEN_EXPIRED("ERR_2001"),
    TOKEN_INVALID("ERR_2002"),
    UNAUTHORIZED("ERR_2003"),
    FORBIDDEN("ERR_2004"),
    ACCOUNT_LOCKED("ERR_2005"),
    REFRESH_TOKEN_REUSED("ERR_2006"),

    // Order (3xxx)
    ORDER_NOT_FOUND("ERR_3000"),
    IDEMPOTENCY_KEY_CONFLICT("ERR_3001"),
    INVALID_ORDER_STATE("ERR_3002"),

    // Payment (4xxx)
    PAYMENT_FAILED("ERR_4000"),
    PAYMENT_PROVIDER_ERROR("ERR_4001"),

    // Inventory (5xxx)
    INSUFFICIENT_STOCK("ERR_5000"),
    OPTIMISTIC_LOCK_CONFLICT("ERR_5001"),

    // Cart (6xxx)
    CART_NOT_FOUND("ERR_6000"),
    PRICE_CHANGED("ERR_6001"),
    OFFER_UNAVAILABLE("ERR_6002");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
