package com.smartcommerce.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.ai.api.dto.EnrichmentResult;
import com.smartcommerce.ai.client.ProductClient;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductEnrichmentService {

    private static final String ENRICHMENT_SYSTEM_PROMPT = """
        You are a Turkish e-commerce content writer for SmartCommerce.
        Given a product's title, brand, category, and basic attributes, generate:
        1. An SEO-friendly description (200-300 words, Turkish, persuasive but factual).
        2. 5 marketing bullet points highlighting key features.
        3. 3-5 relevant search keywords for the product.

        Output STRICT JSON only, no markdown or code fences:
        {
          "description": "...",
          "bullets": ["...", "...", "...", "...", "..."],
          "keywords": ["...", "...", "..."]
        }

        Never invent technical specs not provided. Stick to facts.
        """;

    private final AnthropicService anthropic;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper;

    public EnrichmentResult enrich(UUID productId) {
        var product = productClient.getProduct(productId);
        var input = serialize(Map.of(
            "title", product.getOrDefault("title", ""),
            "brandId", product.getOrDefault("brandId", ""),
            "categoryId", product.getOrDefault("categoryId", ""),
            "shortDescription", product.getOrDefault("shortDescription", ""),
            "attributes", product.getOrDefault("attributes", Map.of())
        ));

        var response = anthropic.chat(
            List.of(new AnthropicService.Message("user", input)),
            ENRICHMENT_SYSTEM_PROMPT, null, null, "PRODUCT_DESCRIPTION");

        var jsonText = response.content().stream()
            .filter(b -> "text".equals(b.type()))
            .findFirst()
            .map(AnthropicService.ContentBlock::text)
            .orElseThrow(() -> new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "AI returned no text content"));

        var trimmed = stripCodeFence(jsonText.trim());
        try {
            return objectMapper.readValue(trimmed, EnrichmentResult.class);
        } catch (Exception e) {
            log.error("Failed to parse enrichment JSON: {}", trimmed, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR,
                "AI returned malformed enrichment JSON");
        }
    }

    private String stripCodeFence(String text) {
        if (text.startsWith("```")) {
            var firstNewline = text.indexOf('\n');
            if (firstNewline > 0) text = text.substring(firstNewline + 1);
            var fenceIdx = text.lastIndexOf("```");
            if (fenceIdx >= 0) text = text.substring(0, fenceIdx);
        }
        return text.trim();
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
