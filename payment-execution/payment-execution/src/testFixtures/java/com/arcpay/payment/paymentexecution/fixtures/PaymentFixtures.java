package com.arcpay.payment.paymentexecution.fixtures;

import com.arcpay.payment.paymentexecution.api.model.CreatePaymentRequest;
import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;
import com.arcpay.payment.paymentexecution.domain.model.ChainResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentExecutionInput;
import com.arcpay.payment.paymentexecution.domain.model.PaymentRequest;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.ReviewDecisionSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningVerdict;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class PaymentFixtures {

    private PaymentFixtures() {}

    public static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    public static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    public static final UUID SOME_OWNER_ID = UUID.fromString("0197aa00-3333-7def-8000-333333333333");
    public static final String SOME_OWNER_EMAIL = "owner@arcpay.dev";
    public static final String SOME_IDEMPOTENCY_KEY = "invoice-2026-0042";
    public static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    public static final String SOME_WALLET_ID = "wallet-abc-123";
    public static final String SOME_WALLET_ADDRESS = "0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    public static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");
    public static final String SOME_CURRENCY = "USDC";
    public static final String SOME_MEMO = "GPT-4 API credits";
    public static final String SOME_FINGERPRINT = "0xfingerprint";
    public static final Map<String, String> SOME_METADATA = Map.of("category", "compute", "team", "research");
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-05-29T10:00:00Z");
    public static final Instant SOME_TRANSITIONED_AT = Instant.parse("2026-05-29T10:05:00Z");

    public static AgentInfo someAgentInfo(String status) {
        return AgentInfo.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .status(status)
                .walletId(SOME_WALLET_ID)
                .walletAddress(SOME_WALLET_ADDRESS)
                .build();
    }

    public static CreatePaymentRequest someCreatePaymentRequest() {
        return CreatePaymentRequest.builder()
                .agentId(SOME_AGENT_ID)
                .idempotencyKey(SOME_IDEMPOTENCY_KEY)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .currency(SOME_CURRENCY)
                .memo(SOME_MEMO)
                .metadata(SOME_METADATA)
                .build();
    }

    public static PaymentRequest someRequest() {
        return PaymentRequest.builder()
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .idempotencyKey(SOME_IDEMPOTENCY_KEY)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .currency(SOME_CURRENCY)
                .memo(SOME_MEMO)
                .metadata(SOME_METADATA)
                .build();
    }

    public static final UUID SOME_OTHER_AGENT_ID = UUID.fromString("0197aa00-4444-7def-8000-444444444444");
    public static final String SOME_OTHER_IDEMPOTENCY_KEY = "invoice-2026-0043";
    public static final String SOME_OTHER_FINGERPRINT = "0xotherfingerprint";

    public static Payment somePayment(PaymentStatus status) {
        return Payment.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .idempotencyKey(SOME_IDEMPOTENCY_KEY)
                .requestFingerprint(SOME_FINGERPRINT)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .currency(SOME_CURRENCY)
                .memo(SOME_MEMO)
                .metadata(SOME_METADATA)
                .status(status)
                .createdAt(SOME_CREATED_AT)
                .updatedAt(SOME_CREATED_AT)
                .build();
    }

    public static Payment somePaymentWith(UUID paymentId, UUID agentId, String idempotencyKey, PaymentStatus status) {
        return somePayment(status).toBuilder()
                .paymentId(paymentId)
                .agentId(agentId)
                .idempotencyKey(idempotencyKey)
                .build();
    }

    public static final String SOME_TX_HASH = "0xtransfer1234567890abcdef";
    public static final String SOME_ON_CHAIN_REF = "0xonchainref1234567890";

    public static PaymentRequested somePaymentRequested() {
        return PaymentRequested.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .ownerId(SOME_OWNER_ID)
                .walletId(SOME_WALLET_ID)
                .idempotencyKey(SOME_IDEMPOTENCY_KEY)
                .recipientAddress(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .currency(SOME_CURRENCY)
                .memo(SOME_MEMO)
                .metadata(SOME_METADATA)
                .requestedAt(SOME_CREATED_AT)
                .build();
    }

    public static PaymentExecutionInput someExecutionInput() {
        return PaymentExecutionInput.builder()
                .paymentId(SOME_PAYMENT_ID)
                .agentId(SOME_AGENT_ID)
                .walletId(SOME_WALLET_ID)
                .recipient(SOME_RECIPIENT)
                .amount(SOME_AMOUNT)
                .memo(SOME_MEMO)
                .build();
    }

    public static ScreeningResultSignal someScreeningResult(ScreeningVerdict verdict) {
        return ScreeningResultSignal.builder()
                .verdict(verdict)
                .riskScore(10)
                .build();
    }

    public static ReviewDecisionSignal someReviewDecision(boolean approved) {
        return ReviewDecisionSignal.builder()
                .approved(approved)
                .build();
    }

    public static ChainResultSignal someChainResult(boolean confirmed) {
        return ChainResultSignal.builder()
                .confirmed(confirmed)
                .onChainRef(confirmed ? SOME_ON_CHAIN_REF : null)
                .blockNumber(confirmed ? 42L : null)
                .build();
    }
}
