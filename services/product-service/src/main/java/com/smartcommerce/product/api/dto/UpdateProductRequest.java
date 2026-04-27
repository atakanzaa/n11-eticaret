package com.smartcommerce.product.api.dto;

import com.smartcommerce.product.domain.ProductStatus;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateProductRequest(
    @Size(max = 500) String title,
    String description,
    @Size(max = 1000) String shortDescription,
    Map<String, Object> attributes,
    ProductStatus status
) {
}
