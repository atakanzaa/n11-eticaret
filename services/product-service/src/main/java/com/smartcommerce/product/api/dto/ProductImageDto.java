package com.smartcommerce.product.api.dto;

import java.util.UUID;

public record ProductImageDto(
    UUID id,
    String url,
    String altText,
    int displayOrder,
    boolean primary
) {
}
