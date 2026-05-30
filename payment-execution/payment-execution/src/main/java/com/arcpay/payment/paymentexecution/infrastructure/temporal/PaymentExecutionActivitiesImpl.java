package com.arcpay.payment.paymentexecution.infrastructure.temporal;

import com.arcpay.payment.paymentexecution.domain.exception.PaymentNotFoundException;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.arcpay.payment.paymentexecution.domain.port.AgentServiceClient;
import com.arcpay.payment.paymentexecution.domain.port.CompliancePort;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import com.arcpay.payment.paymentexecution.domain.port.PolicyPort;
import com.arcpay.payment.paymentexecution.domain.port.SettlementPort;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionActivities;
import com.arcpay.payment.paymentexecution.domain.service.PaymentStatusService;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ActivityImpl(taskQueues = "PaymentExecutionTaskQueue")
class PaymentExecutionActivitiesImpl implements PaymentExecutionActivities {

    private static final String ACTIVE_STATUS = "ACTIVE";

    private final AgentServiceClient agentServiceClient;
    private final PolicyPort policyPort;
    private final CompliancePort compliancePort;
    private final SettlementPort settlementPort;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusService paymentStatusService;

    @Override
    public boolean verifyAgentActive(UUID agentId) {
        return agentServiceClient.getAgent(agentId)
                .map(agent -> ACTIVE_STATUS.equals(agent.status()))
                .orElse(false);
    }

    @Override
    public String reserve(UUID paymentId, UUID agentId, String recipient, BigDecimal amount) {
        var result = policyPort.reserve(paymentId, agentId, recipient, amount);
        log.info("Policy reservation paymentId={} verdict={}", paymentId, result.verdict());
        return result.verdict();
    }

    @Override
    public void commit(UUID paymentId) {
        policyPort.commit(paymentId);
        log.info("Policy reservation committed paymentId={}", paymentId);
    }

    @Override
    public void release(UUID paymentId) {
        policyPort.release(paymentId);
        log.info("Policy reservation released paymentId={}", paymentId);
    }

    @Override
    public void publishScreeningRequested(UUID paymentId) {
        var payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApplicationFailure.newNonRetryableFailure(
                        "Payment not found: " + paymentId, PaymentNotFoundException.class.getSimpleName()));
        compliancePort.publishScreeningRequest(payment);
        log.info("Screening request published paymentId={}", paymentId);
    }

    @Override
    public String submitTransfer(UUID paymentId, UUID agentId, String recipient, BigDecimal amount) {
        var walletId = agentServiceClient.getAgent(agentId)
                .map(agent -> agent.walletId())
                .orElseThrow(() -> ApplicationFailure.newNonRetryableFailure(
                        "Agent wallet not found: " + agentId, "AgentWalletNotFound"));
        var txHash = settlementPort.transfer(paymentId, walletId, recipient, amount);
        log.info("Transfer submitted paymentId={} txHash={}", paymentId, txHash);
        return txHash;
    }

    @Override
    public void writeReceiptAsync(UUID paymentId) {
        try {
            var payment = paymentRepository.findById(paymentId)
                    .orElseThrow(() -> ApplicationFailure.newNonRetryableFailure(
                            "Payment not found: " + paymentId, PaymentNotFoundException.class.getSimpleName()));
            var receipt = settlementPort.writeReceipt(payment);
            paymentStatusService.recordOnChainRef(paymentId, receipt.onChainRef());
            log.info("Receipt written paymentId={} onChainRef={}", paymentId, receipt.onChainRef());
        } catch (RuntimeException e) {
            log.warn("Receipt write failed paymentId={} reason={}", paymentId, e.getMessage());
        }
    }

    @Override
    public void persistStatus(UUID paymentId, PaymentStatus toStatus, Instant transitionedAt) {
        paymentStatusService.moveTo(paymentId, toStatus, transitionedAt);
    }

    @Override
    public void persistRejected(UUID paymentId, RejectionReason reason, Instant transitionedAt) {
        paymentStatusService.reject(paymentId, reason, transitionedAt);
    }

    @Override
    public void persistFailed(UUID paymentId, FailureReason reason, Instant transitionedAt) {
        paymentStatusService.fail(paymentId, reason, transitionedAt);
    }

    @Override
    public void persistCompleted(UUID paymentId, Instant transitionedAt) {
        paymentStatusService.complete(paymentId, transitionedAt);
    }

    @Override
    public void recordTransfer(UUID paymentId, String txHash) {
        paymentStatusService.recordTransfer(paymentId, txHash);
    }

    @Override
    public void recordOnChainRef(UUID paymentId, String onChainRef) {
        paymentStatusService.recordOnChainRef(paymentId, onChainRef);
    }
}
