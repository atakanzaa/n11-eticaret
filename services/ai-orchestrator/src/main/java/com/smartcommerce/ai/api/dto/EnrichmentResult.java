package com.smartcommerce.ai.api.dto;

import java.util.List;

public record EnrichmentResult(
    String description,
    List<String> bullets,
    List<String> keywords
) {
}
