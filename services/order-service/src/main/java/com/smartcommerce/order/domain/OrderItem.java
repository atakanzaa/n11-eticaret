package com.smartcommerce.order.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;
    @Column(name = "product_id", nullable = false)
    private UUID productId;
    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price", nullable = false)
    private BigDecimal unitPrice;
    @Column(name = "line_total", nullable = false)
    private BigDecimal lineTotal;
    @Column(name = "product_title", nullable = false)
    private String productTitle;
    @Column(name = "product_image_url")
    private String productImageUrl;
    @Column(name = "seller_name", nullable = false)
    private String sellerName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
