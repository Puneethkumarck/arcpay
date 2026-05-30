package com.arcpay.payment.paymentexecution.api.model;

import lombok.Builder;

@Builder
public record PolicyResult(
        String verdict,
        Integer rulesEvaluated
) {}
