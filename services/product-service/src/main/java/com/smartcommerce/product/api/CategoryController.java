package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.CategoryResponse;
import com.smartcommerce.product.api.dto.ProductResponse;
import com.smartcommerce.product.service.CategoryService;
import com.smartcommerce.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping
    public List<CategoryResponse> list() {
        return categoryService.listAll();
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
}
