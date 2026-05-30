package com.arcpay.payment.paymentexecution.api.model;

import lombok.Builder;

@Builder
public record ComplianceResult(
        String verdict,
        Integer riskScore
) {}
