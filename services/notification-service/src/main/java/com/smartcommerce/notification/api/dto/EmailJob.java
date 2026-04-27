package com.smartcommerce.notification.api.dto;

import java.util.Map;
import java.util.UUID;

public record EmailJob(
    UUID userId,
    String templateCode,
    String recipient,
    String subject,
    Map<String, Object> model,
    String correlationId
) {
}
