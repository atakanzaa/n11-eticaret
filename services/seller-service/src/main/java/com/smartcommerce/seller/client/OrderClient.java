package com.smartcommerce.seller.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FeignClient(name = "order-service", url = "${services.order.url:http://localhost:8089}")
public interface OrderClient {

    /**
     * Internal endpoint exposing the order's full item list. Used by the
     * SellerRatingEventConsumer to resolve which seller a review applies to —
     * REVIEW events carry productId+orderId, but the seller is item-level
     * snapshot and only order-service knows it.
     */
    @GetMapping("/api/orders/internal/{orderId}")
    @CircuitBreaker(name = "order-service")
    OrderDetail getById(@PathVariable UUID orderId);

    record OrderDetail(UUID id, UUID userId, String status, BigDecimal grandTotal,
                       String currency, UUID paymentId,
                       String shippingFullName, String shippingPhone,
                       String shippingCity, String shippingDistrict,
                       String shippingFullAddress, String shippingPostalCode,
                       List<OrderItem> items) {}

    record OrderItem(UUID offerId, UUID productId, UUID sellerId, int quantity,
                     BigDecimal unitPrice, BigDecimal lineTotal, String productTitle) {}
}
