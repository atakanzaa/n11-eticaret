package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@FeignClient(name = "inventory-service", url = "${services.inventory.url:http://localhost:8085}")
public interface InventoryClient {
    @PostMapping("/api/inventory/reserve")
    @CircuitBreaker(name = "inventory-service")
    ReservationResponse reserve(@RequestBody ReserveRequest request);

    @PostMapping("/api/inventory/orders/{orderId}/confirm")
    @CircuitBreaker(name = "inventory-service")
    void confirm(@PathVariable UUID orderId);

    @PostMapping("/api/inventory/orders/{orderId}/release")
    @CircuitBreaker(name = "inventory-service")
    void release(@PathVariable UUID orderId);

    record ReserveRequest(UUID orderId, UUID offerId, int quantity, String reservationKey) {}
    record ReservationResponse(UUID id, String reservationKey, UUID orderId, UUID offerId, int quantity,
                               String status, Instant expiresAt) {}
}
