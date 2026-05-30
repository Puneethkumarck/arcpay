package com.arcpay.settlement.infrastructure.circle;

import com.arcpay.settlement.domain.InsufficientBalanceException;
import com.arcpay.settlement.domain.model.TransferCommand;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.CustodyProvider;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import com.arcpay.settlement.test.FullContextIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.arcpay.settlement.fixtures.CircleKeyFixtures.publicKeyPem;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_WALLET_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someTransferCommand;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubCreateTransfer;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubEntityPublicKey;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubWalletBalance;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CircleTransferAdapterIntegrationTest extends FullContextIntegrationTest {

    private static final String TRANSFER_PATH = "/v1/w3s/developer/transactions/transfer";
    private static final String SOME_CIRCLE_TX_ID = "circle-tx-int-001";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CustodyProvider custodyProvider;

    @Autowired
    private SettlementTransactionRepository repository;

    private WireMockServer circleServer;

    @BeforeEach
    void setUp() {
        circleServer = new WireMockServer(options().port(8089));
        circleServer.start();
        stubEntityPublicKey(circleServer, publicKeyPem());
    }

    @AfterEach
    void tearDown() {
        circleServer.stop();
    }

    @Test
    void shouldSubmitTransferPersistInitiatedAndRecordCircleTxId() {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "100.00");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");
        var command = commandWithRandomPaymentId();

        // when
        var submission = custodyProvider.submitTransfer(command);

        // then
        var persisted = repository.findByPaymentId(command.paymentId()).orElseThrow();
        assertThat(submission.circleTxId()).isEqualTo(SOME_CIRCLE_TX_ID);
        assertThat(persisted.state()).isEqualTo(TransferState.INITIATED);
        assertThat(persisted.circleTxId()).isEqualTo(SOME_CIRCLE_TX_ID);
    }

    @Test
    void shouldNotCallCircleAgainOnIdempotentReplay() {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "100.00");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");
        var command = commandWithRandomPaymentId();
        custodyProvider.submitTransfer(command);

        // when
        var replay = custodyProvider.submitTransfer(command);

        // then
        assertThat(replay.circleTxId()).isEqualTo(SOME_CIRCLE_TX_ID);
        circleServer.verify(1, postRequestedFor(urlPathEqualTo(TRANSFER_PATH)));
    }

    @Test
    void shouldRejectWhenBalanceBelowAmountPlusGasBuffer() {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "25.10");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");
        var command = commandWithRandomPaymentId();

        // when / then
        assertThatThrownBy(() -> custodyProvider.submitTransfer(command))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(repository.findByPaymentId(command.paymentId())).isEmpty();
        circleServer.verify(0, postRequestedFor(urlPathEqualTo(TRANSFER_PATH)));
    }

    @Test
    void shouldSendFreshEntitySecretCiphertextPerCall() {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "100.00");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");

        // when
        custodyProvider.submitTransfer(commandWithRandomPaymentId());
        custodyProvider.submitTransfer(commandWithRandomPaymentId());

        // then
        var ciphertexts = circleServer.findAll(postRequestedFor(urlPathEqualTo(TRANSFER_PATH))).stream()
                .map(request -> ciphertextOf(request.getBodyAsString()))
                .toList();
        assertThat(ciphertexts).hasSize(2);
        assertThat(ciphertexts).doesNotHaveDuplicates();
        assertThat(ciphertexts).allSatisfy(value -> assertThat(value).isNotBlank());
    }

    @Test
    void shouldSendCorrectTransferBodyWithDeterministicIdempotencyKey() throws Exception {
        // given
        stubWalletBalance(circleServer, SOME_WALLET_ID, "100.00");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");
        var command = commandWithRandomPaymentId();

        // when
        custodyProvider.submitTransfer(command);

        // then
        var body = objectMapper.readTree(
                circleServer.findAll(postRequestedFor(urlPathEqualTo(TRANSFER_PATH)))
                        .getFirst().getBodyAsString());
        assertThat(body.get("walletId").asText()).isEqualTo(SOME_WALLET_ID);
        assertThat(body.get("tokenAddress").asText())
                .isEqualTo("0x3600000000000000000000000000000000000000");
        assertThat(body.get("blockchain").asText()).isEqualTo("ARC-TESTNET");
        assertThat(body.get("feeLevel").asText()).isEqualTo("MEDIUM");
        assertThat(body.get("refId").asText()).isEqualTo(command.paymentId().toString());
        assertThat(body.get("idempotencyKey").asText())
                .isEqualTo(CircleTransferAdapter.idempotencyKey(command.paymentId()).toString());
        assertThat(amounts(body)).containsExactly("25.00");
    }

    private TransferCommand commandWithRandomPaymentId() {
        return someTransferCommand().toBuilder().paymentId(UUID.randomUUID()).build();
    }

    private List<String> amounts(JsonNode body) {
        var values = new ArrayList<String>();
        body.get("amounts").forEach(node -> values.add(node.asText()));
        return values;
    }

    private String ciphertextOf(String requestBody) {
        try {
            return objectMapper.readTree(requestBody).get("entitySecretCiphertext").asText();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
