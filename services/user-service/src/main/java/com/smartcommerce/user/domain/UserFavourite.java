package com.smartcommerce.user.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_favourites",
    uniqueConstraints = @UniqueConstraint(name = "uq_user_favourite", columnNames = {"user_id", "product_id"})
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserFavourite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private Instant addedAt;
}
