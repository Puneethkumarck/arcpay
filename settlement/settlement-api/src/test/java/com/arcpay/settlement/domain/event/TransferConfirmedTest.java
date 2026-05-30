package com.arcpay.settlement.domain.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferConfirmedTest {

    @Test
    void shouldExposeTransferConfirmedTopic() {
        // when / then
        assertThat(TransferConfirmed.TOPIC).isEqualTo("transfer.confirmed");
    }

    @Test
    void shouldRebuildEquivalentInstanceViaToBuilder() {
        // given
        var original = TransferConfirmed.builder()
                .paymentId(UUID.randomUUID())
                .txHash("0xdeadbeef")
                .networkFee(new BigDecimal("0.010000"))
                .confirmedAt(Instant.parse("2026-05-30T10:00:00Z"))
                .build();

        // when
        var rebuilt = original.toBuilder().build();

        // then
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void shouldRejectNullRequiredField() {
        // given
        var builder = TransferConfirmed.builder()
                .txHash("0xdeadbeef")
                .confirmedAt(Instant.parse("2026-05-30T10:00:00Z"));

        // when / then
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }
}
