package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.CategoryResponse;
import com.smartcommerce.product.api.dto.CreateCategoryRequest;
import com.smartcommerce.product.api.dto.ProductResponse;
import com.smartcommerce.product.api.dto.UpdateCategoryRequest;
import com.smartcommerce.product.service.CategoryService;
import com.smartcommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final ProductService productService;

    /** Public — returns only active categories (for storefront). */
    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listAll();
    }

    /** Admin — returns ALL categories including inactive ones. */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<CategoryResponse> listForAdmin() {
        return categoryService.listAllForAdmin();
    }

    @GetMapping("/{slug}")
    public CategoryResponse getBySlug(@PathVariable String slug) {
        return categoryService.getBySlug(slug);
    }

    @GetMapping("/{slug}/products")
    public Page<ProductResponse> productsByCategory(@PathVariable String slug, Pageable pageable) {
        var category = categoryService.getBySlug(slug);
        return productService.search(null, category.id(), null, pageable);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CreateCategoryRequest request) {
        return categoryService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public CategoryResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest request) {
        return categoryService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        categoryService.delete(id);
    }
}
