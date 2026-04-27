package com.smartcommerce.cart.api.dto;

import java.util.UUID;

public record CartValidationIssue(
    UUID offerId,
    String code,
    String message
) {
}
