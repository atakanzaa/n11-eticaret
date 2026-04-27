package com.smartcommerce.notification.api.dto;

import com.smartcommerce.notification.domain.*;

import java.time.Instant;
import java.util.UUID;

public record NotificationLogResponse(
    UUID id,
    UUID userId,
    NotificationChannel channel,
    String templateCode,
    String recipient,
    String subject,
    NotificationStatus status,
    String correlationId,
    String errorMessage,
    Instant sentAt,
    Instant createdAt
) {
}
