package com.arcpay.payment.paymentexecution.domain.model;

import lombok.Builder;

@Builder(toBuilder = true)
public record ChainResultSignal(
        boolean confirmed,
        String onChainRef,
        Long blockNumber
) {
}
