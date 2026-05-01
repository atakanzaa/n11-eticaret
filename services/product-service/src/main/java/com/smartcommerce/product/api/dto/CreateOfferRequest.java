package com.smartcommerce.product.api.dto;

import com.smartcommerce.product.domain.CargoProvider;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateOfferRequest(
    @NotNull UUID productId,
    @Size(max = 100) String sku,
    @NotNull @DecimalMin("0.01") BigDecimal price,
    @DecimalMin("0.01") BigDecimal listPrice,
    CargoProvider cargoProvider,
    @DecimalMin("0.00") BigDecimal cargoPrice,
    @Min(1) @Max(30) Integer estimatedDeliveryDays,
    @DecimalMin("0.00") BigDecimal freeShippingThreshold,
    @PositiveOrZero Integer initialStock
) {
}
