package com.arcpay.settlement.infrastructure.circle;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static org.assertj.core.api.Assertions.assertThat;

class CircleTransferIdempotencyKeyTest {

    @Test
    void shouldDeriveDeterministicV5KeyFromPaymentId() {
        // when
        var first = CircleTransferAdapter.idempotencyKey(SOME_PAYMENT_ID);
        var second = CircleTransferAdapter.idempotencyKey(SOME_PAYMENT_ID);

        // then
        assertThat(first).isEqualTo(second);
        assertThat(first.version()).isEqualTo(5);
    }

    @Test
    void shouldNotReuseRawPaymentIdAsKey() {
        // when
        var key = CircleTransferAdapter.idempotencyKey(SOME_PAYMENT_ID);

        // then
        assertThat(key).isNotEqualTo(SOME_PAYMENT_ID);
    }

    @Test
    void shouldDeriveDistinctKeysForDistinctPaymentIds() {
        // given
        var otherPaymentId = UUID.randomUUID();

        // when
        var key = CircleTransferAdapter.idempotencyKey(SOME_PAYMENT_ID);
        var otherKey = CircleTransferAdapter.idempotencyKey(otherPaymentId);

        // then
        assertThat(key).isNotEqualTo(otherKey);
    }
}
