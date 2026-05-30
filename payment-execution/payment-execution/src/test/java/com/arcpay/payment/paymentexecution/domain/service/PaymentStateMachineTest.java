package com.arcpay.payment.paymentexecution.domain.service;

import com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged;
import com.arcpay.payment.paymentexecution.domain.exception.IllegalPaymentTransitionException;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.PaymentTransition;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.COMPLETED;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.EXECUTING;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.HELD;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.PENDING;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.POLICY_CHECK;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.REJECTED;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.SCREENING;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TRANSITIONED_AT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class PaymentStateMachineTest {

    private final PaymentStateMachine stateMachine = new PaymentStateMachine();

    @Test
    void shouldTransitionPendingToPolicyCheck() {
        // given
        var payment = somePayment(PENDING);

        // when
        var result = stateMachine.transition(payment, POLICY_CHECK, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.withStatus(POLICY_CHECK, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTransitionPolicyCheckToScreening() {
        // given
        var payment = somePayment(POLICY_CHECK);

        // when
        var result = stateMachine.transition(payment, SCREENING, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.withStatus(SCREENING, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTransitionScreeningToHeld() {
        // given
        var payment = somePayment(SCREENING);

        // when
        var result = stateMachine.transition(payment, HELD, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.withStatus(HELD, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTransitionScreeningToExecuting() {
        // given
        var payment = somePayment(SCREENING);

        // when
        var result = stateMachine.transition(payment, EXECUTING, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.withStatus(EXECUTING, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldTransitionHeldToExecuting() {
        // given
        var payment = somePayment(HELD);

        // when
        var result = stateMachine.transition(payment, EXECUTING, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.withStatus(EXECUTING, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectPendingWithAgentNotActive() {
        // given
        var payment = somePayment(PENDING);

        // when
        var result = stateMachine.reject(payment, RejectionReason.AGENT_NOT_ACTIVE, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.reject(RejectionReason.AGENT_NOT_ACTIVE, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectPolicyCheckWithPolicyViolation() {
        // given
        var payment = somePayment(POLICY_CHECK);

        // when
        var result = stateMachine.reject(payment, RejectionReason.POLICY_VIOLATION, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.reject(RejectionReason.POLICY_VIOLATION, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectScreeningWithComplianceBlock() {
        // given
        var payment = somePayment(SCREENING);

        // when
        var result = stateMachine.reject(payment, RejectionReason.COMPLIANCE_BLOCK, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.reject(RejectionReason.COMPLIANCE_BLOCK, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectHeldWithReviewDenied() {
        // given
        var payment = somePayment(HELD);

        // when
        var result = stateMachine.reject(payment, RejectionReason.REVIEW_DENIED, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.reject(RejectionReason.REVIEW_DENIED, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldFailPolicyCheckWithPolicyUnavailable() {
        // given
        var payment = somePayment(POLICY_CHECK);

        // when
        var result = stateMachine.fail(payment, FailureReason.POLICY_UNAVAILABLE, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.fail(FailureReason.POLICY_UNAVAILABLE, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldFailExecutingWithExecutionReverted() {
        // given
        var payment = somePayment(EXECUTING);

        // when
        var result = stateMachine.fail(payment, FailureReason.EXECUTION_REVERTED, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.fail(FailureReason.EXECUTION_REVERTED, SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldCompleteExecuting() {
        // given
        var payment = somePayment(EXECUTING);

        // when
        var result = stateMachine.complete(payment, SOME_TRANSITIONED_AT);

        // then
        var expected = expectedTransition(payment, payment.complete(SOME_TRANSITIONED_AT));
        assertThat(result).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldRejectIllegalTransitionFromPendingToExecuting() {
        // given
        var payment = somePayment(PENDING);

        // when then
        assertThatThrownBy(() -> stateMachine.transition(payment, EXECUTING, SOME_TRANSITIONED_AT))
                .isInstanceOf(IllegalPaymentTransitionException.class)
                .hasMessage("Payment " + payment.paymentId() + " cannot transition from PENDING to EXECUTING");
    }

    @Test
    void shouldRejectIllegalCompletionFromScreening() {
        // given
        var payment = somePayment(SCREENING);

        // when then
        assertThatThrownBy(() -> stateMachine.complete(payment, SOME_TRANSITIONED_AT))
                .isInstanceOf(IllegalPaymentTransitionException.class)
                .hasMessage("Payment " + payment.paymentId() + " cannot transition from SCREENING to COMPLETED");
    }

    @Test
    void shouldRejectIllegalTransitionFromTerminalCompleted() {
        // given
        var payment = somePayment(COMPLETED);

        // when then
        assertThatThrownBy(() -> stateMachine.transition(payment, POLICY_CHECK, SOME_TRANSITIONED_AT))
                .isInstanceOf(IllegalPaymentTransitionException.class)
                .hasMessage("Payment " + payment.paymentId() + " cannot transition from COMPLETED to POLICY_CHECK");
    }

    @Test
    void shouldRejectIllegalFailureFromScreening() {
        // given
        var payment = somePayment(SCREENING);

        // when then
        assertThatThrownBy(() -> stateMachine.fail(payment, FailureReason.CHAIN_TIMEOUT, SOME_TRANSITIONED_AT))
                .isInstanceOf(IllegalPaymentTransitionException.class)
                .hasMessage("Payment " + payment.paymentId() + " cannot transition from SCREENING to FAILED");
    }

    private PaymentTransition expectedTransition(Payment from, Payment to) {
        var event = PaymentStatusChanged.builder()
                .paymentId(to.paymentId())
                .agentId(to.agentId())
                .status(to.status().name())
                .previousStatus(from.status().name())
                .transactionHash(to.txHash())
                .changedAt(to.updatedAt())
                .build();
        return PaymentTransition.builder()
                .payment(to)
                .event(event)
                .build();
    }
}
