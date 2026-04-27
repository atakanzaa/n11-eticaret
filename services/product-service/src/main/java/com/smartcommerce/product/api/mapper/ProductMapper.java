package com.smartcommerce.product.api.mapper;

import com.smartcommerce.product.api.dto.ProductImageDto;
import com.smartcommerce.product.api.dto.ProductResponse;
import com.smartcommerce.product.domain.Product;
import com.smartcommerce.product.domain.ProductImage;
import org.mapstruct.*;

import java.util.Comparator;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "primaryImageUrl", expression = "java(primaryImageUrl(product))")
    ProductResponse toResponse(Product product);

    ProductImageDto toDto(ProductImage image);

    default String primaryImageUrl(Product product) {
        if (product.getImages() == null || product.getImages().isEmpty()) {
            return null;
        }
        return product.getImages().stream()
            .filter(ProductImage::isPrimary)
            .findFirst()
            .orElseGet(() -> product.getImages().stream()
                .min(Comparator.comparingInt(ProductImage::getDisplayOrder))
                .orElse(null))
            .getUrl();
    }
}
