package com.smartcommerce.order.api.dto;

import java.math.BigDecimal;

public record AdminOverviewResponse(
    BigDecimal gmv30d,
    long ordersCount30d,
    BigDecimal averageBasket,
    long activeSellers30d
) {
}
