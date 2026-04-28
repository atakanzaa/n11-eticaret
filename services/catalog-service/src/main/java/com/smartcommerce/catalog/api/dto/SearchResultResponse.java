package com.smartcommerce.catalog.api.dto;

import com.smartcommerce.catalog.domain.ProductDocument;

import java.util.List;
import java.util.Map;

public record SearchResultResponse(
    List<ProductDocument> hits,
    long total,
    int page,
    int size,
    Map<String, Map<String, Long>> facets
) {
}
