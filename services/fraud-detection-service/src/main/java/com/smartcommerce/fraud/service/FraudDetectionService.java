package com.smartcommerce.fraud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcommerce.fraud.api.dto.FraudCheckRequest;
import com.smartcommerce.fraud.api.dto.FraudCheckResult;
import com.smartcommerce.fraud.client.UserClient;
import com.smartcommerce.fraud.domain.FraudCheck;
import com.smartcommerce.fraud.repository.FraudCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private static final int DECLINE_THRESHOLD = 70;
    private static final int FLAG_THRESHOLD = 40;

    private final List<FraudRule> rules;
    private final FraudCheckRepository fraudCheckRepository;
    private final UserClient userClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public FraudCheckResult check(FraudCheckRequest request) {
        var existing = fraudCheckRepository.findByOrderId(request.orderId());
        if (existing.isPresent()) {
            log.info("Fraud check already exists for order {}, returning cached decision", request.orderId());
            return toResult(existing.get());
        }

        var user = safeGetUser(request.userId());
        var ctx = new FraudRule.FraudCheckContext(
            request.orderId(),
            request.userId(),
            user != null ? user.email() : null,
            request.userIp(),
            request.amount(),
            request.currency() != null ? request.currency() : "TRY",
            request.itemCount(),
            0, BigDecimal.ZERO, 0,
            request.firstOrder(),
            user != null ? user.createdAt() : null
        );

        var triggered = rules.stream()
            .map(rule -> Map.entry(rule, rule.evaluate(ctx)))
            .filter(e -> e.getValue().triggered())
            .toList();

        var totalScore = Math.min(100, triggered.stream()
            .mapToInt(e -> e.getValue().riskScore()).sum());

        var decision = totalScore >= DECLINE_THRESHOLD ? "DECLINED"
            : totalScore >= FLAG_THRESHOLD ? "FLAGGED"
            : "APPROVED";

        var triggeredRules = triggered.stream()
            .map(e -> Map.<String, Object>of(
                "rule", e.getKey().getName(),
                "score", e.getValue().riskScore(),
                "reason", e.getValue().reason() != null ? e.getValue().reason() : ""))
            .toList();

        var check = FraudCheck.builder()
            .orderId(request.orderId())
            .userId(request.userId())
            .amount(request.amount())
            .currency(ctx.currency())
            .riskScore(totalScore)
            .decision(decision)
            .triggeredRules(objectMapper.valueToTree(triggeredRules))
            .userVelocityCount(ctx.userOrdersLast24h())
            .userAmount24h(ctx.userAmountLast24h())
            .decidedAt(Instant.now())
            .build();
        fraudCheckRepository.save(check);

        log.info("Fraud check: orderId={} score={} decision={} rules={}",
            request.orderId(), totalScore, decision, triggeredRules);

        return new FraudCheckResult(
            !"APPROVED".equals(decision),
            decision,
            totalScore,
            triggered.isEmpty() ? null : triggered.get(0).getValue().reason(),
            triggered.stream().map(e -> e.getKey().getName()).toList()
        );
    }

    private FraudCheckResult toResult(FraudCheck check) {
        return new FraudCheckResult(
            !"APPROVED".equals(check.getDecision()),
            check.getDecision(),
            check.getRiskScore(),
            null,
            List.of()
        );
    }

    private UserClient.UserProfile safeGetUser(java.util.UUID userId) {
        try {
            return userClient.getProfile(userId);
        } catch (Exception e) {
            log.warn("Could not fetch user profile for fraud check (userId={}): {}", userId, e.getMessage());
            return null;
        }
    }
}
