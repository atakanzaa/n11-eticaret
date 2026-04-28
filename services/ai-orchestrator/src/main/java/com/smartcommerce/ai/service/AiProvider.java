package com.smartcommerce.ai.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Provider-agnostic interface for chat-with-tools LLM calls.
 *
 * <p>Adapters implement this for Anthropic, OpenAI, Gemini, etc. Spring picks
 * the active one via {@code @ConditionalOnProperty("ai.provider")}.
 *
 * <p>The shape is modeled after Anthropic Messages API (text + tool_use + tool_result blocks)
 * because it is the most expressive of the three. Other adapters translate between
 * this canonical shape and their native APIs.
 */
public interface AiProvider {

    /** Provider key, e.g. "anthropic", "openai", "gemini". */
    String getName();

    AiResponse chat(AiRequest request);

    record AiRequest(
        String systemPrompt,
        List<AiMessage> messages,
        List<AiToolDefinition> tools,
        UUID userId,
        String purpose
    ) {}

    record AiResponse(
        List<AiContentBlock> content,
        String stopReason,
        int inputTokens,
        int outputTokens,
        BigDecimal costUsd
    ) {}

    record AiToolDefinition(String name, String description, Map<String, Object> inputSchema) {}

    /**
     * One turn of the conversation.
     * <ul>
     *   <li>{@code role} is either {@code "user"} or {@code "assistant"}.</li>
     *   <li>If {@code text != null}: simple text message.</li>
     *   <li>If {@code blocks != null}: structured content (mixed text / tool_use / tool_result blocks).</li>
     * </ul>
     */
    record AiMessage(String role, String text, List<AiContentBlock> blocks) {
        public static AiMessage userText(String text) {
            return new AiMessage("user", text, null);
        }

        public static AiMessage assistantBlocks(List<AiContentBlock> blocks) {
            return new AiMessage("assistant", null, blocks);
        }

        public static AiMessage userToolResults(List<AiContentBlock> results) {
            return new AiMessage("user", null, results);
        }
    }

    /**
     * Content block.
     * <ul>
     *   <li>{@code type="text"}: plain assistant text in {@code text}.</li>
     *   <li>{@code type="tool_use"}: model wants to call {@code toolName} with {@code input};
     *       tracked by {@code toolUseId} so we can correlate the result.</li>
     *   <li>{@code type="tool_result"}: caller's response to a prior tool_use, identified by
     *       the same {@code toolUseId}; payload is in {@code text} (JSON-stringified).</li>
     * </ul>
     */
    record AiContentBlock(
        String type,
        String text,
        String toolUseId,
        String toolName,
        Map<String, Object> input
    ) {
        public static AiContentBlock text(String text) {
            return new AiContentBlock("text", text, null, null, null);
        }

        public static AiContentBlock toolUse(String id, String name, Map<String, Object> input) {
            return new AiContentBlock("tool_use", null, id, name, input);
        }

        public static AiContentBlock toolResult(String toolUseId, String content) {
            return new AiContentBlock("tool_result", content, toolUseId, null, null);
        }
    }
}
