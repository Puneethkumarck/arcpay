package com.arcpay.payment.paymentexecution.application.stream;

import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionWorkflow;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_PAYMENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePaymentRequested;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentExecutionTriggerTest {

    @Mock
    private WorkflowClient workflowClient;

    @InjectMocks
    private PaymentExecutionTrigger trigger;

    @Test
    void shouldStartPaymentExecutionWorkflowWithCorrectOptions() {
        // given
        PaymentRequested event = somePaymentRequested();
        var expectedOptions = WorkflowOptions.newBuilder()
                .setWorkflowId("PaymentExecution_" + SOME_PAYMENT_ID)
                .setTaskQueue("PaymentExecutionTaskQueue")
                .setWorkflowExecutionTimeout(Duration.ofHours(145))
                .setWorkflowIdReusePolicy(WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                .build();
        var mockWorkflow = mock(PaymentExecutionWorkflow.class);
        given(workflowClient.newWorkflowStub(PaymentExecutionWorkflow.class, expectedOptions))
                .willReturn(mockWorkflow);

        // when
        trigger.onPaymentRequested(event);

        // then
        then(workflowClient).should().newWorkflowStub(PaymentExecutionWorkflow.class, expectedOptions);
    }
}
