package com.arcpay.payment.paymentexecution.fixtures;

import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentRequest;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class PaymentFixtures {

    private PaymentFixtures() {}

    public static final UUID SOME_PAYMENT_ID = UUID.fromString("0197aa00-1111-7def-8000-111111111111");
    public static final UUID SOME_AGENT_ID = UUID.fromString("0197aa00-2222-7def-8000-222222222222");
    public static final UUID SOME_OWNER_ID = UUID.fromString("0197aa00-3333-7def-8000-333333333333");
    public static final String SOME_IDEMPOTENCY_KEY = "invoice-2026-0042";
    public static final String SOME_RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    public static final BigDecimal SOME_AMOUNT = new BigDecimal("25.00");
    public static final String SOME_CURRENCY = "USDC";
    public static final String SOME_MEMO = "GPT-4 API credits";
    public static final String SOME_FINGERPRINT = "0xfingerprint";
    public static final Map<String, String> SOME_METADATA = Map.of("category", "compute", "team", "research");
    public static final Instant SOME_CREATED_AT = Instant.parse("2026-05-29T10:00:00Z");
    public static final Instant SOME_TRANSITIONED_AT = Instant.parse("2026-05-29T10:05:00Z");

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
}
