package com.arcpay.payment.paymentexecution.application.service;

import com.arcpay.payment.paymentexecution.api.model.CreatePaymentRequest;
import com.arcpay.payment.paymentexecution.domain.agent.AgentAuthorization;
import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.exception.InvalidPaymentRequestException;
import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentRequest;
import com.arcpay.payment.paymentexecution.domain.port.EventPublisher;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import com.arcpay.payment.paymentexecution.domain.service.PaymentOrchestrationService;
import com.arcpay.platform.api.OwnerPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCreationService {

    private final AgentAuthorization agentAuthorization;
    private final PaymentOrchestrationService paymentOrchestrationService;
    private final PaymentRepository paymentRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public PaymentCreationResult create(OwnerPrincipal principal, CreatePaymentRequest request) {
        var ownerId = principal.ownerId();
        var agent = agentAuthorization.verifyOwnershipAndActive(request.agentId(), ownerId);
        rejectSelfPayment(request, agent);

        var pending = paymentOrchestrationService.newPayment(toDomainRequest(request, ownerId));
        var saved = paymentRepository.save(pending);
        var created = saved.paymentId().equals(pending.paymentId());

        if (created) {
            eventPublisher.publish(toEvent(saved, agent));
            log.info("Payment created paymentId={} agentId={} ownerId={}",
                    saved.paymentId(), saved.agentId(), ownerId);
        } else {
            log.info("Idempotent payment replay paymentId={} agentId={} idempotencyKey={}",
                    saved.paymentId(), saved.agentId(), saved.idempotencyKey());
        }

        return new PaymentCreationResult(saved, created);
    }

    private void rejectSelfPayment(CreatePaymentRequest request, AgentInfo agent) {
        if (agent.walletAddress() != null
                && agent.walletAddress().toLowerCase(Locale.ROOT)
                        .equals(request.recipientAddress().toLowerCase(Locale.ROOT))) {
            throw new InvalidPaymentRequestException(
                    "Recipient must not be the agent's own wallet address: " + request.recipientAddress());
        }
    }

    private PaymentRequest toDomainRequest(CreatePaymentRequest request, UUID ownerId) {
        return PaymentRequest.builder()
                .agentId(request.agentId())
                .ownerId(ownerId)
                .idempotencyKey(request.idempotencyKey())
                .recipientAddress(request.recipientAddress())
                .amount(request.amount())
                .currency(request.currency())
                .memo(request.memo())
                .metadata(request.metadata())
                .build();
    }

    private PaymentRequested toEvent(Payment payment, AgentInfo agent) {
        return PaymentRequested.builder()
                .paymentId(payment.paymentId())
                .agentId(payment.agentId())
                .ownerId(payment.ownerId())
                .walletId(agent.walletId())
                .idempotencyKey(payment.idempotencyKey())
                .recipientAddress(payment.recipientAddress())
                .amount(payment.amount())
                .currency(payment.currency())
                .memo(payment.memo())
                .metadata(payment.metadata())
                .requestedAt(payment.createdAt())
                .build();
    }
}
