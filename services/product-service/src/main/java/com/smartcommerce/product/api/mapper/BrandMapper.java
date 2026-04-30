package com.smartcommerce.product.api.mapper;

import com.smartcommerce.product.api.dto.BrandResponse;
import com.smartcommerce.product.domain.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {
    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName(), brand.getSlug(), brand.getLogoUrl());
    }
}
