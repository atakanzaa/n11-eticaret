package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.product.api.dto.CategoryResponse;
import com.smartcommerce.product.api.mapper.CategoryMapper;
import com.smartcommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper mapper;

    public List<CategoryResponse> listAll() {
        return categoryRepository.findAllActiveOrdered().stream()
            .map(c -> mapper.toResponse(c, categoryRepository.countActiveProducts(c.getId())))
            .toList();
    }

    public CategoryResponse getBySlug(String slug) {
        var category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + slug));
        return mapper.toResponse(category, categoryRepository.countActiveProducts(category.getId()));
    }
}
