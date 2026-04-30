package com.smartcommerce.ai.api;

import com.smartcommerce.ai.api.dto.AiUsageBreakdownResponse;
import com.smartcommerce.ai.api.dto.ChatRequest;
import com.smartcommerce.ai.api.dto.ChatResponse;
import com.smartcommerce.ai.api.dto.EnrichmentResult;
import com.smartcommerce.ai.domain.AiConversation;
import com.smartcommerce.ai.domain.AiMessage;
import com.smartcommerce.ai.service.AiBudgetGuard;
import com.smartcommerce.ai.service.AiUsageQueryService;
import com.smartcommerce.ai.service.ProductEnrichmentService;
import com.smartcommerce.ai.service.ShoppingAssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ShoppingAssistantService assistantService;
    private final ProductEnrichmentService enrichmentService;
    private final AiBudgetGuard budgetGuard;
    private final AiUsageQueryService usageQueryService;

    @PostMapping("/chat")
    public ChatResponse chat(@AuthenticationPrincipal String userId,
                             @Valid @RequestBody ChatRequest request) {
        return assistantService.chat(UUID.fromString(userId), request.conversationId(), request.message());
    }

    @GetMapping("/conversations/me")
    public List<AiConversation> myConversations(@AuthenticationPrincipal String userId) {
        return assistantService.myConversations(UUID.fromString(userId));
    }

    @GetMapping("/conversations/{id}/messages")
    public List<AiMessage> conversationMessages(@PathVariable UUID id) {
        return assistantService.conversationMessages(id);
    }

    @PostMapping("/products/{id}/enrich")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SELLER')")
    public EnrichmentResult enrichProduct(@PathVariable UUID id) {
        return enrichmentService.enrich(id);
    }

    @GetMapping("/usage/today")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, BigDecimal> todayUsage() {
        return Map.of(
            "spent", budgetGuard.todaySpend(),
            "remaining", budgetGuard.remainingBudget()
        );
    }

    @GetMapping("/usage/breakdown")
    @PreAuthorize("hasRole('ADMIN')")
    public AiUsageBreakdownResponse usageBreakdown(@RequestParam(defaultValue = "7") int days) {
        return usageQueryService.breakdown(days);
    }
}
