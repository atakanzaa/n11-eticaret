package com.smartcommerce.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "shipping_full_name", nullable = false)
    private String shippingFullName;
    @Column(name = "shipping_phone", nullable = false)
    private String shippingPhone;
    @Column(name = "shipping_country", nullable = false)
    private String shippingCountry;
    @Column(name = "shipping_city", nullable = false)
    private String shippingCity;
    @Column(name = "shipping_district", nullable = false)
    private String shippingDistrict;
    @Column(name = "shipping_full_address", nullable = false)
    private String shippingFullAddress;
    @Column(name = "shipping_postal_code")
    private String shippingPostalCode;
    @Column(name = "billing_full_name")
    private String billingFullName;
    @Column(name = "billing_full_address")
    private String billingFullAddress;

    @Column(nullable = false)
    private BigDecimal subtotal;
    @Column(name = "shipping_total", nullable = false)
    private BigDecimal shippingTotal;
    @Column(name = "discount_total", nullable = false)
    private BigDecimal discountTotal;
    @Column(name = "tax_total", nullable = false)
    private BigDecimal taxTotal;
    @Column(name = "grand_total", nullable = false)
    private BigDecimal grandTotal;
    @Column(nullable = false)
    private String currency;

    @Column(name = "payment_method")
    private String paymentMethod;
    @Column(name = "payment_id")
    private UUID paymentId;
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @Column(name = "saga_state")
    private String sagaState;
    @Column(name = "saga_completed_at")
    private Instant sagaCompletedAt;
    @Column(name = "saga_failed_at")
    private Instant sagaFailedAt;
    @Column(name = "saga_failure_reason")
    private String sagaFailureReason;
    @Column(name = "confirmed_at")
    private Instant confirmedAt;
    @Column(name = "cancelled_at")
    private Instant cancelledAt;
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "coupon_code", length = 50)
    private String couponCode;
    @Column(name = "coupon_discount")
    private BigDecimal couponDiscount;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
    @Version
    private Long version;

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    public boolean canCancel() {
        return status == OrderStatus.CREATED || status == OrderStatus.PAYMENT_PENDING;
    }

    /**
     * Enforces the OrderStatus state machine. Throws IllegalStateException when
     * the requested transition is not allowed by OrderStatus.canTransitionTo,
     * preventing data-integrity bugs like CANCELLED → CONFIRMED. Self-transitions
     * (status == target) are no-ops and accepted.
     */
    public void transitionTo(OrderStatus target) {
        if (target == null) throw new IllegalArgumentException("target status is null");
        if (this.status != null && !this.status.canTransitionTo(target)) {
            throw new IllegalStateException(
                "Illegal order status transition: " + this.status + " -> " + target + " (orderId=" + id + ")");
        }
        this.status = target;
    }
}
