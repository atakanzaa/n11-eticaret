package com.smartcommerce.product.api.dto;

import java.util.UUID;

public record CategoryResponse(
    UUID id,
    UUID parentId,
    String name,
    String slug,
    String description,
    String imageUrl,
    int displayOrder,
    long productCount
) {
}
