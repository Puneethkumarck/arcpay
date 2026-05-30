package com.arcpay.payment.paymentexecution.domain.saga;

import com.arcpay.payment.paymentexecution.domain.model.ChainResultSignal;
import com.arcpay.payment.paymentexecution.domain.model.PaymentExecutionInput;
import com.arcpay.payment.paymentexecution.domain.model.ReviewDecisionSignal;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningResultSignal;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.UUID;

@WorkflowInterface
public interface PaymentExecutionWorkflow {

    String TASK_QUEUE = "PaymentExecutionTaskQueue";

    @WorkflowMethod
    void execute(PaymentExecutionInput input);

    @SignalMethod
    void onScreeningResult(ScreeningResultSignal signal);

    @SignalMethod
    void onReviewDecision(ReviewDecisionSignal signal);

    @SignalMethod
    void onChainResult(ChainResultSignal signal);

    static String workflowId(UUID paymentId) {
        return "PaymentExecution_" + paymentId;
    }
}
