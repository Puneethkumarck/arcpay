package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.event.TransferConfirmed;
import com.arcpay.settlement.domain.event.TransferReverted;
import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.TransferState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.settlement.domain.model.TransferState.CANCELLED;
import static com.arcpay.settlement.domain.model.TransferState.COMPLETED;
import static com.arcpay.settlement.domain.model.TransferState.DENIED;
import static com.arcpay.settlement.domain.model.TransferState.FAILED;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_NETWORK_FEE;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_TX_HASH;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someFailedTransaction;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someSettlementTransaction;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SettlementEventFactoryTest {

    private final SettlementEventFactory factory = new SettlementEventFactory();

    @Test
    void shouldEmitTransferConfirmedWhenCompleted() {
        // given
        var transaction = someSettlementTransaction(COMPLETED);
        var expected = TransferConfirmed.builder()
                .paymentId(SOME_PAYMENT_ID)
                .txHash(SOME_TX_HASH)
                .networkFee(SOME_NETWORK_FEE)
                .confirmedAt(transaction.createdAt())
                .build();

        // when
        var event = factory.eventFor(transaction);

        // then
        assertThat(event).get()
                .usingRecursiveComparison()
                .ignoringFields("confirmedAt")
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(value = TransferState.class, names = {"FAILED", "DENIED", "CANCELLED"})
    void shouldEmitTransferRevertedWhenFailedDeniedOrCancelled(TransferState state) {
        // given
        var transaction = someFailedTransaction(state);
        var expected = TransferReverted.builder()
                .paymentId(SOME_PAYMENT_ID)
                .reason(transaction.errorReason())
                .revertedAt(transaction.createdAt())
                .build();

        // when
        var event = factory.eventFor(transaction);

        // then
        assertThat(event).get()
                .usingRecursiveComparison()
                .ignoringFields("revertedAt")
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(value = TransferState.class, names = {"FAILED", "DENIED", "CANCELLED"})
    void shouldFallBackToStateNameAsReasonWhenErrorReasonMissing(TransferState state) {
        // given
        var transaction = someSettlementTransaction(state);
        var expected = TransferReverted.builder()
                .paymentId(SOME_PAYMENT_ID)
                .reason(state.name())
                .revertedAt(transaction.createdAt())
                .build();

        // when
        var event = factory.eventFor(transaction);

        // then
        assertThat(event).get()
                .usingRecursiveComparison()
                .ignoringFields("revertedAt")
                .isEqualTo(expected);
    }

    @ParameterizedTest
    @EnumSource(value = TransferState.class, names = {"INITIATED", "QUEUED", "SENT", "CONFIRMED", "STUCK"})
    void shouldEmitNoEventForNonTerminalStates(TransferState state) {
        // given
        var transaction = someSettlementTransaction(state);

        // when
        var event = factory.eventFor(transaction);

        // then
        assertThat(event).isEmpty();
    }

    @Test
    void shouldTreatConfirmedAsNonTerminalWithNoEvent() {
        // given
        var transaction = someSettlementTransaction(TransferState.CONFIRMED);

        // when
        var event = factory.eventFor(transaction);

        // then
        assertThat(event).isEmpty();
    }

    @Test
    void shouldMapEveryStateExactlyOnce() {
        // given
        var confirming = someSettlementTransaction(COMPLETED);
        var reverting = someFailedTransaction(FAILED);
        var denied = someFailedTransaction(DENIED);
        var cancelled = someFailedTransaction(CANCELLED);

        // when / then
        assertThat(factory.eventFor(confirming)).get().isInstanceOf(TransferConfirmed.class);
        assertThat(factory.eventFor(reverting)).get().isInstanceOf(TransferReverted.class);
        assertThat(factory.eventFor(denied)).get().isInstanceOf(TransferReverted.class);
        assertThat(factory.eventFor(cancelled)).get().isInstanceOf(TransferReverted.class);
    }
}
