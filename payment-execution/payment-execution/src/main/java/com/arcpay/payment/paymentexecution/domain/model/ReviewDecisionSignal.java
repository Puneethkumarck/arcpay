package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ReviewDecisionSignal(
        boolean approved
) {
}
