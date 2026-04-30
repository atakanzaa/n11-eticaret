package com.smartcommerce.order.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuePoint(LocalDate date, BigDecimal amount, long orderCount) {
}
