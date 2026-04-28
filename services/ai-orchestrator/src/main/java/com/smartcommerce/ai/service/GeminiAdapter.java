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
import java.util.UUID;

/**
 * Google Gemini adapter (v1beta API).
 *
 * <p>Notable shape differences:
 * <ul>
 *   <li>Roles are {@code "user"} and {@code "model"} (not "assistant").</li>
 *   <li>Each message has {@code parts[]} which can be text, functionCall, or functionResponse.</li>
 *   <li>Tools defined under {@code tools[].functionDeclarations[]}.</li>
 *   <li>System prompt goes in {@code systemInstruction.parts[].text}, NOT in messages.</li>
 *   <li>Token counts: {@code usageMetadata.promptTokenCount} / {@code candidatesTokenCount}.</li>
 *   <li>API key passed via {@code x-goog-api-key} header.</li>
 *   <li>Function call responses use synthesized id (model doesn't return one) — we map by name.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "gemini")
@Slf4j
public class GeminiAdapter extends AbstractAiAdapter {

    private static final String DEFAULT_MODEL = "gemini-2.0-flash";

    private static final Map<String, Pricing> PRICING = Map.of(
        "gemini-1.5-pro", Pricing.of(1.25, 5.0),
        "gemini-1.5-flash", Pricing.of(0.075, 0.30),
        "gemini-2.0-flash", Pricing.of(0.10, 0.40),
        "gemini-2.5-pro", Pricing.of(1.25, 10.0)
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;
    private final int timeoutSeconds;

    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    public GeminiAdapter(AiUsageLogRepository usageRepository,
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
            .defaultHeader("x-goog-api-key", apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Override
    public String getName() {
        return "gemini";
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    protected Pricing pricingFor(String model) {
        return PRICING.getOrDefault(model, Pricing.zero());
    }

    @Override
    @CircuitBreaker(name = "gemini")
    protected RawResult invoke(AiRequest request) {
        var contents = new ArrayList<Map<String, Object>>();
        for (var m : request.messages()) {
            contents.add(toGeminiContent(m));
        }

        var body = new HashMap<String, Object>();
        body.put("contents", contents);
        body.put("generationConfig", Map.of(
            "maxOutputTokens", maxTokens,
            "temperature", 0.7));

        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            body.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", request.systemPrompt()))));
        }

        if (request.tools() != null && !request.tools().isEmpty()) {
            var declarations = request.tools().stream()
                .map(t -> Map.<String, Object>of(
                    "name", t.name(),
                    "description", t.description(),
                    "parameters", t.inputSchema()))
                .toList();
            body.put("tools", List.of(Map.of("functionDeclarations", declarations)));
        }

        var response = requireSuccess(
            webClient.post()
                .uri("/v1beta/models/{model}:generateContent", model)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block(),
            "response body");

        return parseResponse(response);
    }

    private Map<String, Object> toGeminiContent(AiMessage m) {
        var role = "assistant".equals(m.role()) ? "model" : "user";
        var parts = new ArrayList<Map<String, Object>>();
        if (m.text() != null) {
            parts.add(Map.of("text", m.text()));
        } else {
            for (var block : m.blocks()) {
                switch (block.type()) {
                    case "text" -> parts.add(Map.of("text", block.text()));
                    case "tool_use" -> parts.add(Map.of("functionCall", Map.of(
                        "name", block.toolName(),
                        "args", block.input() != null ? block.input() : Map.of())));
                    case "tool_result" -> parts.add(Map.of("functionResponse", Map.of(
                        "name", block.toolName() != null ? block.toolName() : extractFnNameFromId(block.toolUseId()),
                        "response", Map.of("result", block.text() != null ? block.text() : ""))));
                    default -> log.warn("Unknown block type for Gemini: {}", block.type());
                }
            }
        }
        return Map.of("role", role, "parts", parts);
    }

    /** Gemini doesn't track call IDs — synthesize from function name. */
    private String extractFnNameFromId(String toolUseId) {
        if (toolUseId == null || !toolUseId.contains(":")) return "unknown";
        return toolUseId.substring(toolUseId.indexOf(':') + 1);
    }

    @SuppressWarnings("unchecked")
    private RawResult parseResponse(JsonNode json) {
        var candidate = json.path("candidates").path(0);
        var blocks = new ArrayList<AiContentBlock>();
        for (var part : candidate.path("content").path("parts")) {
            if (part.has("text")) {
                blocks.add(AiContentBlock.text(part.path("text").asText()));
            } else if (part.has("functionCall")) {
                var fc = part.path("functionCall");
                var fnName = fc.path("name").asText();
                var args = (Map<String, Object>) objectMapper.convertValue(fc.path("args"), Map.class);
                // Gemini doesn't issue call IDs — synthesize one tied to the function name
                // so tool_result can map back via extractFnNameFromId.
                blocks.add(AiContentBlock.toolUse(
                    UUID.randomUUID() + ":" + fnName,
                    fnName,
                    args != null ? args : Map.of()));
            }
        }
        var usage = json.path("usageMetadata");
        return new RawResult(
            blocks,
            candidate.path("finishReason").asText(),
            usage.path("promptTokenCount").asInt(),
            usage.path("candidatesTokenCount").asInt());
    }
}
