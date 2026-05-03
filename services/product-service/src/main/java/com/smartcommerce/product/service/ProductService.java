package com.smartcommerce.product.service;

import com.smartcommerce.common.errors.DuplicateResourceException;
import com.smartcommerce.common.errors.ErrorCode;
import com.smartcommerce.common.errors.ResourceNotFoundException;
import com.smartcommerce.common.events.EventType;
import com.smartcommerce.common.events.Topics;
import com.smartcommerce.product.api.dto.*;
import com.smartcommerce.product.api.mapper.ProductMapper;
import com.smartcommerce.product.domain.Product;
import com.smartcommerce.product.domain.ProductStatus;
import com.smartcommerce.product.event.outbox.OutboxService;
import com.smartcommerce.product.repository.CategoryRepository;
import com.smartcommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final OutboxService outboxService;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        var category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Category not found"));

        // Barcode dedupe — when present, redirect the seller to add an offer to
        // the existing product instead of duplicating the catalog entry. The
        // existingProductId is surfaced via ErrorResponse.details so the
        // frontend can prompt "use existing" and call POST /api/offers.
        var barcode = normalizeBarcode(request.barcode());
        if (barcode != null) {
            var existing = productRepository.findByBarcodeAndDeletedAtIsNull(barcode);
            if (existing.isPresent()) {
                throw new DuplicateResourceException(ErrorCode.DUPLICATE_RESOURCE,
                    "Product with this barcode already exists: " + existing.get().getId());
            }
        }

        var product = Product.builder()
            .title(request.title())
            .slug(uniqueSlug(request.title()))
            .description(request.description())
            .shortDescription(request.shortDescription())
            .brandId(request.brandId())
            .categoryId(category.getId())
            .attributes(request.attributes())
            .barcode(barcode)
            .status(ProductStatus.DRAFT)
            .build();
        product = productRepository.save(product);
        outboxService.publish(Topics.PRODUCT_CREATED, EventType.PRODUCT_CREATED,
            product.getId().toString(), "PRODUCT", productMapper.toResponse(product));
        return productMapper.toResponse(product);
    }

    private String normalizeBarcode(String raw) {
        if (raw == null) return null;
        var trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Lookup endpoint used by the seller "Bu ürün zaten var mı?" preflight. */
    public ProductResponse findByBarcode(String barcode) {
        var normalized = normalizeBarcode(barcode);
        if (normalized == null) {
            throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Barcode is required");
        }
        return productRepository.findByBarcodeAndDeletedAtIsNull(normalized)
            .map(productMapper::toResponse)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
    }

    @Transactional
    public ProductResponse update(UUID id, UpdateProductRequest request) {
        var product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        if (request.title() != null) {
            product.setTitle(request.title());
            product.setSlug(uniqueSlug(request.title()));
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.shortDescription() != null) product.setShortDescription(request.shortDescription());
        if (request.attributes() != null) product.setAttributes(request.attributes());
        if (request.status() != null) product.setStatus(request.status());
        product = productRepository.save(product);
        outboxService.publish(Topics.PRODUCT_UPDATED, EventType.PRODUCT_UPDATED,
            product.getId().toString(), "PRODUCT", productMapper.toResponse(product));
        return productMapper.toResponse(product);
    }

    @Transactional
    public void delete(UUID id) {
        var product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        product.setDeletedAt(Instant.now());
        product.setStatus(ProductStatus.ARCHIVED);
        productRepository.save(product);
        outboxService.publish(Topics.PRODUCT_DELETED, EventType.PRODUCT_DELETED,
            product.getId().toString(), "PRODUCT", productMapper.toResponse(product));
    }

    public ProductResponse getById(UUID id) {
        var product = productRepository.findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND, "Product not found"));
        return productMapper.toResponse(product);
    }

    public Page<ProductResponse> search(String query, UUID categoryId, UUID brandId,
                                        Double minRating, Pageable pageable) {
        return productRepository.search(query, categoryId, brandId, minRating, pageable)
            .map(productMapper::toResponse);
    }

    private String uniqueSlug(String title) {
        var base = generateSlug(title);
        var candidate = base;
        var suffix = 2;
        while (productRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String generateSlug(String title) {
        return Normalizer.normalize(title.toLowerCase(), Normalizer.Form.NFD)
            .replace("ı", "i")
            .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
    }
}
