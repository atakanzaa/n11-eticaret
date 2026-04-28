package com.smartcommerce.ai.api.dto;

import java.util.UUID;

public record ChatResponse(
    UUID conversationId,
    String message
) {
}
