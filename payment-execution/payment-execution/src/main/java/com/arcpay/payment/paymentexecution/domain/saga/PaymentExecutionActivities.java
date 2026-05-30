package com.arcpay.payment.paymentexecution.domain.saga;

import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import io.temporal.activity.ActivityInterface;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@ActivityInterface
public interface PaymentExecutionActivities {

    boolean verifyAgentActive(UUID agentId);

    String reserve(UUID paymentId, UUID agentId, String recipient, BigDecimal amount);

    void commit(UUID paymentId);

    void release(UUID paymentId);

    void publishScreeningRequested(UUID paymentId);

    String submitTransfer(UUID paymentId, UUID agentId, String recipient, BigDecimal amount);

    void writeReceiptAsync(UUID paymentId);

    void persistStatus(UUID paymentId, PaymentStatus toStatus, Instant transitionedAt);

    void persistRejected(UUID paymentId, RejectionReason reason, Instant transitionedAt);

    void persistFailed(UUID paymentId, FailureReason reason, Instant transitionedAt);

    void persistCompleted(UUID paymentId, Instant transitionedAt);

    void recordTransfer(UUID paymentId, String txHash);

    void recordOnChainRef(UUID paymentId, String onChainRef);
}
