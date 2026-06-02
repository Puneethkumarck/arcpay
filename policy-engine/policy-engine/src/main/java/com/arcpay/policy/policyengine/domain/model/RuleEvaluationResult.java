package com.arcpay.policy.policyengine.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import lombok.Builder;

@Builder
public record RuleEvaluationResult(
        String ruleType,
        RuleVerdict verdict,
        BigDecimal limit,
        BigDecimal current,
        BigDecimal requested,
        String message) {

    public RuleEvaluationResult {
        Objects.requireNonNull(ruleType, "ruleType must not be null");
        Objects.requireNonNull(verdict, "verdict must not be null");
    }
}
