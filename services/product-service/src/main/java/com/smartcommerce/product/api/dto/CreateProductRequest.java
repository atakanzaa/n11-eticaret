package com.smartcommerce.product.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

public record CreateProductRequest(
    @NotBlank @Size(max = 500) String title,
    String description,
    @Size(max = 1000) String shortDescription,
    UUID brandId,
    @NotNull UUID categoryId,
    Map<String, Object> attributes,
    @Size(max = 32) String barcode
) {
}
