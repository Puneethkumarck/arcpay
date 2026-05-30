package com.arcpay.payment.paymentexecution.domain.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentRequestedTest {

    @Test
    void shouldExposePaymentRequestedTopic() {
        // when / then
        assertThat(PaymentRequested.TOPIC).isEqualTo("payment.requested");
    }

    @Test
    void shouldRebuildEquivalentInstanceViaToBuilder() {
        // given
        var original = PaymentRequested.builder()
                .paymentId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .ownerId(UUID.randomUUID())
                .idempotencyKey("idem-1")
                .recipientAddress("0xabc")
                .amount(new BigDecimal("25.00"))
                .currency("USDC")
                .requestedAt(Instant.parse("2026-05-29T10:00:00Z"))
                .build();

        // when
        var rebuilt = original.toBuilder().build();

        // then
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(original);
    }
}
