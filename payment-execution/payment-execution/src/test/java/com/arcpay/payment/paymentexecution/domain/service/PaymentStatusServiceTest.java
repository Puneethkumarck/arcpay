package com.arcpay.payment.paymentexecution.domain.service;

import com.arcpay.payment.paymentexecution.domain.exception.PaymentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_ON_CHAIN_REF;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_PAYMENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TRANSITIONED_AT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TX_HASH;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class PaymentStatusServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private EventPublisher eventPublisher;

    private final PaymentStateMachine paymentStateMachine = new PaymentStateMachine();

    private PaymentStatusService service() {
        return new PaymentStatusService(paymentRepository, paymentStateMachine, eventPublisher);
    }

    @Test
    void shouldMoveToNextStatusAndPublishEvent() {
        // given
        var current = somePayment(PaymentStatus.PENDING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().moveTo(SOME_PAYMENT_ID, PaymentStatus.POLICY_CHECK, SOME_TRANSITIONED_AT);

        // then
        var expected = current.withStatus(PaymentStatus.POLICY_CHECK, SOME_TRANSITIONED_AT);
        then(paymentRepository).should().save(eqIgnoringTimestamps(expected));
        then(eventPublisher).should().publish(any());
    }

    @Test
    void shouldBeIdempotentWhenAlreadyInTargetStatus() {
        // given
        var current = somePayment(PaymentStatus.SCREENING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().moveTo(SOME_PAYMENT_ID, PaymentStatus.SCREENING, SOME_TRANSITIONED_AT);

        // then
        then(paymentRepository).should(never()).save(any(Payment.class));
        then(eventPublisher).should(never()).publish(any());
    }

    @Test
    void shouldRejectPaymentAndPublishEvent() {
        // given
        var current = somePayment(PaymentStatus.SCREENING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().reject(SOME_PAYMENT_ID, RejectionReason.COMPLIANCE_BLOCK, SOME_TRANSITIONED_AT);

        // then
        var expected = current.reject(RejectionReason.COMPLIANCE_BLOCK, SOME_TRANSITIONED_AT);
        then(paymentRepository).should().save(eqIgnoringTimestamps(expected));
    }

    @Test
    void shouldFailPaymentAndPublishEvent() {
        // given
        var current = somePayment(PaymentStatus.EXECUTING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().fail(SOME_PAYMENT_ID, FailureReason.CHAIN_TIMEOUT, SOME_TRANSITIONED_AT);

        // then
        var expected = current.fail(FailureReason.CHAIN_TIMEOUT, SOME_TRANSITIONED_AT);
        then(paymentRepository).should().save(eqIgnoringTimestamps(expected));
    }

    @Test
    void shouldCompletePaymentAndPublishEvent() {
        // given
        var current = somePayment(PaymentStatus.EXECUTING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().complete(SOME_PAYMENT_ID, SOME_TRANSITIONED_AT);

        // then
        var expected = current.complete(SOME_TRANSITIONED_AT);
        then(paymentRepository).should().save(eqIgnoringTimestamps(expected));
    }

    @Test
    void shouldRecordTransferTxHash() {
        // given
        var current = somePayment(PaymentStatus.EXECUTING);
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().recordTransfer(SOME_PAYMENT_ID, SOME_TX_HASH);

        // then
        var expected = current.toBuilder().txHash(SOME_TX_HASH).build();
        then(paymentRepository).should().save(eqIgnoringTimestamps(expected));
        then(eventPublisher).should(never()).publish(any());
    }

    @Test
    void shouldRecordOnChainRef() {
        // given
        var current = somePayment(PaymentStatus.EXECUTING).toBuilder().txHash(SOME_TX_HASH).build();
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.of(current));

        // when
        service().recordOnChainRef(SOME_PAYMENT_ID, SOME_ON_CHAIN_REF);

        // then
        var expected = current.toBuilder().onChainRef(SOME_ON_CHAIN_REF).build();
        then(paymentRepository).should().save(eqIgnoringTimestamps(expected));
    }

    @Test
    void shouldThrowWhenPaymentNotFound() {
        // given
        given(paymentRepository.findById(SOME_PAYMENT_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> service().complete(SOME_PAYMENT_ID, SOME_TRANSITIONED_AT))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
