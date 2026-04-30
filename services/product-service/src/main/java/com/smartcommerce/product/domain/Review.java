package com.smartcommerce.product.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    /** Snapshotted at write-time so listing reviews never has to call user-service. */
    @Column(name = "user_display_name", nullable = false)
    private String userDisplayName;

    @Column(nullable = false)
    private int rating;

    @Column(length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** Free-form variant fingerprint, e.g. "Renk: Siyah, Beden: M". */
    @Column(name = "variant_info", length = 500)
    private String variantInfo;

    @Column(name = "helpful_count", nullable = false)
    private int helpfulCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReviewStatus status;

    /** The order that justifies this review's authorship — verified-purchase guarantee. */
    @Column(name = "order_id")
    private UUID orderId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
