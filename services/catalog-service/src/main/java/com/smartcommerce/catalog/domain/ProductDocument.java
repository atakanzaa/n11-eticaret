package com.smartcommerce.catalog.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record ProductDocument(
    String productId,
    String title,
    String description,
    String categoryId,
    String categoryName,
    String brandId,
    String brandName,
    java.util.List<String> sellerIds,
    Map<String, Object> attributes,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Integer offerCount,
    Integer totalStock,
    Boolean hasStock,
    Float averageRating,
    Integer ratingCount,
    String primaryImageUrl,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
}
