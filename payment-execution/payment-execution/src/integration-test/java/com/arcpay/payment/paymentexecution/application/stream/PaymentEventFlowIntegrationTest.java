package com.arcpay.payment.paymentexecution.application.stream;

import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.payment.paymentexecution.api.model.PolicyResult;
import com.arcpay.payment.paymentexecution.domain.event.PaymentRequested;
import com.arcpay.payment.paymentexecution.domain.model.AgentInfo;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.domain.port.AgentServiceClient;
import com.arcpay.payment.paymentexecution.domain.port.CompliancePort;
import com.arcpay.payment.paymentexecution.domain.port.PaymentRepository;
import com.arcpay.payment.paymentexecution.domain.port.PolicyPort;
import com.arcpay.payment.paymentexecution.domain.port.SettlementPort;
import com.arcpay.payment.paymentexecution.test.FullContextIntegrationTest;
import com.arcpay.settlement.domain.event.TransferConfirmed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_RECIPIENT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TX_HASH;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePaymentRequested;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.BDDMockito.given;

class PaymentEventFlowIntegrationTest extends FullContextIntegrationTest {

    @MockitoBean
    private AgentServiceClient agentServiceClient;

    @MockitoBean
    private PolicyPort policyPort;

    @MockitoBean
    private SettlementPort settlementPort;

    @MockitoBean
    private CompliancePort compliancePort;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM payment");
        given(agentServiceClient.getAgent(SOME_AGENT_ID)).willReturn(Optional.of(activeAgent()));
    }

    @Test
    void shouldDriveWorkflowToCompletionThroughScreeningAndChainConfirmation() {
        // given
        var paymentId = seedPendingPayment();

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(), screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(), transferConfirmed(paymentId));

        // then
        awaitStatus(paymentId, PaymentStatus.COMPLETED);
    }

    @Test
    void shouldNotStartSecondWorkflowOnDuplicatePaymentRequested() {
        // given
        var paymentId = seedPendingPayment();

        // when
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(), screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(), transferConfirmed(paymentId));

        // then
        awaitStatus(paymentId, PaymentStatus.COMPLETED);
    }

    @Test
    void shouldDropChainSignalForUnknownPaymentWithoutBreakingListener() {
        // given
        var paymentId = seedPendingPayment();
        var unknownPaymentId = UUID.randomUUID();

        // when
        kafkaTemplate.send(TransferConfirmed.TOPIC, unknownPaymentId.toString(),
                new TransferConfirmed(unknownPaymentId, SOME_TX_HASH, new BigDecimal("0.01"), Instant.now()));
        kafkaTemplate.send(PaymentRequested.TOPIC, paymentId.toString(), paymentRequested(paymentId));
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(), screeningCompleted(paymentId, Verdict.PASS));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(), transferConfirmed(paymentId));

        // then
        awaitStatus(paymentId, PaymentStatus.COMPLETED);
    }

    private UUID seedPendingPayment() {
        var paymentId = UUID.randomUUID();
        var idempotencyKey = "idem-" + paymentId;
        var payment = somePayment(PaymentStatus.PENDING).toBuilder()
                .paymentId(paymentId)
                .idempotencyKey(idempotencyKey)
                .requestFingerprint("0xfp-" + paymentId)
                .build();
        paymentRepository.save(payment);
        given(policyPort.reserve(paymentId, SOME_AGENT_ID, SOME_RECIPIENT, payment.amount()))
                .willReturn(PolicyResult.builder().verdict("APPROVED").rulesEvaluated(3).build());
        given(settlementPort.transfer(paymentId, SOME_WALLET_ID, SOME_RECIPIENT, payment.amount()))
                .willReturn(SOME_TX_HASH);
        return paymentId;
    }

    private PaymentRequested paymentRequested(UUID paymentId) {
        return somePaymentRequested().toBuilder()
                .paymentId(paymentId)
                .idempotencyKey("idem-" + paymentId)
                .build();
    }

    private void awaitStatus(UUID paymentId, PaymentStatus status) {
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> assertThat(loadStatus(paymentId)).isEqualTo(status));
    }

    private PaymentStatus loadStatus(UUID paymentId) {
        return paymentRepository.findById(paymentId).map(Payment::status).orElse(null);
    }

    private AgentInfo activeAgent() {
        return AgentInfo.builder()
                .agentId(SOME_AGENT_ID)
                .status("ACTIVE")
                .walletId(SOME_WALLET_ID)
                .build();
    }

    private ScreeningCompleted screeningCompleted(UUID paymentId, Verdict verdict) {
        return new ScreeningCompleted(paymentId, SOME_AGENT_ID, verdict, 10, List.of(), null, Instant.now());
    }

    private TransferConfirmed transferConfirmed(UUID paymentId) {
        return new TransferConfirmed(paymentId, SOME_TX_HASH, new BigDecimal("0.01"), Instant.now());
    }
}
