package com.arcpay.payment.paymentexecution.domain.service;

import com.arcpay.payment.paymentexecution.domain.exception.MissingTransferHashException;
import com.arcpay.payment.paymentexecution.domain.exception.PaymentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.PaymentTransition;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final PaymentRepository paymentRepository;
    private final PaymentStateMachine paymentStateMachine;
    private final EventPublisher eventPublisher;

    @Transactional
    public void moveTo(UUID paymentId, PaymentStatus toStatus, Instant transitionedAt) {
        var payment = load(paymentId);
        if (payment.status() == toStatus) {
            return;
        }
        apply(paymentStateMachine.transition(payment, toStatus, transitionedAt));
    }

    @Transactional
    public void reject(UUID paymentId, RejectionReason reason, Instant transitionedAt) {
        var payment = load(paymentId);
        if (payment.status() == PaymentStatus.REJECTED) {
            return;
        }
        apply(paymentStateMachine.reject(payment, reason, transitionedAt));
    }

    @Transactional
    public void fail(UUID paymentId, FailureReason reason, Instant transitionedAt) {
        var payment = load(paymentId);
        if (payment.status() == PaymentStatus.FAILED) {
            return;
        }
        apply(paymentStateMachine.fail(payment, reason, transitionedAt));
    }

    @Transactional
    public void complete(UUID paymentId, Instant transitionedAt) {
        var payment = load(paymentId);
        if (payment.status() == PaymentStatus.COMPLETED) {
            return;
        }
        apply(paymentStateMachine.complete(payment, transitionedAt));
    }

    @Transactional
    public void recordTransfer(UUID paymentId, String txHash) {
        if (txHash == null) {
            throw new MissingTransferHashException(paymentId);
        }
        var payment = load(paymentId);
        if (txHash.equals(payment.txHash())) {
            return;
        }
        paymentRepository.save(payment.toBuilder().txHash(txHash).build());
    }

    @Transactional
    public void recordOnChainRef(UUID paymentId, String onChainRef) {
        var payment = load(paymentId);
        if (onChainRef == null || payment.onChainRef() != null) {
            return;
        }
        paymentRepository.save(payment.toBuilder().onChainRef(onChainRef).build());
    }

    private void apply(PaymentTransition transition) {
        var payment = transition.payment();
        paymentRepository.save(payment);
        eventPublisher.publish(transition.event());
        log.info("Payment paymentId={} transitioned to status={}", payment.paymentId(), payment.status());
    }

    private Payment load(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException(paymentId));
    }
}
