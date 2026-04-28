package com.smartcommerce.catalog.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@FeignClient(name = "catalog-product-service", url = "${services.product.url:http://localhost:8084}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "product-service")
    ProductSummary getProduct(@PathVariable UUID id);

    @GetMapping("/api/products/{productId}/offers")
    @CircuitBreaker(name = "product-service")
    List<OfferSummary> getOffersForProduct(@PathVariable UUID productId);

    @GetMapping("/api/offers/{id}")
    @CircuitBreaker(name = "product-service")
    OfferSummary getOffer(@PathVariable UUID id);

    record ProductSummary(
        UUID id,
        String title,
        String slug,
        String description,
        String shortDescription,
        UUID brandId,
        UUID categoryId,
        Map<String, Object> attributes,
        String status,
        String primaryImageUrl,
        List<ImageDto> images,
        Long version
    ) {}

    record ImageDto(UUID id, String url, String altText, int displayOrder, boolean primary) {}

    record OfferSummary(
        UUID id,
        UUID productId,
        UUID sellerId,
        String sku,
        BigDecimal price,
        String currency,
        BigDecimal listPrice,
        String cargoProvider,
        BigDecimal cargoPrice,
        int estimatedDeliveryDays,
        BigDecimal freeShippingThreshold,
        String status,
        Long version
    ) {}
}
