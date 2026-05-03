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
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
@Slf4j
public class CheckoutOrchestrator {
    private static final Duration PAYMENT_TTL = Duration.ofMinutes(15);
    private static final Duration IDEMPOTENCY_TTL = Duration.ofHours(24);

    private final CartClient cartClient;
    private final UserClient userClient;
    private final InventoryClient inventoryClient;
    private final FraudDetectionClient fraudDetectionClient;
    private final PromotionClient promotionClient;
    private final PaymentClient paymentClient;
    private final OrderRepository orderRepository;
    private final SagaLogRepository sagaLogRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final OutboxService outboxService;
    private final OrderMapper orderMapper;
    private final ObjectMapper objectMapper;

    private final Counter ordersCreated;
    private final Counter sagaCompensations;
    private final Counter ordersConfirmed;
    private final Timer checkoutDuration;

    public CheckoutOrchestrator(CartClient cartClient, UserClient userClient,
                                InventoryClient inventoryClient, FraudDetectionClient fraudDetectionClient,
                                PromotionClient promotionClient, PaymentClient paymentClient,
                                OrderRepository orderRepository,
                                SagaLogRepository sagaLogRepository,
                                IdempotencyKeyRepository idempotencyKeyRepository,
                                OutboxService outboxService, OrderMapper orderMapper,
                                ObjectMapper objectMapper, MeterRegistry registry) {
        this.cartClient = cartClient;
        this.userClient = userClient;
        this.inventoryClient = inventoryClient;
        this.fraudDetectionClient = fraudDetectionClient;
        this.promotionClient = promotionClient;
        this.paymentClient = paymentClient;
        this.orderRepository = orderRepository;
        this.sagaLogRepository = sagaLogRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.outboxService = outboxService;
        this.orderMapper = orderMapper;
        this.objectMapper = objectMapper;
        this.ordersCreated = Counter.builder("smartcommerce.orders.created")
            .description("Total orders successfully created via checkout").register(registry);
        this.ordersConfirmed = Counter.builder("smartcommerce.orders.confirmed")
            .description("Total orders confirmed after successful payment").register(registry);
        this.sagaCompensations = Counter.builder("smartcommerce.saga.compensations")
            .description("Total saga compensations triggered").register(registry);
        this.checkoutDuration = Timer.builder("smartcommerce.checkout.duration")
            .description("Checkout operation duration").register(registry);
    }

    @Transactional
    public CheckoutResponse checkout(UUID userId, CheckoutRequest request, String idempotencyKey) {
        var sample = Timer.start();
        try {
            return doCheckout(userId, request, idempotencyKey);
        } finally {
            sample.stop(checkoutDuration);
        }
    }

