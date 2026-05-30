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
import io.temporal.api.common.v1.WorkflowExecution;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_PAYMENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TX_HASH;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class PaymentSignalListenerTest {

    private static final String WORKFLOW_ID = "PaymentExecution_" + SOME_PAYMENT_ID;

    @Mock
    private WorkflowClient workflowClient;

    @InjectMocks
    private PaymentSignalListener listener;

    @Test
    void shouldSignalScreeningResultOnScreeningCompleted() {
        // given
        var workflow = givenStub();
        var event = new ScreeningCompleted(
                SOME_PAYMENT_ID, SOME_AGENT_ID, Verdict.HOLD, 55, List.of(), null, Instant.now());

        // when
        listener.onScreeningCompleted(event);

        // then
        var expected = ScreeningResultSignal.builder().verdict(ScreeningVerdict.HOLD).riskScore(55).build();
        then(workflow).should().onScreeningResult(expected);
    }

    @Test
    void shouldSignalApprovedReviewDecisionOnScreeningApproved() {
        // given
        var workflow = givenStub();
        var event = new ScreeningApproved(SOME_PAYMENT_ID, "reviewer-1", "looks fine", Instant.now());

        // when
        listener.onScreeningApproved(event);

        // then
        then(workflow).should().onReviewDecision(new ReviewDecisionSignal(true));
    }

    @Test
    void shouldSignalDeniedReviewDecisionOnScreeningRejected() {
        // given
        var workflow = givenStub();
        var event = new ScreeningRejected(SOME_PAYMENT_ID, "reviewer-1", "sanctioned", Instant.now());

        // when
        listener.onScreeningRejected(event);

        // then
        then(workflow).should().onReviewDecision(new ReviewDecisionSignal(false));
    }

    @Test
    void shouldSignalConfirmedChainResultOnTransferConfirmed() {
        // given
        var workflow = givenStub();
        var event = new TransferConfirmed(SOME_PAYMENT_ID, SOME_TX_HASH, new BigDecimal("0.01"), Instant.now());

        // when
        listener.onTransferConfirmed(event);

        // then
        var expected = ChainResultSignal.builder().confirmed(true).onChainRef(SOME_TX_HASH).build();
        then(workflow).should().onChainResult(expected);
    }

    @Test
    void shouldSignalRevertedChainResultOnTransferReverted() {
        // given
        var workflow = givenStub();
        var event = new TransferReverted(SOME_PAYMENT_ID, "insufficient funds", Instant.now());

        // when
        listener.onTransferReverted(event);

        // then
        var expected = ChainResultSignal.builder().confirmed(false).build();
        then(workflow).should().onChainResult(expected);
    }

    @Test
    void shouldDropSignalWhenWorkflowNotFound() {
        // given
        var workflow = givenStub();
        var execution = WorkflowExecution.newBuilder().setWorkflowId(WORKFLOW_ID).build();
        willThrow(new WorkflowNotFoundException(execution, "missing", null))
                .given(workflow).onChainResult(ChainResultSignal.builder().confirmed(false).build());
        var event = new TransferReverted(SOME_PAYMENT_ID, "insufficient funds", Instant.now());

        // when
        listener.onTransferReverted(event);

        // then
        then(workflow).should().onChainResult(ChainResultSignal.builder().confirmed(false).build());
    }

    private PaymentExecutionWorkflow givenStub() {
        var workflow = mock(PaymentExecutionWorkflow.class);
        given(workflowClient.newWorkflowStub(PaymentExecutionWorkflow.class, WORKFLOW_ID)).willReturn(workflow);
        return workflow;
    }
}
