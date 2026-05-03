package com.smartcommerce.catalog.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "inventory-service", url = "${services.inventory.url:http://localhost:8085}")
public interface InventoryClient {

    /**
     * Bulk lookup used to aggregate `totalStock` for the search projection.
     * Missing items are silently omitted, so the caller must not assume
     * one-to-one mapping with the request ids.
     */
    @GetMapping("/api/inventory/internal/by-offers")
    @CircuitBreaker(name = "inventory-service")
    List<InventorySummary> byOfferIds(@RequestParam("ids") List<UUID> offerIds);

    record InventorySummary(UUID id, UUID offerId, UUID sellerId, int availableQuantity,
                            int reservedQuantity, int soldQuantity, int lowStockThreshold) {}
}
