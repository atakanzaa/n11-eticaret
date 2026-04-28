package com.smartcommerce.recommendation.domain;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "co_purchase_pairs")
@IdClass(CoPurchasePair.PairId.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoPurchasePair {
    @Id
    @Column(name = "product_a_id")
    private UUID productAId;

    @Id
    @Column(name = "product_b_id")
    private UUID productBId;

    @Column(name = "co_purchase_count", nullable = false)
    @Builder.Default
    private Integer coPurchaseCount = 1;

    @Column(name = "last_seen_at", nullable = false)
    @Builder.Default
    private Instant lastSeenAt = Instant.now();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PairId implements Serializable {
        private UUID productAId;
        private UUID productBId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PairId other)) return false;
            return Objects.equals(productAId, other.productAId)
                && Objects.equals(productBId, other.productBId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productAId, productBId);
        }
    }
}
