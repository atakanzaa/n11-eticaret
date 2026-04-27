package com.smartcommerce.order.service;

import com.fasterxml.jackson.databind.*;
import com.smartcommerce.common.errors.*;
import com.smartcommerce.common.events.*;
import com.smartcommerce.order.api.dto.*;
import com.smartcommerce.order.api.mapper.OrderMapper;
import com.smartcommerce.order.client.*;
import com.smartcommerce.order.domain.*;
import com.smartcommerce.order.event.outbox.OutboxService;
import com.smartcommerce.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutOrchestrator {
    private static final Duration PAYMENT_TTL = Duration.ofMinutes(15);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final CartClient cartClient;
    private final UserClient userClient;
    private final InventoryClient inventoryClient;
    private final FraudDetectionClient fraudDetectionClient;
    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxService outboxService;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public CheckoutResponse checkout(UUID userId, CheckoutRequest request, String idempotencyKey) {
        var requestHash = hashRequest(userId, request);
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = idempotencyKeyRepository.findById(idempotencyKey);
            if (existing.isPresent()) {
                if (!existing.get().getRequestHash().equals(requestHash)) {
                    throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT, HttpStatus.CONFLICT,
                        "Idempotency key was already used for a different checkout request");
                }
                return objectMapper.convertValue(existing.get().getResponseBody(), CheckoutResponse.class);
            }
        }

        Order order = null;
        try {
            var validation = logStep(null, "VALIDATE_CART", () -> cartClient.validateCart(userId));
            if (validation.cart() == null || validation.cart().items() == null || validation.cart().items().isEmpty()) {
                throw new BusinessException(ErrorCode.CART_NOT_FOUND, "Cart is empty");
            }
            if (validation.issues() != null && !validation.issues().isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Cart has validation issues");
            }

            var address = logStep(null, "LOAD_ADDRESS", () -> userClient.getAddress(request.addressId()));
            order = createOrder(userId, request, validation.cart(), address, idempotencyKey);
            sagaLogRepository.save(successLog(order.getId(), "CREATE_ORDER", objectMapper.valueToTree(orderMapper.toEvent(order))));
            var activeOrder = order;

            var fraud = logStep(activeOrder.getId(), "FRAUD_CHECK", () -> fraudDetectionClient.check(
                new FraudDetectionClient.FraudCheckRequest(activeOrder.getId(), userId, activeOrder.getGrandTotal(),
                    activeOrder.getCurrency(), validation.cart().itemCount())));
            if (fraud.flagged()) {
                order.setStatus(OrderStatus.CANCELLED);
                order.setSagaState(SagaState.FAILED.name());
                order.setSagaFailedAt(Instant.now());
                order.setSagaFailureReason(fraud.reason());
                order.setCancelledAt(Instant.now());
                orderRepository.save(order);
                outboxService.publish(Topics.ORDER_FRAUD_FLAGGED, EventType.ORDER_FRAUD_FLAGGED,
                    order.getId().toString(), "ORDER", Map.of("orderId", order.getId(), "reason", fraud.reason()));
                throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, "Checkout rejected by fraud rules");
            }

            for (var item : validation.cart().items()) {
                var reservationKey = activeOrder.getId() + ":" + item.offerId();
                logStep(activeOrder.getId(), "RESERVE_INVENTORY", () -> inventoryClient.reserve(
                    new InventoryClient.ReserveRequest(activeOrder.getId(), item.offerId(), item.quantity(), reservationKey)));
            }

            order.setStatus(OrderStatus.PAYMENT_PENDING);
            order.setSagaState(SagaState.PAYMENT_INITIATED.name());
            order.setExpiresAt(Instant.now().plus(PAYMENT_TTL));
            order = orderRepository.save(order);
            outboxService.publish(Topics.ORDER_CREATED, EventType.ORDER_CREATED,
                order.getId().toString(), "ORDER", orderMapper.toEvent(order));
            outboxService.publish(Topics.PAYMENT_INITIATED, EventType.PAYMENT_INITIATED,
                order.getId().toString(), "ORDER", Map.of("orderId", order.getId(), "userId", userId,
                    "amount", order.getGrandTotal(), "currency", order.getCurrency(),
                    "paymentMethod", order.getPaymentMethod()));

            var response = new CheckoutResponse(order.getId(), order.getOrderNumber(), order.getStatus().name(),
                order.getGrandTotal(), "/payments/mock/" + order.getId(), order.getExpiresAt());
            saveIdempotency(idempotencyKey, requestHash, response);
            return response;
        } catch (Exception e) {
            compensate(order, e);
            throw e;
        }
    }

    @Transactional
    public void confirmPayment(UUID orderId, UUID paymentId) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.CONFIRMED) return;
        if (order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE, "Order is not waiting for payment");
        }

        inventoryClient.confirm(orderId);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentId(paymentId);
        order.setSagaState(SagaState.COMPLETED.name());
        order.setSagaCompletedAt(Instant.now());
        order.setConfirmedAt(Instant.now());
        orderRepository.save(order);
        sagaLogRepository.save(successLog(orderId, "PAYMENT_CONFIRMED", objectMapper.valueToTree(Map.of("paymentId", paymentId))));
        outboxService.publish(Topics.ORDER_CONFIRMED, EventType.ORDER_CONFIRMED,
            order.getId().toString(), "ORDER", orderMapper.toEvent(order));
    }

    @Transactional
    public void failPayment(UUID orderId, String reason) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        cancelPaymentPendingOrder(order, reason == null ? "PAYMENT_FAILED" : reason, EventType.ORDER_CANCELLED);
    }

    @Transactional
    public void expirePaymentPendingOrders() {
        orderRepository.findByStatusAndExpiresAtLessThanEqual(OrderStatus.PAYMENT_PENDING, Instant.now())
            .forEach(order -> cancelPaymentPendingOrder(order, "PAYMENT_TIMEOUT", EventType.ORDER_EXPIRED));
    }

    private Order createOrder(UUID userId, CheckoutRequest request, CartClient.CartResponse cart,
                              UserClient.AddressDetail address, String idempotencyKey) {
        var order = Order.builder()
            .orderNumber(generateOrderNumber())
            .userId(userId)
            .status(OrderStatus.CREATED)
            .shippingFullName(address.fullName())
            .shippingPhone(address.phone())
            .shippingCountry(address.country())
            .shippingCity(address.city())
            .shippingDistrict(address.district())
            .shippingFullAddress(address.fullAddress())
            .shippingPostalCode(address.postalCode())
            .billingFullName(address.fullName())
            .billingFullAddress(address.fullAddress())
            .subtotal(cart.total())
            .shippingTotal(cart.items().stream()
                .map(CartClient.CartItem::cargoPriceSnapshot)
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add))
            .discountTotal(java.math.BigDecimal.ZERO)
            .taxTotal(java.math.BigDecimal.ZERO)
            .grandTotal(cart.total().add(cart.items().stream()
                .map(CartClient.CartItem::cargoPriceSnapshot)
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)))
            .currency(cart.items().getFirst().currencySnapshot())
            .paymentMethod(request.paymentMethod() == null ? "MOCK_CARD" : request.paymentMethod())
            .idempotencyKey(idempotencyKey)
            .sagaState(SagaState.STARTED.name())
            .build();
        for (var item : cart.items()) {
            order.addItem(OrderItem.builder()
                .offerId(item.offerId())
                .productId(item.productId())
                .sellerId(item.sellerId())
                .quantity(item.quantity())
                .unitPrice(item.unitPriceSnapshot())
                .lineTotal(item.subtotal())
                .productTitle(item.productTitleSnapshot())
                .productImageUrl(item.productImageSnapshot())
                .sellerName(item.sellerNameSnapshot())
                .build());
        }
        return orderRepository.save(order);
    }

    private void cancelPaymentPendingOrder(Order order, String reason, String eventType) {
        if (!order.canCancel()) return;
        try {
            inventoryClient.release(order.getId());
        } catch (Exception e) {
            log.warn("Inventory release failed for order {} during compensation: {}", order.getId(), e.getMessage());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order.setSagaState(SagaState.COMPENSATED.name());
        order.setSagaFailedAt(Instant.now());
        order.setSagaFailureReason(reason);
        order.setCancelledAt(Instant.now());
        orderRepository.save(order);
        sagaLogRepository.save(successLog(order.getId(), "COMPENSATE_RELEASE_INVENTORY", objectMapper.valueToTree(Map.of("reason", reason))));
        outboxService.publish(Topics.ORDER_CANCELLED, eventType, order.getId().toString(), "ORDER",
            Map.of("orderId", order.getId(), "orderNumber", order.getOrderNumber(),
                "userId", order.getUserId(), "reason", reason));
    }

    private void compensate(Order order, Exception e) {
        if (order == null || order.getId() == null || order.getStatus() == OrderStatus.CANCELLED) return;
        cancelPaymentPendingOrder(order, e.getMessage() == null ? "CHECKOUT_FAILED" : e.getMessage(), EventType.ORDER_CANCELLED);
    }

    private <T> T logStep(UUID orderId, String step, SagaSupplier<T> supplier) {
        var startedAt = Instant.now();
        try {
            var result = supplier.get();
            if (orderId != null) sagaLogRepository.save(SagaLog.builder()
                .orderId(orderId).step(step).status("SUCCESS")
                .payload(objectMapper.valueToTree(result)).startedAt(startedAt).completedAt(Instant.now()).build());
            return result;
        } catch (Exception e) {
            if (orderId != null) sagaLogRepository.save(SagaLog.builder()
                .orderId(orderId).step(step).status("FAILED").errorMessage(e.getMessage())
                .startedAt(startedAt).completedAt(Instant.now()).build());
            throw e;
        }
    }

    private SagaLog successLog(UUID orderId, String step, JsonNode payload) {
        return SagaLog.builder()
            .orderId(orderId)
            .step(step)
            .status("SUCCESS")
            .payload(payload)
            .startedAt(Instant.now())
            .completedAt(Instant.now())
            .build();
    }

    private void saveIdempotency(String idempotencyKey, String requestHash, CheckoutResponse response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) return;
        idempotencyKeyRepository.save(IdempotencyKey.builder()
            .key(idempotencyKey)
            .requestHash(requestHash)
            .responseStatus(HttpStatus.CREATED.value())
            .responseBody(objectMapper.valueToTree(response))
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plus(IDEMPOTENCY_TTL))
            .build());
    }

    private String hashRequest(UUID userId, CheckoutRequest request) {
        return DigestUtils.md5DigestAsHex((userId + ":" + request.addressId() + ":" + request.paymentMethod())
            .getBytes(StandardCharsets.UTF_8));
    }

    private String generateOrderNumber() {
        return "SC" + Instant.now().toEpochMilli() + UUID.randomUUID().toString().substring(0, 4).toUpperCase(Locale.ROOT);
    }

    @FunctionalInterface
    private interface SagaSupplier<T> {
        T get();
    }
}
