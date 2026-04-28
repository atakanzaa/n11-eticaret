package com.smartcommerce.catalog.api.dto;

import java.math.BigDecimal;

public record SearchQuery(
    String queryText,
    String categoryId,
    String brandId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Boolean inStockOnly,
    String sort,
    int page,
    int size
) {
}
