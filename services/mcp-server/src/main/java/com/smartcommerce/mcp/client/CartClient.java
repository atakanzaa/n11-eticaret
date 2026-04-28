package com.smartcommerce.mcp.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "mcp-cart-service", url = "${services.cart.url:http://localhost:8088}")
public interface CartClient {

    @GetMapping("/api/cart/internal/users/{userId}")
    @CircuitBreaker(name = "cart-service")
    Object getCart(@PathVariable UUID userId);
}
