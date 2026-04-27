package com.smartcommerce.product.api.dto;

import com.smartcommerce.product.domain.ProductStatus;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    String title,
    String slug,
    String description,
    String shortDescription,
    UUID brandId,
    UUID categoryId,
    Map<String, Object> attributes,
    ProductStatus status,
    String primaryImageUrl,
    List<ProductImageDto> images,
    Long version
) {
}
