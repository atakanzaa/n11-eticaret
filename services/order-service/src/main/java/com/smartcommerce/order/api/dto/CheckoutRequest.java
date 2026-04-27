package com.smartcommerce.order.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CheckoutRequest(
    @NotNull UUID addressId,
    String paymentMethod
) {
}
