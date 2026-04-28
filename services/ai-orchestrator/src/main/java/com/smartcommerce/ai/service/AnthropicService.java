package com.smartcommerce.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.ai.domain.AiUsageLog;
import com.smartcommerce.ai.repository.AiUsageLogRepository;
import com.smartcommerce.common.errors.BusinessException;
import com.smartcommerce.common.errors.ErrorCode;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnthropicService {

    private final WebClient anthropicWebClient;
    private final AiUsageLogRepository usageRepository;
    private final ObjectMapper objectMapper;
    private final AiBudgetGuard budgetGuard;

    @Value("${anthropic.model}")
    private String model;

    @Value("${anthropic.max-tokens:4096}")
    private int maxTokens;

    @Value("${anthropic.timeout-seconds:60}")
    private int timeoutSeconds;

    @Value("${ai.pricing.input-per-million-tokens-usd:3.0}")
    private BigDecimal inputPricePerMillion;

    @Value("${ai.pricing.output-per-million-tokens-usd:15.0}")
    private BigDecimal outputPricePerMillion;

    @CircuitBreaker(name = "anthropic")
    public AnthropicResponse chat(List<Message> messages, String systemPrompt,
                                   List<ToolDefinition> tools, UUID userId, String purpose) {
        budgetGuard.check();

        var requestBody = new HashMap<String, Object>();
        requestBody.put("model", model);
        requestBody.put("max_tokens", maxTokens);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            requestBody.put("system", systemPrompt);
        }
        requestBody.put("messages", messages);
        if (tools != null && !tools.isEmpty()) {
            requestBody.put("tools", tools);
        }

        var start = Instant.now();
        try {
            var response = anthropicWebClient.post()
                .uri("/v1/messages")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block();

            if (response == null) {
                throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE, "Empty Anthropic response");
            }
            var duration = Duration.between(start, Instant.now()).toMillis();
            var inputTokens = response.path("usage").path("input_tokens").asInt();
            var outputTokens = response.path("usage").path("output_tokens").asInt();
            var cost = calculateCost(inputTokens, outputTokens);

            usageRepository.save(AiUsageLog.builder()
                .userId(userId).purpose(purpose).model(model)
                .inputTokens(inputTokens).outputTokens(outputTokens).costUsd(cost)
                .durationMs((int) duration).success(true).build());

            return parseResponse(response);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Anthropic API call failed", e);
            usageRepository.save(AiUsageLog.builder()
                .userId(userId).purpose(purpose).model(model)
                .durationMs((int) Duration.between(start, Instant.now()).toMillis())
                .success(false).errorMessage(e.getMessage()).build());
            throw new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE,
                "AI service unavailable: " + e.getMessage());
        }
    }

    private BigDecimal calculateCost(int inputTokens, int outputTokens) {
        var perToken = BigDecimal.valueOf(1_000_000);
        var inputCost = inputPricePerMillion.multiply(BigDecimal.valueOf(inputTokens))
            .divide(perToken, 6, RoundingMode.HALF_UP);
        var outputCost = outputPricePerMillion.multiply(BigDecimal.valueOf(outputTokens))
            .divide(perToken, 6, RoundingMode.HALF_UP);
        return inputCost.add(outputCost).setScale(6, RoundingMode.HALF_UP);
    }

    private AnthropicResponse parseResponse(JsonNode json) {
        var contentBlocks = new ArrayList<ContentBlock>();
        for (var block : json.path("content")) {
            var type = block.path("type").asText();
            if ("text".equals(type)) {
                contentBlocks.add(new ContentBlock("text", block.path("text").asText(), null, null, null));
            } else if ("tool_use".equals(type)) {
                @SuppressWarnings("unchecked")
                var input = (Map<String, Object>) objectMapper.convertValue(block.path("input"), Map.class);
                contentBlocks.add(new ContentBlock("tool_use", null,
                    block.path("id").asText(),
                    block.path("name").asText(),
                    input != null ? input : Map.of()));
            }
        }
        return new AnthropicResponse(
            contentBlocks,
            json.path("stop_reason").asText(),
            json.path("usage").path("input_tokens").asInt(),
            json.path("usage").path("output_tokens").asInt());
    }

    public record Message(String role, Object content) {
    }

    public record ToolDefinition(String name, String description, Map<String, Object> input_schema) {
    }

    public record ContentBlock(String type, String text, String toolUseId, String toolName, Map<String, Object> input) {
    }

    public record AnthropicResponse(List<ContentBlock> content, String stopReason, int inputTokens, int outputTokens) {
    }
}
