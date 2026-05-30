package com.arcpay.payment.paymentexecution;

import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.event.ScreeningRejected;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.model.FailureReason;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.model.RejectionReason;
import com.arcpay.settlement.domain.event.TransferConfirmed;
import com.arcpay.settlement.domain.event.TransferReverted;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TX_HASH;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePaymentRequested;
import static com.arcpay.payment.paymentexecution.stubs.IdentityServiceStubs.stubAgent;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.RESERVE_PATH;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.commitPath;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.releasePath;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubCommitAccepted;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubReleaseAccepted;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubReserveApproved;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubReserveRejected;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.TRANSFERS_PATH;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubReceiptAccepted;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubTransferAccepted;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubTransferClientError;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.moreThanOrExactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class PaymentSagaE2ETest extends PaymentExecutionBusinessTest {

    @Test
    void shouldDriveHappyPathToCompletedAndCommitWithoutRelease() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubCommitAccepted(policyServer, paymentId);
        stubTransferAccepted(settlementServer, paymentId);
        stubReceiptAccepted(settlementServer);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(), transferConfirmed(paymentId));
        awaitStatus(paymentId, PaymentStatus.COMPLETED);

        // then
        var completed = loadPayment(paymentId);
        assertThat(completed).usingRecursiveComparison()
                .comparingOnlyFields("status", "txHash", "onChainRef", "rejectionReason", "failureReason")
                .isEqualTo(somePayment(PaymentStatus.COMPLETED).toBuilder()
                        .paymentId(paymentId)
                        .txHash("ctx-abc-123")
                        .onChainRef(SOME_TX_HASH)
                        .rejectionReason(null)
                        .failureReason(null)
                        .build());
        var statuses = awaitStatusEvents(paymentId, 4);
        assertThat(statuses).containsExactly("POLICY_CHECK", "SCREENING", "EXECUTING", "COMPLETED");
        policyServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(commitPath(paymentId))));
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(releasePath(paymentId))));
    }

    @Test
    void shouldRejectWithPolicyViolationWithoutTransferOrReservation() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveRejected(policyServer);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.REJECTED);

        // then
        var rejected = loadPayment(paymentId);
        assertThat(rejected).usingRecursiveComparison()
                .comparingOnlyFields("status", "rejectionReason", "txHash")
                .isEqualTo(somePayment(PaymentStatus.REJECTED).toBuilder()
                        .paymentId(paymentId)
                        .rejectionReason(RejectionReason.POLICY_VIOLATION)
                        .txHash(null)
                        .build());
        settlementServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(commitPath(paymentId))));
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(releasePath(paymentId))));
    }

    @Test
    void shouldRejectWithComplianceBlockAndReleaseReservation() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubReleaseAccepted(policyServer, paymentId);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.BLOCK));
        awaitStatus(paymentId, PaymentStatus.REJECTED);

        // then
        var rejected = loadPayment(paymentId);
        assertThat(rejected).usingRecursiveComparison()
                .comparingOnlyFields("status", "rejectionReason")
                .isEqualTo(somePayment(PaymentStatus.REJECTED).toBuilder()
                        .paymentId(paymentId)
                        .rejectionReason(RejectionReason.COMPLIANCE_BLOCK)
                        .build());
        settlementServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
        policyServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(releasePath(paymentId))));
    }

    @Test
    void shouldCompleteAfterHoldThenReviewApproval() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubCommitAccepted(policyServer, paymentId);
        stubTransferAccepted(settlementServer, paymentId);
        stubReceiptAccepted(settlementServer);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.HOLD));
        awaitStatus(paymentId, PaymentStatus.HELD);
        kafkaTemplate.send(ScreeningApproved.TOPIC, paymentId.toString(), screeningApproved(paymentId));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(), transferConfirmed(paymentId));
        awaitStatus(paymentId, PaymentStatus.COMPLETED);

        // then
        var completed = loadPayment(paymentId);
        assertThat(completed).usingRecursiveComparison()
                .comparingOnlyFields("status", "txHash", "onChainRef", "rejectionReason")
                .isEqualTo(somePayment(PaymentStatus.COMPLETED).toBuilder()
                        .paymentId(paymentId)
                        .txHash("ctx-abc-123")
                        .onChainRef(SOME_TX_HASH)
                        .rejectionReason(null)
                        .build());
        var statuses = awaitStatusEvents(paymentId, 5);
        assertThat(statuses).containsExactly("POLICY_CHECK", "SCREENING", "HELD", "EXECUTING", "COMPLETED");
        policyServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(commitPath(paymentId))));
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(releasePath(paymentId))));
    }

    @Test
    void shouldRejectWithReviewDeniedAndReleaseReservationOnReviewDenial() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubReleaseAccepted(policyServer, paymentId);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.HOLD));
        awaitStatus(paymentId, PaymentStatus.HELD);
        kafkaTemplate.send(ScreeningRejected.TOPIC, paymentId.toString(), screeningRejected(paymentId));
        awaitStatus(paymentId, PaymentStatus.REJECTED);

        // then
        var rejected = loadPayment(paymentId);
        assertThat(rejected).usingRecursiveComparison()
                .comparingOnlyFields("status", "rejectionReason")
                .isEqualTo(somePayment(PaymentStatus.REJECTED).toBuilder()
                        .paymentId(paymentId)
                        .rejectionReason(RejectionReason.REVIEW_DENIED)
                        .build());
        settlementServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
        policyServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(releasePath(paymentId))));
    }

    @Test
    void shouldFailWithExecutionRevertedAndReleaseReservationOnChainRevert() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubReleaseAccepted(policyServer, paymentId);
        stubTransferAccepted(settlementServer, paymentId);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferReverted.TOPIC, paymentId.toString(), transferReverted(paymentId));
        awaitStatus(paymentId, PaymentStatus.FAILED);

        // then
        var failed = loadPayment(paymentId);
        assertThat(failed).usingRecursiveComparison()
                .comparingOnlyFields("status", "failureReason", "txHash")
                .isEqualTo(somePayment(PaymentStatus.FAILED).toBuilder()
                        .paymentId(paymentId)
                        .failureReason(FailureReason.EXECUTION_REVERTED)
                        .txHash("ctx-abc-123")
                        .build());
        policyServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(releasePath(paymentId))));
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(commitPath(paymentId))));
    }

    @Test
    void shouldRejectWithAgentNotActiveWhenAgentSuspended() {
        // given
        var paymentId = seedPendingPayment();
        stubAgent(identityServer, SOME_AGENT_ID, SOME_OWNER_ID, "SUSPENDED", SOME_WALLET_ID, SOME_WALLET_ADDRESS);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.REJECTED);

        // then
        var rejected = loadPayment(paymentId);
        assertThat(rejected).usingRecursiveComparison()
                .comparingOnlyFields("status", "rejectionReason")
                .isEqualTo(somePayment(PaymentStatus.REJECTED).toBuilder()
                        .paymentId(paymentId)
                        .rejectionReason(RejectionReason.AGENT_NOT_ACTIVE)
                        .build());
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(RESERVE_PATH)));
        settlementServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
    }

    @Test
    void shouldNotCompleteOrCommitWhenSettlementRejectsTransfer() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubTransferClientError(settlementServer);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);

        // then
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(6))
                .untilAsserted(() -> assertThat(loadStatus(paymentId)).isEqualTo(PaymentStatus.EXECUTING));
        var stuck = loadPayment(paymentId);
        assertThat(stuck).usingRecursiveComparison()
                .comparingOnlyFields("status", "txHash", "rejectionReason", "failureReason")
                .isEqualTo(somePayment(PaymentStatus.EXECUTING).toBuilder()
                        .paymentId(paymentId)
                        .txHash(null)
                        .rejectionReason(null)
                        .failureReason(null)
                        .build());
        policyServer.verify(exactly(0), postRequestedFor(urlPathEqualTo(commitPath(paymentId))));
        settlementServer.verify(moreThanOrExactly(1), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
    }

    @Test
    void shouldRunSingleWorkflowAndSingleTransferOnDuplicatePaymentRequested() {
        // given
        var paymentId = seedPendingPaymentWithActiveAgent();
        stubReserveApproved(policyServer);
        stubCommitAccepted(policyServer, paymentId);
        stubTransferAccepted(settlementServer, paymentId);
        stubReceiptAccepted(settlementServer);

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(), transferConfirmed(paymentId));
        awaitStatus(paymentId, PaymentStatus.COMPLETED);

        // then
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(loadStatus(paymentId)).isEqualTo(PaymentStatus.COMPLETED));
        settlementServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
    }

    private UUID seedPendingPaymentWithActiveAgent() {
        var paymentId = seedPendingPayment();
        stubAgent(identityServer, SOME_AGENT_ID, SOME_OWNER_ID, "ACTIVE", SOME_WALLET_ID, SOME_WALLET_ADDRESS);
        return paymentId;
    }

    private UUID seedPendingPayment() {
        var paymentId = UUID.randomUUID();
        var payment = somePayment(PaymentStatus.PENDING).toBuilder()
                .paymentId(paymentId)
                .idempotencyKey("idem-" + paymentId)
                .requestFingerprint("0xfp-" + paymentId)
                .build();
        paymentRepository.save(payment);
        return paymentId;
    }

    private PaymentRequested paymentRequested(UUID paymentId) {
        return somePaymentRequested().toBuilder()
                .paymentId(paymentId)
                .idempotencyKey("idem-" + paymentId)
                .build();
    }

    private ScreeningCompleted screeningCompleted(UUID paymentId, Verdict verdict) {
        return new ScreeningCompleted(paymentId, SOME_AGENT_ID, verdict, 10, List.of(), null, Instant.now());
    }

    private ScreeningApproved screeningApproved(UUID paymentId) {
        return new ScreeningApproved(paymentId, "officer@arcpay.dev", "cleared after review", Instant.now());
    }

    private ScreeningRejected screeningRejected(UUID paymentId) {
        return new ScreeningRejected(paymentId, "officer@arcpay.dev", "denied after review", Instant.now());
    }

    private TransferConfirmed transferConfirmed(UUID paymentId) {
        return new TransferConfirmed(paymentId, SOME_TX_HASH, new BigDecimal("0.01"), Instant.now());
    }

    private TransferReverted transferReverted(UUID paymentId) {
        return new TransferReverted(paymentId, "ON_CHAIN_REVERT", Instant.now());
    }
}
