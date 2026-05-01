package com.smartcommerce.catalog.api.dto;

import java.math.BigDecimal;

public record SearchQuery(
    String queryText,
    String categoryId,
    String brandId,
    String sellerId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer minRating,
    Boolean inStockOnly,
    String sort,
    int page,
    int size
) {
}
