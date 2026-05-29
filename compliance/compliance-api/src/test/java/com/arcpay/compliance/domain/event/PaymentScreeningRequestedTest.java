package com.arcpay.compliance.domain.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentScreeningRequestedTest {

    @Test
    void shouldExposeScreeningRequestedTopic() {
        // when / then
        assertThat(PaymentScreeningRequested.TOPIC).isEqualTo("screening.requested");
    }

    @Test
    void shouldRebuildEquivalentInstanceViaToBuilder() {
        // given
        var original = PaymentScreeningRequested.builder()
                .paymentId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .recipientAddress("0xabc")
                .amount(new BigDecimal("100.50"))
                .currency("USDC")
                .requestedAt(Instant.parse("2026-05-29T10:00:00Z"))
                .build();

        // when
        var rebuilt = original.toBuilder().build();

        // then
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(original);
    }
}
