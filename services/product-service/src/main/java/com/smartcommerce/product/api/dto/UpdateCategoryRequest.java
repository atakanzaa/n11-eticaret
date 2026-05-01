package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record UpdateCategoryRequest(
    UUID parentId,
    @Size(max = 255) String name,
    @Size(max = 255) String slug,
    @Size(max = 1000) String description,
    String imageUrl,
    Integer displayOrder,
    Boolean active
) {
}
