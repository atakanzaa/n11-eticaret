package com.smartcommerce.order.api;

import com.smartcommerce.order.api.dto.*;
import com.smartcommerce.order.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final CheckoutOrchestrator checkoutOrchestrator;
    private final OrderQueryService orderQueryService;

    @PostMapping("/api/checkout")
    @ResponseStatus(HttpStatus.CREATED)
    public CheckoutResponse checkout(@AuthenticationPrincipal String userId,
                                     @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
                                     @Valid @RequestBody CheckoutRequest request) {
        return checkoutOrchestrator.checkout(UUID.fromString(userId), request, idempotencyKey);
    }

    @GetMapping("/api/orders")
    public Page<OrderResponse> getMyOrders(@AuthenticationPrincipal String userId,
                                           @PageableDefault(size = 20, sort = "createdAt",
                                               direction = Sort.Direction.DESC) Pageable pageable) {
        return orderQueryService.getMyOrders(UUID.fromString(userId), pageable);
    }

    @GetMapping("/api/orders/{orderId}")
    public OrderResponse getMyOrder(@AuthenticationPrincipal String userId, @PathVariable UUID orderId) {
        return orderQueryService.getMyOrder(UUID.fromString(userId), orderId);
    }
}
