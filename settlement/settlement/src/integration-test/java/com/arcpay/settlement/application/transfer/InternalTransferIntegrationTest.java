package com.arcpay.settlement.application.transfer;

import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import com.arcpay.settlement.test.RestControllerAbstractTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static com.arcpay.settlement.fixtures.CircleKeyFixtures.publicKeyPem;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_WALLET_ID;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubCreateTransfer;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubEntityPublicKey;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubWalletBalance;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalTransferIntegrationTest extends RestControllerAbstractTest {

    private static final String TRANSFER_PATH = "/v1/w3s/developer/transactions/transfer";
    private static final String SOME_CIRCLE_TX_ID = "circle-tx-submit-001";
    private static final String SERVICE_TOKEN = "test-service-token";

    @Autowired
    private SettlementTransactionRepository repository;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    private WireMockServer circleServer;

    @BeforeEach
    void setUp() {
        wipe();
        circleServer = new WireMockServer(options().port(8089));
        circleServer.start();
        stubEntityPublicKey(circleServer, publicKeyPem());
    }

    @AfterEach
    void tearDown() {
        circleServer.stop();
        wipe();
    }

    private void wipe() {
        jdbcTemplate.update("DELETE FROM settlement_outbox_record");
        jdbcTemplate.update("DELETE FROM settlement_transaction");
    }

    @Test
    void shouldSubmitTransferPersistInitiatedAndReturnCircleTxId() throws Exception {
        // given
        var paymentId = UUID.randomUUID();
        stubWalletBalance(circleServer, SOME_WALLET_ID, "100.00");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");

        // when
        postTransfer(transferBody(paymentId, "25.00"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.circleTxId").value(SOME_CIRCLE_TX_ID))
                .andExpect(jsonPath("$.state").value("INITIATED"));

        var persisted = repository.findByPaymentId(paymentId).orElseThrow();
        assertThat(persisted.circleTxId()).isEqualTo(SOME_CIRCLE_TX_ID);
        assertThat(persisted.state()).isEqualTo(TransferState.INITIATED);
        circleServer.verify(1, postRequestedFor(urlPathEqualTo(TRANSFER_PATH)));
    }

    @Test
    void shouldReturnExistingTransactionOnIdempotentReplayWithoutSecondCircleCall() throws Exception {
        // given
        var paymentId = UUID.randomUUID();
        stubWalletBalance(circleServer, SOME_WALLET_ID, "100.00");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");
        postTransfer(transferBody(paymentId, "25.00")).andExpect(status().isOk());

        // when
        postTransfer(transferBody(paymentId, "25.00"))
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(paymentId.toString()))
                .andExpect(jsonPath("$.circleTxId").value(SOME_CIRCLE_TX_ID))
                .andExpect(jsonPath("$.state").value("INITIATED"));

        circleServer.verify(1, postRequestedFor(urlPathEqualTo(TRANSFER_PATH)));
    }

    @Test
    void shouldReturnUnprocessableEntityWhenBalanceBelowAmountPlusGas() throws Exception {
        // given
        var paymentId = UUID.randomUUID();
        stubWalletBalance(circleServer, SOME_WALLET_ID, "25.10");
        stubCreateTransfer(circleServer, SOME_CIRCLE_TX_ID, "INITIATED");

        // when
        postTransfer(transferBody(paymentId, "25.00"))
                // then
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("ARCPAY-SETTLEMENT-0002"));

        assertThat(repository.findByPaymentId(paymentId)).isEmpty();
        circleServer.verify(0, postRequestedFor(urlPathEqualTo(TRANSFER_PATH)));
    }

    private org.springframework.test.web.servlet.ResultActions postTransfer(String body) throws Exception {
        return mockMvc.perform(post("/api/v1/internal/transfers")
                .header("X-Service-Auth", SERVICE_TOKEN)
                .contentType(APPLICATION_JSON)
                .content(body));
    }

    private String transferBody(UUID paymentId, String amount) {
        return """
                {
                  "paymentId": "%s",
                  "walletId": "%s",
                  "recipientAddress": "%s",
                  "amount": "%s",
                  "memo": "invoice-42"
                }
                """.formatted(paymentId, SOME_WALLET_ID, SOME_RECIPIENT_ADDRESS, amount);
    }
}
