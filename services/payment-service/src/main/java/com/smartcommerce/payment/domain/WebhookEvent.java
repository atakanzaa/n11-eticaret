package com.smartcommerce.payment.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "provider_event_id")
    private String providerEventId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(length = 500)
    private String signature;

    @Column(name = "signature_valid")
    private Boolean signatureValid;

    @Column(nullable = false)
    @Builder.Default
    private boolean processed = false;

    @Column(name = "processing_error", columnDefinition = "text")
    private String processingError;

    @CreationTimestamp
    @Column(name = "received_at", updatable = false)
    private Instant receivedAt;

    @Column(name = "processed_at")
    private Instant processedAt;
}
