package com.smartcommerce.promotion.api.dto;

import java.math.BigDecimal;

public record CouponValidationResponse(
    boolean valid,
    String code,
    String discountType,
    BigDecimal discountAmount,
    String errorCode,
    String errorMessage
) {
    public static CouponValidationResponse valid(String code, String type, BigDecimal amount) {
        return new CouponValidationResponse(true, code, type, amount, null, null);
    }

    public static CouponValidationResponse invalid(String code, String errorCode, String errorMessage) {
        return new CouponValidationResponse(false, code, null, BigDecimal.ZERO, errorCode, errorMessage);
    }
}
