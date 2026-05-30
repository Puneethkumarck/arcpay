package com.arcpay.settlement.domain;

import com.arcpay.settlement.domain.event.TransferConfirmed;
import com.arcpay.settlement.domain.event.TransferReverted;
import com.arcpay.settlement.domain.model.SettlementTransaction;
import com.arcpay.settlement.domain.model.TransferNotification;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.EventPublisher;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.settlement.domain.model.TransferState.CANCELLED;
import static com.arcpay.settlement.domain.model.TransferState.COMPLETED;
import static com.arcpay.settlement.domain.model.TransferState.CONFIRMED;
import static com.arcpay.settlement.domain.model.TransferState.DENIED;
import static com.arcpay.settlement.domain.model.TransferState.FAILED;
import static com.arcpay.settlement.domain.model.TransferState.SENT;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_ERROR_REASON;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someSettlementTransaction;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class TransferNotificationHandlerTest {

    @Mock
    private SettlementTransactionRepository repository;

    @Mock
    private EventPublisher eventPublisher;

    private final SettlementEventFactory eventFactory = new SettlementEventFactory();

    private TransferNotificationHandler handler() {
        return new TransferNotificationHandler(repository, eventFactory, eventPublisher);
    }

    @Test
    void shouldPublishTransferConfirmedWhenCompleted() {
        // given
        given(repository.findByCircleTxId(SOME_CIRCLE_TX_ID))
                .willReturn(Optional.of(someSettlementTransaction(SENT)));
        given(repository.update(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        handler().handle(notification(COMPLETED, null));

        // then
        var captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should().publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TransferConfirmed.class);
        assertThat(((TransferConfirmed) captor.getValue()).paymentId()).isEqualTo(SOME_PAYMENT_ID);
    }

    @ParameterizedTest
    @EnumSource(value = TransferState.class, names = {"FAILED", "DENIED", "CANCELLED"})
    void shouldPublishTransferRevertedWhenFailedDeniedOrCancelled(TransferState state) {
        // given
        given(repository.findByCircleTxId(SOME_CIRCLE_TX_ID))
                .willReturn(Optional.of(someSettlementTransaction(SENT)));
        given(repository.update(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        handler().handle(notification(state, SOME_ERROR_REASON));

        // then
        var captor = ArgumentCaptor.forClass(Object.class);
        then(eventPublisher).should().publish(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(TransferReverted.class);
        assertThat(((TransferReverted) captor.getValue()).reason()).isEqualTo(SOME_ERROR_REASON);
    }

    @Test
    void shouldNotPublishWhenConfirmed() {
        // given
        given(repository.findByCircleTxId(SOME_CIRCLE_TX_ID))
                .willReturn(Optional.of(someSettlementTransaction(SENT)));
        given(repository.update(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        handler().handle(notification(CONFIRMED, null));

        // then
        then(repository).should().update(any());
        then(eventPublisher).should(never()).publish(any());
    }

    @ParameterizedTest
    @EnumSource(value = TransferState.class, names = {"COMPLETED", "FAILED", "DENIED", "CANCELLED"})
    void shouldBeNoOpWhenAlreadyTerminal(TransferState terminal) {
        // given
        given(repository.findByCircleTxId(SOME_CIRCLE_TX_ID))
                .willReturn(Optional.of(someSettlementTransaction(terminal)));

        // when
        handler().handle(notification(COMPLETED, null));

        // then
        then(repository).should(never()).update(any());
        then(eventPublisher).should(never()).publish(any());
    }

    @Test
    void shouldThrowWhenCircleTxIdUnknown() {
        // given
        given(repository.findByCircleTxId(SOME_CIRCLE_TX_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> handler().handle(notification(COMPLETED, null)))
                .isInstanceOf(TransferNotFoundException.class);
        then(eventPublisher).should(never()).publish(any());
    }

    private TransferNotification notification(TransferState state, String errorReason) {
        return TransferNotification.builder()
                .circleTxId(SOME_CIRCLE_TX_ID)
                .state(state)
                .errorReason(errorReason)
                .build();
    }
}
