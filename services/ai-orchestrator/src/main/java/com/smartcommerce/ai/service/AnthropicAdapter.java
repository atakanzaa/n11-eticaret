package com.smartcommerce.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.ai.repository.AiUsageLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "anthropic", matchIfMissing = true)
@Slf4j
public class AnthropicAdapter extends AbstractAiAdapter {

    private static final String DEFAULT_MODEL = "claude-sonnet-4-6";

    private static final Map<String, Pricing> PRICING = Map.of(
        "claude-sonnet-4-6", Pricing.of(3.0, 15.0),
        "claude-opus-4-7", Pricing.of(15.0, 75.0),
        "claude-haiku-4-5", Pricing.of(0.25, 1.25)
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;
    private final int timeoutSeconds;

    private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";

    public AnthropicAdapter(AiUsageLogRepository usageRepository,
                            AiBudgetGuard budgetGuard,
                            ObjectMapper objectMapper,
                            @Value("${ai.api-key:dummy}") String apiKey,
                            @Value("${ai.base-url:}") String baseUrl,
                            @Value("${ai.model:}") String model,
                            @Value("${ai.max-tokens:4096}") int maxTokens,
                            @Value("${ai.timeout-seconds:60}") int timeoutSeconds) {
        super(usageRepository, budgetGuard);
        this.objectMapper = objectMapper;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        var resolvedBaseUrl = (baseUrl == null || baseUrl.isBlank()) ? DEFAULT_BASE_URL : baseUrl;
        this.webClient = WebClient.builder()
            .baseUrl(resolvedBaseUrl)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", "2023-06-01")
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Override
    public String getName() {
        return "anthropic";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    protected Pricing pricingFor(String model) {
        return PRICING.getOrDefault(model, PRICING.get(DEFAULT_MODEL));
    }

    @Override
    @CircuitBreaker(name = "anthropic")
    protected RawResult invoke(AiRequest request) {
        var body = new HashMap<String, Object>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            body.put("system", request.systemPrompt());
        }
        body.put("messages", request.messages().stream().map(this::toAnthropicMessage).toList());
        if (request.tools() != null && !request.tools().isEmpty()) {
            body.put("tools", request.tools().stream()
                .map(t -> Map.of("name", t.name(), "description", t.description(),
                    "input_schema", t.inputSchema()))
                .toList());
        }

        var response = requireSuccess(
            webClient.post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block(),
            "response body");

        return parseResponse(response);
    }

    private Map<String, Object> toAnthropicMessage(AiMessage m) {
        if (m.text() != null) {
            return Map.of("role", m.role(), "content", m.text());
        }
        var contentBlocks = new ArrayList<Map<String, Object>>();
        for (var block : m.blocks()) {
            switch (block.type()) {
                case "text" -> contentBlocks.add(Map.of("type", "text", "text", block.text()));
                case "tool_use" -> contentBlocks.add(Map.of(
                    "type", "tool_use",
                    "id", block.toolUseId(),
                    "name", block.toolName(),
                    "input", block.input() != null ? block.input() : Map.of()));
                case "tool_result" -> contentBlocks.add(Map.of(
                    "type", "tool_result",
                    "tool_use_id", block.toolUseId(),
                    "content", block.text() != null ? block.text() : ""));
                default -> log.warn("Unknown block type for Anthropic: {}", block.type());
            }
        }
        return Map.of("role", m.role(), "content", contentBlocks);
    }

    @SuppressWarnings("unchecked")
    private RawResult parseResponse(JsonNode json) {
        var blocks = new ArrayList<AiContentBlock>();
        for (var node : json.path("content")) {
            var type = node.path("type").asText();
            if ("text".equals(type)) {
                blocks.add(AiContentBlock.text(node.path("text").asText()));
            } else if ("tool_use".equals(type)) {
                var input = (Map<String, Object>) objectMapper.convertValue(node.path("input"), Map.class);
                blocks.add(AiContentBlock.toolUse(
                    node.path("id").asText(),
                    node.path("name").asText(),
                    input != null ? input : Map.of()));
            }
        }
        return new RawResult(
            blocks,
            json.path("stop_reason").asText(),
            json.path("usage").path("input_tokens").asInt(),
            json.path("usage").path("output_tokens").asInt());
    }
}
