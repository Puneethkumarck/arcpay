package com.arcpay.payment.paymentexecution.infrastructure.temporal;

import com.arcpay.payment.paymentexecution.domain.model.ChainResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.PaymentExecutionInput;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.arcpay.payment.paymentexecution.domain.model.ReviewDecisionSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningVerdict;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionActivities;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.domain.model.FailureReason.CHAIN_TIMEOUT;
import static com.arcpay.payment.paymentexecution.domain.model.FailureReason.EXECUTION_REVERTED;
import static com.arcpay.payment.paymentexecution.domain.model.RejectionReason.AGENT_NOT_ACTIVE;
import static com.arcpay.payment.paymentexecution.domain.model.RejectionReason.COMPLIANCE_BLOCK;
import static com.arcpay.payment.paymentexecution.domain.model.RejectionReason.POLICY_VIOLATION;
import static com.arcpay.payment.paymentexecution.domain.model.RejectionReason.REVIEW_DENIED;

@WorkflowImpl(taskQueues = "PaymentExecutionTaskQueue")
class PaymentExecutionWorkflowImpl implements PaymentExecutionWorkflow {

    private static final Logger log = Workflow.getLogger(PaymentExecutionWorkflowImpl.class);

    private static final String POLICY_REJECTED = "REJECTED";
    private static final Duration SCREENING_TIMEOUT = Duration.ofHours(72);
    private static final Duration REVIEW_TIMEOUT = Duration.ofHours(72);
    private static final Duration CHAIN_TIMEOUT_WINDOW = Duration.ofMinutes(5);

    private final PaymentExecutionActivities decisionActivities = Workflow.newActivityStub(
            PaymentExecutionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setScheduleToCloseTimeout(Duration.ofMinutes(5))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .build())
                    .build());

    private final PaymentExecutionActivities ledgerActivities = Workflow.newActivityStub(
            PaymentExecutionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(5))
                    .setScheduleToCloseTimeout(Duration.ofHours(24))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(1))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofMinutes(1))
                            .build())
                    .build());

    private final PaymentExecutionActivities receiptActivities = Workflow.newActivityStub(
            PaymentExecutionActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(10))
                    .setScheduleToCloseTimeout(Duration.ofMinutes(10))
                    .build());

    private ScreeningResultSignal screeningResult;
    private ReviewDecisionSignal reviewDecision;
    private ChainResultSignal chainResult;
    private boolean terminal;

    @Override
    public void execute(PaymentExecutionInput input) {
        var paymentId = input.paymentId();
        log.info("Starting payment execution paymentId={}", paymentId);

        if (!decisionActivities.verifyAgentActive(input.agentId())) {
            ledgerActivities.persistRejected(paymentId, AGENT_NOT_ACTIVE, now());
            terminal = true;
            return;
        }

        decisionActivities.persistStatus(paymentId, PaymentStatus.POLICY_CHECK, now());
        var verdict = decisionActivities.reserve(paymentId, input.agentId(), input.recipient(), input.amount());
        if (POLICY_REJECTED.equals(verdict)) {
            ledgerActivities.persistRejected(paymentId, POLICY_VIOLATION, now());
            terminal = true;
            return;
        }

        decisionActivities.persistStatus(paymentId, PaymentStatus.SCREENING, now());
        decisionActivities.publishScreeningRequested(paymentId);

        if (!Workflow.await(SCREENING_TIMEOUT, () -> screeningResult != null)) {
            releaseAndReject(paymentId, REVIEW_DENIED);
            return;
        }

        if (screeningResult.verdict() == ScreeningVerdict.BLOCK) {
            releaseAndReject(paymentId, COMPLIANCE_BLOCK);
            return;
        }

        if (screeningResult.verdict() == ScreeningVerdict.HOLD) {
            decisionActivities.persistStatus(paymentId, PaymentStatus.HELD, now());
            if (!Workflow.await(REVIEW_TIMEOUT, () -> reviewDecision != null)) {
                releaseAndReject(paymentId, REVIEW_DENIED);
                return;
            }
            if (!reviewDecision.approved()) {
                releaseAndReject(paymentId, REVIEW_DENIED);
                return;
            }
        }

        decisionActivities.persistStatus(paymentId, PaymentStatus.EXECUTING, now());
        var txHash = decisionActivities.submitTransfer(
                paymentId, input.agentId(), input.recipient(), input.amount());
        ledgerActivities.recordTransfer(paymentId, txHash);

        if (!Workflow.await(CHAIN_TIMEOUT_WINDOW, () -> chainResult != null)) {
            log.warn("Chain confirmation timed out paymentId={} txHash={}", paymentId, txHash);
            ledgerActivities.release(paymentId);
            ledgerActivities.persistFailed(paymentId, CHAIN_TIMEOUT, now());
            terminal = true;
            return;
        }

        if (!chainResult.confirmed()) {
            ledgerActivities.release(paymentId);
            ledgerActivities.persistFailed(paymentId, EXECUTION_REVERTED, now());
            terminal = true;
            return;
        }

        ledgerActivities.recordOnChainRef(paymentId, chainResult.onChainRef());
        ledgerActivities.commit(paymentId);
        ledgerActivities.persistCompleted(paymentId, now());
        terminal = true;
        receiptActivities.writeReceiptAsync(paymentId);
        log.info("Payment execution completed paymentId={}", paymentId);
    }

    @Override
    public void onScreeningResult(ScreeningResultSignal signal) {
        if (terminal || screeningResult != null) {
            return;
        }
        screeningResult = signal;
    }

    @Override
    public void onReviewDecision(ReviewDecisionSignal signal) {
        if (terminal || reviewDecision != null) {
            return;
        }
        reviewDecision = signal;
    }

    @Override
    public void onChainResult(ChainResultSignal signal) {
        if (terminal || chainResult != null) {
            return;
        }
        chainResult = signal;
    }

    private void releaseAndReject(UUID paymentId, RejectionReason reason) {
        ledgerActivities.release(paymentId);
        ledgerActivities.persistRejected(paymentId, reason, now());
        terminal = true;
    }

    private Instant now() {
        return Instant.ofEpochMilli(Workflow.currentTimeMillis());
    }
}
