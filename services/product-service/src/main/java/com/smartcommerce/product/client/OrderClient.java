package com.smartcommerce.product.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * Read-only access to order-service for verified-purchase checks during
 * review creation. We only call the internal endpoint, which doesn't enforce
 * auth so we must trust this client only inside the application network.
 */
@FeignClient(name = "order-service", url = "${services.order.url:http://localhost:8089}")
public interface OrderClient {

    @GetMapping("/api/orders/internal/{orderId}")
    @CircuitBreaker(name = "order-service")
    OrderInternalResponse getById(@PathVariable UUID orderId);

    record OrderInternalResponse(
        UUID id,
        UUID userId,
        String status,
        List<OrderItem> items
    ) {}

    record OrderItem(
        UUID id,
        UUID offerId,
        UUID productId,
        UUID sellerId,
        int quantity
    ) {}
}
