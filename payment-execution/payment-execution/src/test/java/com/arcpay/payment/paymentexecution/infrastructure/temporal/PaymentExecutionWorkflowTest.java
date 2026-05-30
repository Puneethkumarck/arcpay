package com.arcpay.payment.paymentexecution.infrastructure.temporal;

import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.arcpay.payment.paymentexecution.domain.model.ScreeningVerdict;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionActivities;
import com.arcpay.payment.paymentexecution.domain.saga.PaymentExecutionWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AMOUNT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_PAYMENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_RECIPIENT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TX_HASH;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someChainResult;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someExecutionInput;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someReviewDecision;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.someScreeningResult;
import static com.arcpay.platform.test.TestUtils.eqIgnoringTimestamps;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class PaymentExecutionWorkflowTest {

    private static final String TASK_QUEUE = PaymentExecutionWorkflow.TASK_QUEUE;
    private static final String POLICY_APPROVED = "APPROVED";
    private static final String POLICY_REJECTED = "REJECTED";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient client;
    private PaymentExecutionActivities activities;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationFactory(
                PaymentExecutionWorkflow.class, PaymentExecutionWorkflowImpl::new);
        activities = Mockito.mock(PaymentExecutionActivities.class);
        worker.registerActivitiesImplementations(activities);
        client = testEnv.getWorkflowClient();
        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void shouldCompletePaymentWhenScreeningPassesAndChainConfirms() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        givenTransfer();
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.PASS)));
        testEnv.registerDelayedCallback(Duration.ofSeconds(2),
                () -> workflow.onChainResult(someChainResult(true)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofMinutes(10));

        // then
        InOrder inOrder = Mockito.inOrder(activities);
        inOrder.verify(activities).verifyAgentActive(SOME_AGENT_ID);
        inOrder.verify(activities).persistStatus(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(PaymentStatus.POLICY_CHECK), anyInstant());
        inOrder.verify(activities).reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
        inOrder.verify(activities).persistStatus(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(PaymentStatus.SCREENING), anyInstant());
        inOrder.verify(activities).publishScreeningRequested(SOME_PAYMENT_ID);
        inOrder.verify(activities).persistStatus(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(PaymentStatus.EXECUTING), anyInstant());
        inOrder.verify(activities).submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
        inOrder.verify(activities).commit(SOME_PAYMENT_ID);
        inOrder.verify(activities).persistCompleted(eqIgnoringTimestamps(SOME_PAYMENT_ID), anyInstant());
        inOrder.verify(activities).writeReceiptAsync(SOME_PAYMENT_ID);
        then(activities).should(never()).release(SOME_PAYMENT_ID);
    }

    @Test
    void shouldRejectWhenAgentNotActive() {
        // given
        given(activities.verifyAgentActive(SOME_AGENT_ID)).willReturn(false);
        var workflow = newWorkflow();

        // when
        workflow.execute(someExecutionInput());

        // then
        then(activities).should()
                .persistRejected(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(RejectionReason.AGENT_NOT_ACTIVE), anyInstant());
        then(activities).should(never()).reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
        then(activities).should(never()).release(SOME_PAYMENT_ID);
    }

    @Test
    void shouldRejectOnPolicyViolationWithoutTransferOrRelease() {
        // given
        givenAgentActive();
        givenReserve(POLICY_REJECTED);
        var workflow = newWorkflow();

        // when
        workflow.execute(someExecutionInput());

        // then
        then(activities).should()
                .persistRejected(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(RejectionReason.POLICY_VIOLATION), anyInstant());
        then(activities).should(never()).publishScreeningRequested(SOME_PAYMENT_ID);
        then(activities).should(never()).submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
        then(activities).should(never()).release(SOME_PAYMENT_ID);
    }

    @Test
    void shouldReleaseAndRejectWhenComplianceBlocks() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.BLOCK)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofMinutes(10));

        // then
        InOrder inOrder = Mockito.inOrder(activities);
        inOrder.verify(activities).release(SOME_PAYMENT_ID);
        inOrder.verify(activities)
                .persistRejected(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(RejectionReason.COMPLIANCE_BLOCK), anyInstant());
        then(activities).should(never()).submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
    }

    @Test
    void shouldCompleteWhenHeldThenApproved() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        givenTransfer();
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.HOLD)));
        testEnv.registerDelayedCallback(Duration.ofHours(1),
                () -> workflow.onReviewDecision(someReviewDecision(true)));
        testEnv.registerDelayedCallback(Duration.ofHours(1).plusSeconds(2),
                () -> workflow.onChainResult(someChainResult(true)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofHours(2));

        // then
        then(activities).should().persistStatus(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(PaymentStatus.HELD), anyInstant());
        then(activities).should().commit(SOME_PAYMENT_ID);
        then(activities).should().persistCompleted(eqIgnoringTimestamps(SOME_PAYMENT_ID), anyInstant());
        then(activities).should(never()).release(SOME_PAYMENT_ID);
    }

    @Test
    void shouldReleaseAndRejectWhenHeldThenDenied() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.HOLD)));
        testEnv.registerDelayedCallback(Duration.ofHours(1),
                () -> workflow.onReviewDecision(someReviewDecision(false)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofHours(2));

        // then
        then(activities).should().release(SOME_PAYMENT_ID);
        then(activities).should()
                .persistRejected(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(RejectionReason.REVIEW_DENIED), anyInstant());
        then(activities).should(never()).submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
    }

    @Test
    void shouldRejectAfterScreeningTimeoutAndIgnoreLateApproval() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofHours(73),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.PASS)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofHours(80));

        // then
        then(activities).should().release(SOME_PAYMENT_ID);
        then(activities).should()
                .persistRejected(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(RejectionReason.REVIEW_DENIED), anyInstant());
        then(activities).should(never()).submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
    }

    @Test
    void shouldFailWithChainTimeoutWhenNoConfirmation() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        givenTransfer();
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.PASS)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofMinutes(10));

        // then
        then(activities).should().recordTransfer(SOME_PAYMENT_ID, SOME_TX_HASH);
        then(activities).should().release(SOME_PAYMENT_ID);
        then(activities).should()
                .persistFailed(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(FailureReason.CHAIN_TIMEOUT), anyInstant());
        then(activities).should(never()).commit(SOME_PAYMENT_ID);
    }

    @Test
    void shouldFailWhenChainReverts() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        givenTransfer();
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.PASS)));
        testEnv.registerDelayedCallback(Duration.ofSeconds(2),
                () -> workflow.onChainResult(someChainResult(false)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofMinutes(10));

        // then
        InOrder inOrder = Mockito.inOrder(activities);
        inOrder.verify(activities).submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT);
        inOrder.verify(activities).recordTransfer(SOME_PAYMENT_ID, SOME_TX_HASH);
        inOrder.verify(activities).release(SOME_PAYMENT_ID);
        inOrder.verify(activities)
                .persistFailed(eqIgnoringTimestamps(SOME_PAYMENT_ID), eqIgnoringTimestamps(FailureReason.EXECUTION_REVERTED), anyInstant());
        then(activities).should(never()).commit(SOME_PAYMENT_ID);
        then(activities).should(never()).persistCompleted(eqIgnoringTimestamps(SOME_PAYMENT_ID), anyInstant());
    }

    @Test
    void shouldBlockCompletionUntilCommitSucceeds() {
        // given
        givenAgentActive();
        givenReserve(POLICY_APPROVED);
        givenTransfer();
        willThrow(new RuntimeException("ledger lag"))
                .willThrow(new RuntimeException("ledger lag"))
                .willDoNothing()
                .given(activities).commit(SOME_PAYMENT_ID);
        var workflow = newWorkflow();
        testEnv.registerDelayedCallback(Duration.ofSeconds(1),
                () -> workflow.onScreeningResult(someScreeningResult(ScreeningVerdict.PASS)));
        testEnv.registerDelayedCallback(Duration.ofSeconds(2),
                () -> workflow.onChainResult(someChainResult(true)));

        // when
        WorkflowClient.start(workflow::execute, someExecutionInput());
        testEnv.sleep(Duration.ofMinutes(10));

        // then
        then(activities).should(times(3)).commit(SOME_PAYMENT_ID);
        then(activities).should().persistCompleted(eqIgnoringTimestamps(SOME_PAYMENT_ID), anyInstant());
    }

    private void givenAgentActive() {
        given(activities.verifyAgentActive(SOME_AGENT_ID)).willReturn(true);
    }

    private void givenReserve(String verdict) {
        given(activities.reserve(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT)).willReturn(verdict);
    }

    private void givenTransfer() {
        given(activities.submitTransfer(SOME_PAYMENT_ID, SOME_AGENT_ID, SOME_RECIPIENT, SOME_AMOUNT))
                .willReturn(SOME_TX_HASH);
    }

    private Instant anyInstant() {
        return argThat(instant -> instant != null);
    }

    private PaymentExecutionWorkflow newWorkflow() {
        return client.newWorkflowStub(
                PaymentExecutionWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(PaymentExecutionWorkflow.workflowId(SOME_PAYMENT_ID))
                        .setTaskQueue(TASK_QUEUE)
                        .build());
    }
}
