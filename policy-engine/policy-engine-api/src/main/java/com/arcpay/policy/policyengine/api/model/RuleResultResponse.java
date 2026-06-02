package com.arcpay.policy.policyengine.api.model;

import java.math.BigDecimal;
import lombok.Builder;

@Builder
public record RuleResultResponse(
        String ruleType, String verdict, BigDecimal limit, BigDecimal current, BigDecimal requested, String message) {}
