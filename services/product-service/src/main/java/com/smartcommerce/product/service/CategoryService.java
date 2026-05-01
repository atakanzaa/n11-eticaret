package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.product.api.dto.CategoryResponse;
import com.smartcommerce.product.api.dto.CreateCategoryRequest;
import com.smartcommerce.product.api.dto.UpdateCategoryRequest;
import com.smartcommerce.product.api.mapper.CategoryMapper;
import com.smartcommerce.product.domain.Category;
import com.smartcommerce.product.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

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

    /** Returns ALL categories (including inactive) for admin management. */
    public List<CategoryResponse> listAllForAdmin() {
        return categoryRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
            .map(c -> mapper.toResponse(c, categoryRepository.countActiveProducts(c.getId())))
            .toList();
    }

    public CategoryResponse getBySlug(String slug) {
        var category = categoryRepository.findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found: " + slug));
        return mapper.toResponse(category, categoryRepository.countActiveProducts(category.getId()));
    }

    public CategoryResponse getById(UUID id) {
        var category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found: " + id));
        return mapper.toResponse(category, categoryRepository.countActiveProducts(category.getId()));
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        var category = Category.builder()
            .parentId(request.parentId())
            .name(request.name())
            .slug(request.slug())
            .description(request.description())
            .imageUrl(request.imageUrl())
            .displayOrder(request.displayOrder())
            .active(true)
            .build();
        category = categoryRepository.save(category);
        return mapper.toResponse(category, 0L);
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        var category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found: " + id));

        if (request.name() != null) category.setName(request.name());
        if (request.slug() != null) category.setSlug(request.slug());
        if (request.description() != null) category.setDescription(request.description());
        if (request.imageUrl() != null) category.setImageUrl(request.imageUrl());
        if (request.displayOrder() != null) category.setDisplayOrder(request.displayOrder());
        if (request.active() != null) category.setActive(request.active());
        if (request.parentId() != null) category.setParentId(request.parentId());

        category = categoryRepository.save(category);
        return mapper.toResponse(category, categoryRepository.countActiveProducts(category.getId()));
    }

    @Transactional
    public void delete(UUID id) {
        var category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found: " + id));
        long productCount = categoryRepository.countActiveProducts(id);
        if (productCount > 0) {
            throw new IllegalStateException("Cannot delete category with " + productCount + " active products");
        }
        // Also check for child categories
        long childCount = categoryRepository.countByParentId(id);
        if (childCount > 0) {
            throw new IllegalStateException("Cannot delete category with " + childCount + " subcategories");
        }
        categoryRepository.delete(category);
    }
}
