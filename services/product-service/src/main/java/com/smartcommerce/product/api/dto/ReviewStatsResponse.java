package com.smartcommerce.product.api.dto;

import java.util.Map;

public record ReviewStatsResponse(
    double averageRating,
    long totalCount,
    Map<Integer, Long> distribution
) {}
