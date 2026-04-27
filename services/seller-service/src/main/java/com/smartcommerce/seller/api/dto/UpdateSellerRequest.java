package com.smartcommerce.seller.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSellerRequest(
    @NotBlank @Size(max = 255) String storeName,
    @Size(max = 2000) String description
) {}
