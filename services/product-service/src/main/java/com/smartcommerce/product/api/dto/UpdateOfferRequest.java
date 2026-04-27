package com.smartcommerce.product.api.dto;

import com.smartcommerce.product.domain.CargoProvider;
import com.smartcommerce.product.domain.OfferStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateOfferRequest(
    @DecimalMin("0.01") BigDecimal price,
    @DecimalMin("0.01") BigDecimal listPrice,
    CargoProvider cargoProvider,
    @DecimalMin("0.00") BigDecimal cargoPrice,
    @Min(1) @Max(30) Integer estimatedDeliveryDays,
    BigDecimal freeShippingThreshold,
    OfferStatus status
) {
}