    private CheckoutResponse doCheckout(UUID userId, CheckoutRequest request, String idempotencyKey) {
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

        // Serialize concurrent checkouts for the same user to avoid duplicate
        // PAYMENT_PENDING orders. Lock is released when the @Transactional commits.
        orderRepository.lockCheckoutForUser(userId.toString());

        // After acquiring the lock, find any in-flight order for this user
        // and auto-cancel it. Starting a new checkout implies the previous
        // attempt was abandoned (closed browser, 3DS dropped, switched
        // payment method). Releases inventory + emits ORDER_CANCELLED.
        var existing = orderRepository.findFirstByUserIdAndStatusIn(
            userId, List.of(OrderStatus.CREATED, OrderStatus.PAYMENT_PENDING));
        if (existing.isPresent()) {
            var stale = existing.get();
            log.info("Auto-cancelling abandoned {} order {} for user {} on new checkout",
                stale.getStatus(), stale.getId(), userId);
            cancelPaymentPendingOrder(stale, "REPLACED_BY_NEW_CHECKOUT", EventType.ORDER_CANCELLED);
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
            applyCouponIfPresent(order, userId, request, validation.cart());
            sagaLogRepository.save(successLog(order.getId(), "CREATE_ORDER", objectMapper.valueToTree(orderMapper.toEvent(order))));
            var activeOrder = order;

            var firstOrder = orderRepository.countByUserId(userId) <= 1;
            var fraud = logStep(activeOrder.getId(), "FRAUD_CHECK", () -> fraudDetectionClient.check(
                new FraudDetectionClient.FraudCheckRequest(activeOrder.getId(), userId, activeOrder.getGrandTotal(),
                    activeOrder.getCurrency(), validation.cart().itemCount(), null, firstOrder)));
            if (fraud.flagged()) {
                order.transitionTo(OrderStatus.CANCELLED);
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

            order.transitionTo(OrderStatus.PAYMENT_PENDING);
            order.setSagaState(SagaState.PAYMENT_INITIATED.name());
            order.setExpiresAt(Instant.now().plus(PAYMENT_TTL));
            order = orderRepository.save(order);
            outboxService.publish(Topics.ORDER_CREATED, EventType.ORDER_CREATED,
                order.getId().toString(), "ORDER", orderMapper.toEvent(order));
            outboxService.publish(Topics.PAYMENT_INITIATED, EventType.PAYMENT_INITIATED,
                order.getId().toString(), "ORDER", Map.of("orderId", order.getId(), "userId", userId,
                    "amount", order.getGrandTotal(), "currency", order.getCurrency(),
                    "paymentMethod", order.getPaymentMethod()));

            // Frontend kicks off the real 3DS flow with POST /api/payments/initiate using the orderId
            // returned here, so we don't surface any payment URL from the order service.
            var response = new CheckoutResponse(order.getId(), order.getOrderNumber(), order.getStatus().name(),
                order.getGrandTotal(), order.getExpiresAt());
            saveIdempotency(idempotencyKey, requestHash, response);
            ordersCreated.increment();
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
        order.transitionTo(OrderStatus.CONFIRMED);
        order.setPaymentId(paymentId);
        order.setSagaState(SagaState.COMPLETED.name());
        order.setSagaCompletedAt(Instant.now());
        order.setConfirmedAt(Instant.now());
        orderRepository.save(order);
        ordersConfirmed.increment();
        recordCouponUsage(order);
        sagaLogRepository.save(successLog(orderId, "PAYMENT_CONFIRMED", objectMapper.valueToTree(Map.of("paymentId", paymentId))));
        outboxService.publish(Topics.ORDER_CONFIRMED, EventType.ORDER_CONFIRMED,
            order.getId().toString(), "ORDER", orderMapper.toEvent(order));
    }

    private void applyCouponIfPresent(Order order, UUID userId, CheckoutRequest request, CartClient.CartResponse cart) {
        if (request.couponCode() == null || request.couponCode().isBlank()) return;
        var firstOrder = orderRepository.countByUserId(userId) <= 1;
        var productIds = cart.items().stream().map(CartClient.CartItem::productId).toList();
        var sellerIds = cart.items().stream().map(CartClient.CartItem::sellerId).distinct().toList();
        var validation = promotionClient.validate(new PromotionClient.ValidationRequest(
            request.couponCode(), userId, order.getSubtotal(), order.getShippingTotal(),
            productIds, java.util.List.of(), sellerIds, firstOrder));
        if (!validation.valid()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION,
                validation.errorMessage() != null ? validation.errorMessage() : "Coupon validation failed");
        }
        order.setCouponCode(request.couponCode());
        order.setCouponDiscount(validation.discountAmount());
        order.setDiscountTotal(order.getDiscountTotal().add(validation.discountAmount()));
        order.setGrandTotal(order.getSubtotal().add(order.getShippingTotal())
            .subtract(order.getDiscountTotal()).add(order.getTaxTotal()));
        if (order.getGrandTotal().compareTo(java.math.BigDecimal.ZERO) < 0) {
            order.setGrandTotal(java.math.BigDecimal.ZERO);
        }
        orderRepository.save(order);
    }

    private void recordCouponUsage(Order order) {
        if (order.getCouponCode() == null || order.getCouponDiscount() == null) return;
        // Publish via outbox first — guaranteed at-least-once delivery to
        // promotion-service even if the sync call below fails. The promotion
        // consumer must be idempotent (keyed by orderId).
        outboxService.publish(Topics.COUPON_USED, EventType.COUPON_USED,
            order.getId().toString(), "ORDER",
            Map.of(
                "orderId", order.getId(),
                "userId", order.getUserId(),
                "couponCode", order.getCouponCode(),
                "discountAmount", order.getCouponDiscount()
            ));
        try {
            promotionClient.apply(new PromotionClient.ApplyRequest(
                order.getCouponCode(), order.getUserId(), order.getId(), order.getCouponDiscount()));
        } catch (Exception e) {
            log.warn("Sync coupon-usage call failed for order {} — event will be re-applied via Kafka: {}",
                order.getId(), e.getMessage());
        }
    }

    @Transactional
    public void failPayment(UUID orderId, String reason) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        cancelPaymentPendingOrder(order, reason == null ? "PAYMENT_FAILED" : reason, EventType.ORDER_CANCELLED);
    }

