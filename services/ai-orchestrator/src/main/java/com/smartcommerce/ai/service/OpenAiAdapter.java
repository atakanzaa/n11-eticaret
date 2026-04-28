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
 * OpenAI Chat Completions adapter.
 *
 * <p>Notable shape differences from Anthropic:
 * <ul>
 *   <li>Tool definitions wrapped as {@code {type: "function", function: {...}}}.</li>
 *   <li>Tool calls live on the assistant message's {@code tool_calls[]}.</li>
 *   <li>Tool results are separate messages with {@code role: "tool"} and {@code tool_call_id}.</li>
 *   <li>Tool call arguments are a JSON string that needs parsing.</li>
 *   <li>Tokens come back as {@code prompt_tokens}/{@code completion_tokens}.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "ai.provider", havingValue = "openai")
@Slf4j
public class OpenAiAdapter extends AbstractAiAdapter {

    private static final String DEFAULT_MODEL = "gpt-4o-mini";

    private static final Map<String, Pricing> PRICING = Map.of(
        "gpt-4o", Pricing.of(2.5, 10.0),
        "gpt-4o-mini", Pricing.of(0.15, 0.60),
        "gpt-4-turbo", Pricing.of(10.0, 30.0)
    );

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String model;
    private final int maxTokens;
    private final int timeoutSeconds;

    private static final String DEFAULT_BASE_URL = "https://api.openai.com";

    public OpenAiAdapter(AiUsageLogRepository usageRepository,
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
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();
    }

    @Override
    public String getName() {
        return "openai";
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
    @CircuitBreaker(name = "openai")
    protected RawResult invoke(AiRequest request) {
        var openAiMessages = new ArrayList<Map<String, Object>>();
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            openAiMessages.add(Map.of("role", "system", "content", request.systemPrompt()));
        }
        for (var m : request.messages()) {
            openAiMessages.addAll(toOpenAiMessages(m));
        }

        var body = new HashMap<String, Object>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("messages", openAiMessages);
        if (request.tools() != null && !request.tools().isEmpty()) {
            body.put("tools", request.tools().stream()
                .map(t -> Map.of("type", "function", "function", Map.of(
                    "name", t.name(),
                    "description", t.description(),
                    "parameters", t.inputSchema())))
                .toList());
        }

        var response = requireSuccess(
            webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .block(),
            "response body");

        return parseResponse(response);
    }

    /** A single canonical message can become 1+ OpenAI messages (e.g. tool_results split). */
    private List<Map<String, Object>> toOpenAiMessages(AiMessage m) {
        if (m.text() != null) {
            return List.of(Map.of("role", m.role(), "content", m.text()));
        }
        var out = new ArrayList<Map<String, Object>>();
        if ("assistant".equals(m.role())) {
            // assistant message: combine text blocks + tool_calls in one message
            var textParts = m.blocks().stream()
                .filter(b -> "text".equals(b.type()))
                .map(AiContentBlock::text)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
            var toolCalls = m.blocks().stream()
                .filter(b -> "tool_use".equals(b.type()))
                .map(b -> Map.<String, Object>of(
                    "id", b.toolUseId(),
                    "type", "function",
                    "function", Map.of(
                        "name", b.toolName(),
                        "arguments", serialize(b.input() != null ? b.input() : Map.of()))))
                .toList();
            var msg = new HashMap<String, Object>();
            msg.put("role", "assistant");
            msg.put("content", textParts.isEmpty() ? null : textParts);
            if (!toolCalls.isEmpty()) msg.put("tool_calls", toolCalls);
            out.add(msg);
        } else {
            // user message: split tool_results into individual {role: tool} messages
            for (var block : m.blocks()) {
                if ("tool_result".equals(block.type())) {
                    out.add(Map.of(
                        "role", "tool",
                        "tool_call_id", block.toolUseId(),
                        "content", block.text() != null ? block.text() : ""));
                } else if ("text".equals(block.type())) {
                    out.add(Map.of("role", "user", "content", block.text()));
                }
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private RawResult parseResponse(JsonNode json) {
        var choice = json.path("choices").path(0);
        var message = choice.path("message");
        var blocks = new ArrayList<AiContentBlock>();
        var content = message.path("content");
        if (content.isTextual() && !content.asText().isBlank()) {
            blocks.add(AiContentBlock.text(content.asText()));
        }
        for (var call : message.path("tool_calls")) {
            var fn = call.path("function");
            var argsText = fn.path("arguments").asText("{}");
            Map<String, Object> args;
            try {
                args = (Map<String, Object>) objectMapper.readValue(argsText, Map.class);
            } catch (Exception e) {
                log.warn("Failed to parse tool arguments JSON: {}", argsText);
                args = Map.of();
            }
            blocks.add(AiContentBlock.toolUse(
                call.path("id").asText(UUID.randomUUID().toString()),
                fn.path("name").asText(),
                args));
        }
        return new RawResult(
            blocks,
            choice.path("finish_reason").asText(),
            json.path("usage").path("prompt_tokens").asInt(),
            json.path("usage").path("completion_tokens").asInt());
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "{}";
        }
    }
}
