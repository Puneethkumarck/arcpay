package com.arcpay.payment.paymentexecution.domain.port;

import com.arcpay.payment.paymentexecution.api.model.PaymentReceipt;

import java.math.BigDecimal;
import java.util.UUID;

public interface SettlementPort {

    String transfer(UUID paymentId, UUID agentId, String recipientAddress, BigDecimal amount);

    BigDecimal balance(UUID agentId);

    PaymentReceipt writeReceipt(UUID paymentId);
}