    /**
     * User-initiated order cancellation. Allowed only while the order has not
     * yet been dispatched (CREATED, PAYMENT_PENDING, CONFIRMED, PROCESSING).
     * Anything past SHIPPED requires the return flow instead.
     */
    @Transactional
    public OrderResponse cancelByUser(UUID userId, UUID orderId) {
        var order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        var allowed = Set.of(
            OrderStatus.CREATED,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.CONFIRMED,
            OrderStatus.PROCESSING
        );
        if (!allowed.contains(order.getStatus())) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.CONFLICT,
                "Bu sipariş artık iptal edilemez (durum: " + order.getStatus() + ")");
        }

        // Inventory release best-effort. Saga compensation flows will retry on failure.
        try {
            inventoryClient.release(order.getId());
        } catch (Exception e) {
            log.warn("Inventory release failed during user cancel for order {}: {}", order.getId(), e.getMessage());
        }

        // If payment was already captured (status CONFIRMED has paymentId set on
        // confirmPayment), trigger a full refund. Best-effort — failure logged and
        // a manual refund can be issued via the admin endpoint.
        if (order.getStatus() == OrderStatus.CONFIRMED && order.getPaymentId() != null) {
            try {
                paymentClient.refund(order.getPaymentId(),
                    new PaymentClient.RefundRequest(order.getGrandTotal(), "USER_CANCELLED"));
            } catch (Exception e) {
                log.error("Refund failed during user cancel for order {} payment {}: {}",
                    order.getId(), order.getPaymentId(), e.getMessage());
            }
        }

        order.transitionTo(OrderStatus.CANCELLED);
        order.setSagaState(SagaState.COMPENSATED.name());
        order.setSagaFailedAt(Instant.now());
        order.setSagaFailureReason("USER_CANCELLED");
        order.setCancelledAt(Instant.now());
        order = orderRepository.save(order);

        sagaLogRepository.save(successLog(order.getId(), "USER_CANCEL",
            objectMapper.valueToTree(Map.of("userId", userId.toString()))));
        outboxService.publish(Topics.ORDER_CANCELLED, EventType.ORDER_CANCELLED,
            order.getId().toString(), "ORDER",
            Map.of("orderId", order.getId(), "orderNumber", order.getOrderNumber(),
                "userId", order.getUserId(), "reason", "USER_CANCELLED"));

        return orderMapper.toResponse(order);
    }

    /**
     * Customer-driven delivery confirmation. Used by the "Siparişi Teslim Aldım"
     * button — moves SHIPPED → DELIVERED and publishes ORDER_RECEIVED so
     * notification/seller-rating/review-eligibility downstream can react.
     *
     * Idempotent: if the order is already at a terminal/post-delivery state
     * (DELIVERED/COMPLETED/RETURN_REQUESTED/REFUNDED) the call is a no-op and
     * returns the current view. Concurrent click protection is the user's
     * responsibility on the client; the server stays consistent either way.
     */
    @Transactional
    public OrderResponse markOrderReceived(UUID userId, UUID orderId) {
        var order = orderRepository.findByIdAndUserId(orderId, userId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));

        // Idempotent acknowledgement for already-acknowledged states.
        var current = order.getStatus();
        if (current == OrderStatus.DELIVERED
            || current == OrderStatus.COMPLETED
            || current == OrderStatus.RETURN_REQUESTED
            || current == OrderStatus.REFUNDED) {
            return orderMapper.toResponse(order);
        }
        if (current != OrderStatus.SHIPPED && current != OrderStatus.PROCESSING && current != OrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE_VIOLATION, HttpStatus.CONFLICT,
                "Bu sipariş için teslim alındı işaretlenemez (durum: " + current + ")");
        }

        // PROCESSING/CONFIRMED → SHIPPED → DELIVERED transition chain. We allow
        // confirming directly from PROCESSING/CONFIRMED to be tolerant of mock
        // shipment flows where SHIPPED was never recorded.
        if (current != OrderStatus.SHIPPED) {
            order.transitionTo(OrderStatus.SHIPPED);
        }
        order.transitionTo(OrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        order = orderRepository.save(order);

        var sellerIds = order.getItems().stream()
            .map(OrderItem::getSellerId)
            .distinct()
            .toList();
        sagaLogRepository.save(successLog(order.getId(), "ORDER_RECEIVED",
            objectMapper.valueToTree(Map.of("userId", userId.toString()))));
        outboxService.publish(Topics.ORDER_RECEIVED, EventType.ORDER_RECEIVED,
            order.getId().toString(), "ORDER",
            Map.of(
                "orderId", order.getId(),
                "orderNumber", order.getOrderNumber(),
                "userId", order.getUserId(),
                "sellerIds", sellerIds,
                "receivedAt", order.getDeliveredAt()
            ));
        return orderMapper.toResponse(order);
    }

    /**
     * Idempotently marks the order REFUNDED in response to PAYMENT_REFUNDED.
     * Used when a refund originates outside the order-service flow (admin refund,
     * return approval, etc.) so the order's lifecycle reflects the money state.
     */
    @Transactional
    public void markOrderRefunded(UUID orderId) {
        var order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found"));
        if (order.getStatus() == OrderStatus.REFUNDED) return;
        try {
            order.transitionTo(OrderStatus.REFUNDED);
            orderRepository.save(order);
        } catch (IllegalStateException e) {
            log.warn("Cannot transition order {} ({}) to REFUNDED: {}", orderId, order.getStatus(), e.getMessage());
        }
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
        order.transitionTo(OrderStatus.CANCELLED);
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
        sagaCompensations.increment();
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
