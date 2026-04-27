package com.smartcommerce.inventory.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@FeignClient(name = "seller-service", url = "${services.seller.url:http://localhost:8083}")
public interface SellerClient {
    @GetMapping("/api/sellers/by-user/{userId}")
    @CircuitBreaker(name = "seller-service")
    SellerInfo getByUserId(@PathVariable UUID userId);

    default UUID getSellerIdByUserId(UUID userId) {
        return getByUserId(userId).id();
    }

    record SellerInfo(UUID id, UUID userId, String storeName, String status) {
    }
}
