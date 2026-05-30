package com.arcpay.settlement.domain.event;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransferRevertedTest {

    @Test
    void shouldExposeTransferRevertedTopic() {
        // when / then
        assertThat(TransferReverted.TOPIC).isEqualTo("transfer.reverted");
    }

    @Test
    void shouldRebuildEquivalentInstanceViaToBuilder() {
        // given
        var original = TransferReverted.builder()
                .paymentId(UUID.randomUUID())
                .reason("DENIED")
                .revertedAt(Instant.parse("2026-05-30T10:00:01Z"))
                .build();

        // when
        var rebuilt = original.toBuilder().build();

        // then
        assertThat(rebuilt).usingRecursiveComparison().isEqualTo(original);
    }

    @Test
    void shouldRejectNullRequiredField() {
        // given
        var builder = TransferReverted.builder()
                .reason("DENIED")
                .revertedAt(Instant.parse("2026-05-30T10:00:01Z"));

        // when / then
        assertThatThrownBy(builder::build).isInstanceOf(NullPointerException.class);
    }
}
