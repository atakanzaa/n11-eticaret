package com.smartcommerce.order.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@FeignClient(name = "cart-service", url = "${services.cart.url:http://localhost:8088}")
public interface CartClient {
    @PostMapping("/api/cart/internal/users/{userId}/validate")
    @CircuitBreaker(name = "cart-service")
    CartValidationResponse validateCart(@PathVariable UUID userId);

    record CartValidationResponse(CartResponse cart, List<CartValidationIssue> issues) {}
    record CartValidationIssue(UUID offerId, String code, String message) {}
    record CartResponse(UUID id, UUID userId, String status, List<CartItem> items, BigDecimal total, int itemCount) {}
    record CartItem(UUID id, UUID offerId, UUID productId, UUID sellerId, int quantity,
                    BigDecimal unitPriceSnapshot, String currencySnapshot, String productTitleSnapshot,
                    String productImageSnapshot, String sellerNameSnapshot, BigDecimal cargoPriceSnapshot,
                    BigDecimal subtotal) {}
}
