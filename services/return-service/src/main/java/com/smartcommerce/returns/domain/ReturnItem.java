package com.smartcommerce.returns.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "order_item_id", nullable = false)
    private UUID orderItemId;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;

    @Column(name = "refund_amount", nullable = false)
    private BigDecimal refundAmount;

    @Column(name = "item_status", nullable = false, length = 20)
    @Builder.Default
    private String itemStatus = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
