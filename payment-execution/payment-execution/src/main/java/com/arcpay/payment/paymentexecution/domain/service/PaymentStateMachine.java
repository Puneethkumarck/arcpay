package com.arcpay.payment.paymentexecution.domain.service;

import com.arcpay.payment.paymentexecution.domain.event.PaymentStatusChanged;
import com.arcpay.payment.paymentexecution.domain.exception.IllegalPaymentTransitionException;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.PaymentTransition;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.COMPLETED;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.EXECUTING;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.FAILED;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.HELD;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.PENDING;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.POLICY_CHECK;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.REJECTED;
import static com.arcpay.payment.paymentexecution.domain.model.PaymentStatus.SCREENING;

@Component
public class PaymentStateMachine {

    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, Set.of(POLICY_CHECK, REJECTED),
            POLICY_CHECK, Set.of(SCREENING, REJECTED, FAILED),
            SCREENING, Set.of(EXECUTING, HELD, REJECTED),
            HELD, Set.of(EXECUTING, REJECTED),
            EXECUTING, Set.of(COMPLETED, FAILED));

    public PaymentTransition transition(Payment payment, PaymentStatus toStatus, Instant transitionedAt) {
        verifyTransitionAllowed(payment, toStatus);
        return toTransition(payment, payment.withStatus(toStatus, transitionedAt));
    }

    public PaymentTransition reject(Payment payment, RejectionReason reason, Instant transitionedAt) {
        verifyTransitionAllowed(payment, REJECTED);
        return toTransition(payment, payment.reject(reason, transitionedAt));
    }

    public PaymentTransition fail(Payment payment, FailureReason reason, Instant transitionedAt) {
        verifyTransitionAllowed(payment, FAILED);
        return toTransition(payment, payment.fail(reason, transitionedAt));
    }

    public PaymentTransition complete(Payment payment, Instant transitionedAt) {
        verifyTransitionAllowed(payment, COMPLETED);
        return toTransition(payment, payment.complete(transitionedAt));
    }

    private void verifyTransitionAllowed(Payment payment, PaymentStatus toStatus) {
        var from = payment.status();
        if (!ALLOWED_TRANSITIONS.getOrDefault(from, Set.of()).contains(toStatus)) {
            throw new IllegalPaymentTransitionException(payment.paymentId(), from, toStatus);
        }
    }

    private PaymentTransition toTransition(Payment from, Payment to) {
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
