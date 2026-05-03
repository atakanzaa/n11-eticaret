package com.smartcommerce.product.api;

import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public Page<ProductResponse> search(@RequestParam(required = false) String query,
                                        @RequestParam(required = false) UUID categoryId,
                                        @RequestParam(required = false) UUID brandId,
                                        @RequestParam(required = false) Integer minRating,
                                        Pageable pageable) {
        Double minRatingDouble = minRating != null ? minRating.doubleValue() : null;
        return productService.search(query, categoryId, brandId, minRatingDouble, pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable UUID id) {
        return productService.getById(id);
    }

    /**
     * Pre-create lookup used by the seller "ürün ekle" form. Returns 200 + the
     * existing product when the barcode is already registered, 404 otherwise.
     * Sellers can then short-circuit to the "add an offer" flow.
     */
    @GetMapping("/by-barcode/{barcode}")
    public ProductResponse getByBarcode(@PathVariable String barcode) {
        return productService.findByBarcode(barcode);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        return productService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
