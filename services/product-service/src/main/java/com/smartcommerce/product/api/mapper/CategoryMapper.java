package com.smartcommerce.product.api.mapper;

import com.smartcommerce.product.api.dto.CategoryResponse;
import com.smartcommerce.product.domain.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {
    public CategoryResponse toResponse(Category category, long productCount) {
        return new CategoryResponse(
            category.getId(),
            category.getParentId(),
            category.getName(),
            category.getSlug(),
            category.getDescription(),
            category.getImageUrl(),
            category.getDisplayOrder(),
            productCount
        );
    }
}
