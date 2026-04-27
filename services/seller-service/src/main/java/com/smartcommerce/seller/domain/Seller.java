package com.smartcommerce.seller.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "sellers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column(name = "rating_count")
    private Integer ratingCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SellerStatus status;

    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
