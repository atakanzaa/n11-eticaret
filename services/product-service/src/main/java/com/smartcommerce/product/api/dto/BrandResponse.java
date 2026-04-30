package com.smartcommerce.product.api.dto;

import java.util.UUID;

public record BrandResponse(
    UUID id,
    String name,
    String slug,
    String logoUrl
) {
}
