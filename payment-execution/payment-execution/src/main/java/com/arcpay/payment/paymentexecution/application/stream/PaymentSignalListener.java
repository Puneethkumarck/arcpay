package com.arcpay.payment.paymentexecution.application.stream;

import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.event.ScreeningRejected;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.payment.paymentexecution.domain.model.ChainResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.ReviewDecisionSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningVerdict;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionWorkflow;
import com.arcpay.settlement.domain.event.TransferConfirmed;
import com.arcpay.settlement.domain.event.TransferReverted;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Slf4j
@Component
@RequiredArgsConstructor
class PaymentSignalListener {

    private final WorkflowClient workflowClient;

    @KafkaListener(topics = ScreeningCompleted.TOPIC, groupId = "${spring.kafka.consumer.group-id}-screening-result")
    void onScreeningCompleted(ScreeningCompleted event) {
        log.info("Received screening completed paymentId={} verdict={}", event.paymentId(), event.verdict());
        signal(event.paymentId(), workflow -> workflow.onScreeningResult(toScreeningResult(event)));
    }

    @KafkaListener(topics = ScreeningApproved.TOPIC, groupId = "${spring.kafka.consumer.group-id}-review-approved")
    void onScreeningApproved(ScreeningApproved event) {
        log.info("Received review approval paymentId={} reviewer={}", event.paymentId(), event.reviewer());
        signal(event.paymentId(), workflow -> workflow.onReviewDecision(new ReviewDecisionSignal(true)));
    }

    @KafkaListener(topics = ScreeningRejected.TOPIC, groupId = "${spring.kafka.consumer.group-id}-review-rejected")
    void onScreeningRejected(ScreeningRejected event) {
        log.info("Received review rejection paymentId={} reviewer={}", event.paymentId(), event.reviewer());
        signal(event.paymentId(), workflow -> workflow.onReviewDecision(new ReviewDecisionSignal(false)));
    }

    @KafkaListener(topics = TransferConfirmed.TOPIC, groupId = "${spring.kafka.consumer.group-id}-transfer-confirmed")
    void onTransferConfirmed(TransferConfirmed event) {
        log.info("Received transfer confirmed paymentId={} txHash={}", event.paymentId(), event.txHash());
        signal(event.paymentId(), workflow -> workflow.onChainResult(toConfirmedResult(event)));
    }

    @KafkaListener(topics = TransferReverted.TOPIC, groupId = "${spring.kafka.consumer.group-id}-transfer-reverted")
    void onTransferReverted(TransferReverted event) {
        log.info("Received transfer reverted paymentId={} reason={}", event.paymentId(), event.reason());
        signal(event.paymentId(), workflow -> workflow.onChainResult(toRevertedResult()));
    }

    private void signal(UUID paymentId, Consumer<PaymentExecutionWorkflow> signal) {
        var workflow = workflowClient.newWorkflowStub(
                PaymentExecutionWorkflow.class, PaymentExecutionWorkflow.workflowId(paymentId));
        try {
            signal.accept(workflow);
        } catch (WorkflowNotFoundException e) {
            log.warn("No running workflow to signal paymentId={}, dropping signal", paymentId);
        }
    }

    private ScreeningResultSignal toScreeningResult(ScreeningCompleted event) {
        return ScreeningResultSignal.builder()
                .verdict(toScreeningVerdict(event.verdict()))
                .riskScore(event.riskScore())
                .build();
    }

    private ScreeningVerdict toScreeningVerdict(Verdict verdict) {
        return switch (verdict) {
            case PASS -> ScreeningVerdict.PASS;
            case HOLD -> ScreeningVerdict.HOLD;
            case BLOCK -> ScreeningVerdict.BLOCK;
        };
    }

    private ChainResultSignal toConfirmedResult(TransferConfirmed event) {
        return ChainResultSignal.builder()
                .confirmed(true)
                .onChainRef(event.txHash())
                .build();
    }

    private ChainResultSignal toRevertedResult() {
        return ChainResultSignal.builder()
                .confirmed(false)
                .build();
    }
}
