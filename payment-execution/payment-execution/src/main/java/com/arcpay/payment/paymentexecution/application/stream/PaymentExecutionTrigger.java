package com.arcpay.payment.paymentexecution.application.stream;

import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.model.PaymentExecutionInput;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionWorkflow;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
class PaymentExecutionTrigger {

    private static final Duration EXECUTION_TIMEOUT = Duration.ofHours(145);

    private final WorkflowClient workflowClient;

    @KafkaListener(topics = PaymentRequested.TOPIC, groupId = "${spring.kafka.consumer.group-id}-payment-trigger")
    void onPaymentRequested(PaymentRequested event) {
        log.info("Received payment requested event paymentId={} agentId={}", event.paymentId(), event.agentId());
        var input = toExecutionInput(event);

        var options = WorkflowOptions.newBuilder()
                .setWorkflowId(PaymentExecutionWorkflow.workflowId(event.paymentId()))
                .setTaskQueue(PaymentExecutionWorkflow.TASK_QUEUE)
                .setWorkflowExecutionTimeout(EXECUTION_TIMEOUT)
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();

        try {
            var workflow = workflowClient.newWorkflowStub(PaymentExecutionWorkflow.class, options);
            WorkflowClient.start(workflow::execute, input);
            log.info("Started payment execution workflow paymentId={}", event.paymentId());
        } catch (WorkflowExecutionAlreadyStarted e) {
            log.info("Payment execution workflow already running paymentId={}, skipping duplicate", event.paymentId());
        }
    }

    private PaymentExecutionInput toExecutionInput(PaymentRequested event) {
        return PaymentExecutionInput.builder()
                .paymentId(event.paymentId())
                .agentId(event.agentId())
                .walletId(event.walletId())
                .recipient(event.recipientAddress())
                .amount(event.amount())
                .memo(event.memo())
                .build();
    }
}
