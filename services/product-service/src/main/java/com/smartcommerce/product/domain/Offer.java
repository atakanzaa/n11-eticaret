package com.smartcommerce.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "offers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Offer {
    private static final BigDecimal PRICE_CHANGE_THRESHOLD = new BigDecimal("0.01");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private String currency;

    @Column(name = "list_price")
    private BigDecimal listPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "cargo_provider", nullable = false)
    private CargoProvider cargoProvider;

    @Column(name = "cargo_price", nullable = false)
    private BigDecimal cargoPrice;

    @Column(name = "estimated_delivery_days", nullable = false)
    private int estimatedDeliveryDays;

    @Column(name = "free_shipping_threshold")
    private BigDecimal freeShippingThreshold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;

    public boolean priceChanged(BigDecimal newPrice) {
        if (price == null || newPrice == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }
        var difference = price.subtract(newPrice).abs();
        var ratio = difference.divide(price, 4, RoundingMode.HALF_UP);
        return ratio.compareTo(PRICE_CHANGE_THRESHOLD) > 0 || price.compareTo(newPrice) != 0;
    }

    public void normalizeMoney() {
        price = price.setScale(2, RoundingMode.HALF_UP);
        if (listPrice != null) listPrice = listPrice.setScale(2, RoundingMode.HALF_UP);
        cargoPrice = cargoPrice == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : cargoPrice.setScale(2, RoundingMode.HALF_UP);
        if (freeShippingThreshold != null) freeShippingThreshold = freeShippingThreshold.setScale(2, RoundingMode.HALF_UP);
    }
}
