package com.smartcommerce.recommendation.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "product_popularity")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductPopularity {
    @Id
    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "view_count", nullable = false)
    @Builder.Default
    private Long viewCount = 0L;

    @Column(name = "cart_add_count", nullable = false)
    @Builder.Default
    private Long cartAddCount = 0L;

    @Column(name = "purchase_count", nullable = false)
    @Builder.Default
    private Long purchaseCount = 0L;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal score = BigDecimal.ZERO;

    @Column(name = "last_calculated_at", nullable = false)
    @Builder.Default
    private Instant lastCalculatedAt = Instant.now();

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
