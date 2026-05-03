package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "seller-service", url = "${services.seller.url:http://localhost:8087}")
public interface SellerClient {

    @GetMapping("/api/sellers/by-user/{userId}")
    @CircuitBreaker(name = "seller-service")
    SellerDto getByUserId(@PathVariable UUID userId);

    record SellerDto(UUID id, UUID userId, String storeName) {}
}
