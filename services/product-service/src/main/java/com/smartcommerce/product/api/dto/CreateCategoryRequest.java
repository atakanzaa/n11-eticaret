package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateCategoryRequest(
    UUID parentId,
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 255) String slug,
    @Size(max = 1000) String description,
    String imageUrl,
    int displayOrder
) {
}
