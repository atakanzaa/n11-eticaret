package com.smartcommerce.order.api.dto;

import java.math.BigDecimal;

/**
 * Per-seller order-side metrics. Inventory-side metrics (low-stock count) are
 * fetched separately by the seller dashboard from inventory-service.
 */
public record SellerOrderKpiResponse(
    BigDecimal todayRevenue,
    BigDecimal last7DaysRevenue,
    long pendingOrdersCount
) {
}
