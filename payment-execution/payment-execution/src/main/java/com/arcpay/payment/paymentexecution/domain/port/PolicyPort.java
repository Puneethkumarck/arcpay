package com.arcpay.payment.paymentexecution.domain.port;

import com.arcpay.payment.paymentexecution.api.model.PolicyResult;

import java.math.BigDecimal;
import java.util.UUID;

public interface PolicyPort {

    PolicyResult reserve(UUID paymentId, UUID agentId, String recipientAddress, BigDecimal amount);

    void commit(UUID paymentId);

    void release(UUID paymentId);
}
