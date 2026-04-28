package com.smartcommerce.mcp.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@FeignClient(name = "mcp-order-service", url = "${services.order.url:http://localhost:8089}")
public interface OrderClient {

    @GetMapping("/api/orders/internal/{orderId}")
    @CircuitBreaker(name = "order-service")
    Object getOrder(@PathVariable UUID orderId);

    @GetMapping("/api/orders/internal/users/{userId}")
    @CircuitBreaker(name = "order-service")
    Object getOrdersByUser(@PathVariable UUID userId, @RequestParam(defaultValue = "10") int size);
}
