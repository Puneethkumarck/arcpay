package com.arcpay.payment.paymentexecution.infrastructure.messaging;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.port.CompliancePort;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
class CompliancePortAdapter implements CompliancePort {

    private final EventPublisher eventPublisher;

    @Override
    public void publishScreeningRequest(Payment payment) {
        var event = toScreeningRequested(payment);
        eventPublisher.publish(event);
        log.info("Scheduled screening request paymentId={} agentId={}", payment.paymentId(), payment.agentId());
    }

    private PaymentScreeningRequested toScreeningRequested(Payment payment) {
        return PaymentScreeningRequested.builder()
                .paymentId(payment.paymentId())
                .agentId(payment.agentId())
                .recipientAddress(payment.recipientAddress())
                .amount(payment.amount())
                .currency(payment.currency())
                .requestedAt(Instant.now())
                .build();
    }
}
