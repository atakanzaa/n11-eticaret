package com.smartcommerce.cart.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_snapshot", nullable = false)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "currency_snapshot", nullable = false)
    private String currencySnapshot;

    @Column(name = "product_title_snapshot", nullable = false)
    private String productTitleSnapshot;

    @Column(name = "product_image_snapshot")
    private String productImageSnapshot;

    @Column(name = "seller_name_snapshot")
    private String sellerNameSnapshot;

    @Column(name = "cargo_price_snapshot")
    private BigDecimal cargoPriceSnapshot;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Instant addedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public BigDecimal subtotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }
}
