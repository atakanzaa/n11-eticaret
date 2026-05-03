package com.smartcommerce.order.api;

import com.smartcommerce.order.api.dto.*;
import com.smartcommerce.order.security.SellerOwnershipGuard;
import com.smartcommerce.order.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {
    private final CheckoutOrchestrator checkoutOrchestrator;
    private final OrderQueryService orderQueryService;
    private final SellerOwnershipGuard sellerGuard;

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

    @PostMapping("/api/orders/{orderId}/cancel")
    public OrderResponse cancelOrder(@AuthenticationPrincipal String userId,
                                     @PathVariable UUID orderId) {
        return checkoutOrchestrator.cancelByUser(UUID.fromString(userId), orderId);
    }

    @PostMapping("/api/orders/{orderId}/confirm-received")
    public OrderResponse confirmReceived(@AuthenticationPrincipal String userId,
                                         @PathVariable UUID orderId) {
        return checkoutOrchestrator.markOrderReceived(UUID.fromString(userId), orderId);
    }

    @GetMapping("/api/orders/internal/{orderId}")
    public OrderInternalResponse getInternalOrder(@PathVariable UUID orderId) {
        return orderQueryService.getInternal(orderId);
    }

    @GetMapping("/api/orders/internal/users/{userId}")
    public Page<OrderResponse> getInternalOrdersForUser(@PathVariable UUID userId,
                                                       @PageableDefault(size = 20, sort = "createdAt",
                                                           direction = Sort.Direction.DESC) Pageable pageable) {
        return orderQueryService.getMyOrders(userId, pageable);
    }

    @GetMapping("/api/orders/seller/{sellerId}/kpi")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public SellerOrderKpiResponse getSellerKpi(@AuthenticationPrincipal String userId,
                                               @PathVariable UUID sellerId,
                                               Authentication auth) {
        sellerGuard.requireOwnsSeller(userId, sellerId, auth);
        return orderQueryService.getSellerKpi(sellerId);
    }

    @GetMapping("/api/orders/seller/{sellerId}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public Page<OrderResponse> getSellerOrders(@AuthenticationPrincipal String userId,
                                               @PathVariable UUID sellerId,
                                               Authentication auth,
                                               @PageableDefault(size = 20) Pageable pageable) {
        sellerGuard.requireOwnsSeller(userId, sellerId, auth);
        return orderQueryService.getSellerOrders(sellerId, pageable);
    }

    @PatchMapping("/api/orders/{orderId}/ship")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public OrderResponse markShipped(@AuthenticationPrincipal String userId,
                                     @RequestParam UUID sellerId,
                                     @PathVariable UUID orderId,
                                     Authentication auth) {
        sellerGuard.requireOwnsSeller(userId, sellerId, auth);
        return orderQueryService.markShipped(sellerId, orderId);
    }

    @GetMapping("/api/orders/seller/{sellerId}/revenue")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    public List<RevenuePoint> getSellerRevenue(@AuthenticationPrincipal String userId,
                                               @PathVariable UUID sellerId,
                                               Authentication auth,
                                               @RequestParam(defaultValue = "30") int days) {
        sellerGuard.requireOwnsSeller(userId, sellerId, auth);
        return orderQueryService.getSellerRevenueSeries(sellerId, days);
    }

    @GetMapping("/api/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public AdminOverviewResponse getAdminOverview() {
        return orderQueryService.getAdminOverview();
    }
}
