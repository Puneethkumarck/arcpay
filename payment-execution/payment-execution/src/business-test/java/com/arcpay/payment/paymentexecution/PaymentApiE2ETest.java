package com.arcpay.payment.paymentexecution;

import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.payment.paymentexecution.api.model.PaymentResponse;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter;
import com.arcpay.settlement.domain.event.TransferConfirmed;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_RECIPIENT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_TX_HASH;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ADDRESS;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.somePayment;
import static com.arcpay.payment.paymentexecution.stubs.IdentityServiceStubs.stubAgent;
import static com.arcpay.payment.paymentexecution.stubs.IdentityServiceStubs.stubApiKey;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubCommitAccepted;
import static com.arcpay.payment.paymentexecution.stubs.PolicyServiceStubs.stubReserveApproved;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.TRANSFERS_PATH;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubReceiptAccepted;
import static com.arcpay.payment.paymentexecution.stubs.SettlementServiceStubs.stubTransferAccepted;
import static com.github.tomakehurst.wiremock.client.WireMock.exactly;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class PaymentApiE2ETest extends PaymentExecutionBusinessTest {

    private static final String OWNER_API_KEY = "owner-api-key-secret";

    @Test
    void shouldCompletePaymentEndToEndThroughRestThenScreeningAndChainConfirmation() {
        // given
        stubOwnerAuthAndActiveAgent();
        stubReserveApproved(policyServer);
        stubReceiptAccepted(settlementServer);
        var idempotencyKey = "invoice-e2e-" + UUID.randomUUID();

        // when
        var created = postPayment(idempotencyKey, HttpStatus.ACCEPTED);
        var paymentId = created.paymentId();
        stubCommitAccepted(policyServer, paymentId);
        stubTransferAccepted(settlementServer, paymentId);
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                new ScreeningCompleted(paymentId, SOME_AGENT_ID, Verdict.PASS, 5, List.of(), null, Instant.now()));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(),
                new TransferConfirmed(paymentId, SOME_TX_HASH, new BigDecimal("0.01"), Instant.now()));
        awaitStatus(paymentId, PaymentStatus.COMPLETED);

        // then
        assertThat(created).usingRecursiveComparison()
                .comparingOnlyFields("status", "transactionHash")
                .isEqualTo(PaymentResponse.builder()
                        .status("PENDING")
                        .transactionHash(null)
                        .build());
        var completed = loadPayment(paymentId);
        assertThat(completed).usingRecursiveComparison()
                .comparingOnlyFields("status", "txHash", "onChainRef", "ownerId")
                .isEqualTo(somePayment(PaymentStatus.COMPLETED).toBuilder()
                        .paymentId(paymentId)
                        .ownerId(SOME_OWNER_ID)
                        .txHash("ctx-abc-123")
                        .onChainRef(SOME_TX_HASH)
                        .build());
        var statuses = awaitStatusEvents(paymentId, 4);
        assertThat(statuses).containsExactly("POLICY_CHECK", "SCREENING", "EXECUTING", "COMPLETED");
    }

    @Test
    void shouldReturnExistingPaymentAndSingleTransferOnIdempotentReplay() {
        // given
        stubOwnerAuthAndActiveAgent();
        stubReserveApproved(policyServer);
        stubReceiptAccepted(settlementServer);
        var idempotencyKey = "invoice-idem-" + UUID.randomUUID();
        var created = postPayment(idempotencyKey, HttpStatus.ACCEPTED);
        var paymentId = created.paymentId();
        stubCommitAccepted(policyServer, paymentId);
        stubTransferAccepted(settlementServer, paymentId);

        // when
        var replay = postPayment(idempotencyKey, HttpStatus.OK);
        awaitStatus(paymentId, PaymentStatus.SCREENING);
        kafkaTemplate.send(ScreeningCompleted.TOPIC, paymentId.toString(),
                new ScreeningCompleted(paymentId, SOME_AGENT_ID, Verdict.PASS, 5, List.of(), null, Instant.now()));
        awaitStatus(paymentId, PaymentStatus.EXECUTING);
        kafkaTemplate.send(TransferConfirmed.TOPIC, paymentId.toString(),
                new TransferConfirmed(paymentId, SOME_TX_HASH, new BigDecimal("0.01"), Instant.now()));
        awaitStatus(paymentId, PaymentStatus.COMPLETED);

        // then
        assertThat(replay.paymentId()).isEqualTo(paymentId);
        assertThat(paymentCount()).isEqualTo(1);
        settlementServer.verify(exactly(1), postRequestedFor(urlPathEqualTo(TRANSFERS_PATH)));
    }

    private void stubOwnerAuthAndActiveAgent() {
        stubApiKey(identityServer, ApiKeyAuthFilter.hashApiKey(OWNER_API_KEY), SOME_OWNER_ID, SOME_OWNER_EMAIL);
        stubAgent(identityServer, SOME_AGENT_ID, SOME_OWNER_ID, "ACTIVE", SOME_WALLET_ID, SOME_WALLET_ADDRESS);
    }

    private PaymentResponse postPayment(String idempotencyKey, HttpStatus expectedStatus) {
        return restClient().post()
                .uri("/api/v1/payments")
                .header("Authorization", "Bearer " + OWNER_API_KEY)
                .header("Content-Type", "application/json")
                .body(createBody(idempotencyKey))
                .exchange((request, response) -> {
                    assertThat(HttpStatus.valueOf(response.getStatusCode().value())).isEqualTo(expectedStatus);
                    return jsonMapper.readValue(response.getBody().readAllBytes(), PaymentResponse.class);
                });
    }

    private String createBody(String idempotencyKey) {
        return "{"
                + "\"agentId\":\"" + SOME_AGENT_ID + "\","
                + "\"idempotencyKey\":\"" + idempotencyKey + "\","
                + "\"recipientAddress\":\"" + SOME_RECIPIENT + "\","
                + "\"amount\":25.00,"
                + "\"currency\":\"USDC\","
                + "\"memo\":\"GPT-4 API credits\""
                + "}";
    }

    private long paymentCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM payment", Long.class);
    }
}
