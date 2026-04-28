package com.smartcommerce.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.ai.api.dto.ChatResponse;
import com.smartcommerce.ai.client.McpClient;
import com.smartcommerce.ai.domain.AiConversation;
import com.smartcommerce.ai.domain.AiMessage;
import com.smartcommerce.ai.repository.AiConversationRepository;
import com.smartcommerce.ai.repository.AiMessageRepository;
import com.smartcommerce.ai.service.AiProvider.AiContentBlock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShoppingAssistantService {

    private static final int MAX_TOOL_USE_ITERATIONS = 5;

    private static final String SYSTEM_PROMPT = """
        You are a helpful shopping assistant for SmartCommerce, a Turkish marketplace.
        You help customers find products, compare offers, and get personalized recommendations.

        IMPORTANT GUARDRAILS:
        - Only recommend products from search results — never invent product IDs or details.
        - Communicate in the same language the user uses (Turkish or English).
        - When the user asks for products, ALWAYS use the search_products tool first.
        - Format prices clearly with TL/TRY currency.
        - Be concise: 2-3 short paragraphs maximum unless the user asks for details.
        - If a tool returns no results, say so honestly.
        - Never make up prices or stock info.
        - Do not discuss topics unrelated to shopping.
        """;

    private final AiProvider aiProvider;
    private final McpClient mcpClient;
    private final AiConversationRepository conversationRepo;
    private final AiMessageRepository messageRepo;
    private final ObjectMapper objectMapper;

    @Transactional
    public ChatResponse chat(UUID userId, UUID conversationId, String userMessage) {
        var conversation = conversationId != null
            ? conversationRepo.findById(conversationId).orElseGet(() -> createConversation(userId))
            : createConversation(userId);

        var history = messageRepo.findTop20ByConversationIdOrderByCreatedAtAsc(conversation.getId());
        var messages = new ArrayList<AiProvider.AiMessage>();
        for (var m : history) {
            if ("USER".equals(m.getRole()) && m.getContent() != null) {
                messages.add(AiProvider.AiMessage.userText(m.getContent()));
            } else if ("ASSISTANT".equals(m.getRole()) && m.getContent() != null) {
                messages.add(AiProvider.AiMessage.assistantBlocks(
                    List.of(AiContentBlock.text(m.getContent()))));
            }
        }
        messages.add(AiProvider.AiMessage.userText(userMessage));

        messageRepo.save(AiMessage.builder()
            .conversationId(conversation.getId()).role("USER").content(userMessage).build());

        var mcpTools = mcpClient.listTools();
        var toolDefs = mcpTools.stream()
            .map(t -> new AiProvider.AiToolDefinition(t.name(), t.description(), t.inputSchema()))
            .toList();

        AiProvider.AiResponse response = null;
        for (var iter = 0; iter < MAX_TOOL_USE_ITERATIONS; iter++) {
            response = aiProvider.chat(new AiProvider.AiRequest(
                SYSTEM_PROMPT, messages, toolDefs, userId, "CHAT"));

            var toolUses = response.content().stream()
                .filter(b -> "tool_use".equals(b.type()))
                .toList();
            if (toolUses.isEmpty()) break;

            messages.add(AiProvider.AiMessage.assistantBlocks(response.content()));

            var toolResults = new ArrayList<AiContentBlock>();
            for (var toolUse : toolUses) {
                try {
                    var result = mcpClient.invokeTool(toolUse.toolName(), toolUse.input());
                    var serialized = objectMapper.writeValueAsString(result);
                    toolResults.add(AiContentBlock.toolResult(toolUse.toolUseId(), serialized));
                    messageRepo.save(AiMessage.builder()
                        .conversationId(conversation.getId()).role("TOOL_USE")
                        .toolName(toolUse.toolName())
                        .toolInput(objectMapper.valueToTree(toolUse.input()))
                        .toolResult(objectMapper.valueToTree(result))
                        .build());
                } catch (Exception e) {
                    log.error("MCP tool {} invocation failed", toolUse.toolName(), e);
                    toolResults.add(AiContentBlock.toolResult(toolUse.toolUseId(),
                        "Error: " + e.getMessage()));
                }
            }
            messages.add(AiProvider.AiMessage.userToolResults(toolResults));
        }

        var finalText = response == null ? "" : response.content().stream()
            .filter(b -> "text".equals(b.type()))
            .map(AiContentBlock::text)
            .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);

        messageRepo.save(AiMessage.builder()
            .conversationId(conversation.getId()).role("ASSISTANT").content(finalText)
            .tokenCount(response != null ? response.outputTokens() : 0).build());

        conversation.setUpdatedAt(java.time.Instant.now());
        conversationRepo.save(conversation);

        return new ChatResponse(conversation.getId(), finalText);
    }

    private AiConversation createConversation(UUID userId) {
        return conversationRepo.save(AiConversation.builder()
            .userId(userId).title("New conversation").build());
    }

    public List<AiConversation> myConversations(UUID userId) {
        return conversationRepo.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    public List<AiMessage> conversationMessages(UUID conversationId) {
        return messageRepo.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @SuppressWarnings("unused")
    private static String safeJson(ObjectMapper mapper, Object o) {
        try {
            return mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
