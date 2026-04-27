package com.smartcommerce.cart.api.dto;

import java.util.List;

public record CartValidationResponse(
    CartResponse cart,
    List<CartValidationIssue> issues
) {
}
