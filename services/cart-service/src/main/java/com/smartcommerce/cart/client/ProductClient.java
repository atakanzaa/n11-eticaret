package com.smartcommerce.cart.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "product-service", url = "${services.product.url:http://localhost:8084}")
public interface ProductClient {
    @GetMapping("/api/offers/{id}")
    @CircuitBreaker(name = "product-service")
    OfferDetail getOffer(@PathVariable UUID id);

    @GetMapping("/api/products/{id}")
    @CircuitBreaker(name = "product-service")
    ProductDetail getProduct(@PathVariable UUID id);

    enum OfferStatus {
        ACTIVE,
        PAUSED,
        OUT_OF_STOCK,
        REJECTED
    }

    record OfferDetail(UUID id, UUID productId, UUID sellerId, BigDecimal price,
                       String currency, BigDecimal cargoPrice, OfferStatus status) {
    }

    record ProductDetail(UUID id, String title, String primaryImageUrl) {
    }
}
