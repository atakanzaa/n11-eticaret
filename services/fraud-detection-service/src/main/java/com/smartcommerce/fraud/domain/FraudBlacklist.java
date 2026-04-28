package com.smartcommerce.fraud.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudBlacklist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "blacklist_type", nullable = false, length = 20)
    private String blacklistType;

    @Column(nullable = false)
    private String value;

    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
