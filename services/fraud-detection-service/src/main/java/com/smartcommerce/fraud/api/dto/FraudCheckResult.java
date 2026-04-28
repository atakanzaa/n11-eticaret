package com.smartcommerce.fraud.api.dto;

import java.util.List;

public record FraudCheckResult(
    boolean flagged,
    String decision,
    int riskScore,
    String reason,
    List<String> triggeredRules
) {
}
