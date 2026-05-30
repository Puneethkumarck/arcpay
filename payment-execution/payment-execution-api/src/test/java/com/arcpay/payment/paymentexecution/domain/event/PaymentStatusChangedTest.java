package com.arcpay.payment.paymentexecution.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentStatusChangedTest {

    @Test
    void shouldExposePaymentStatusChangedTopic() {
        // when / then
        assertThat(PaymentStatusChanged.TOPIC).isEqualTo("payment.status-changed");
    }

    @Test
    void shouldRebuildEquivalentInstanceViaToBuilder() {
        // given
        var original = PaymentStatusChanged.builder()
                .paymentId(UUID.randomUUID())
                .agentId(UUID.randomUUID())
                .status("COMPLETED")
                .previousStatus("PENDING")
                .transactionHash("0xdeadbeef")
                .changedAt(Instant.parse("2026-05-29T10:00:01Z"))
                .build();

        // when
        var rebuilt = original.toBuilder().build();

        // then
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void shouldRejectNullRequiredField() {
        // given
        var builder = PaymentStatusChanged.builder()
                .agentId(UUID.randomUUID())
                .status("COMPLETED")
                .changedAt(Instant.parse("2026-05-29T10:00:01Z"));

        // when / then
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }
}
