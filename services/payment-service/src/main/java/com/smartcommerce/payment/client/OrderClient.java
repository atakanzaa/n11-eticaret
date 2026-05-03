package com.smartcommerce.payment.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.UUID;

@FeignClient(name = "order-service", url = "${services.order.url:http://localhost:8089}")
public interface OrderClient {
    @GetMapping("/api/orders/internal/{orderId}")
    @CircuitBreaker(name = "order-service")
    OrderDetail getOrder(@PathVariable UUID orderId);

    record OrderDetail(
        UUID id,
        UUID userId,
        String status,
        BigDecimal grandTotal,
        String currency,
        String shippingFullName,
        String shippingPhone,
        String shippingCity,
        String shippingDistrict,
        String shippingFullAddress,
        String shippingPostalCode,
        java.util.List<OrderItem> items
    ) {}

    record OrderItem(
        UUID offerId,
        UUID productId,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String productTitle
    ) {}
}
