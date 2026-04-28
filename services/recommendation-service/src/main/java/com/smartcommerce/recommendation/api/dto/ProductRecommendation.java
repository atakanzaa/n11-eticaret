package com.smartcommerce.recommendation.api.dto;

import java.util.UUID;

public record ProductRecommendation(UUID productId, double score, String reason) {
}
