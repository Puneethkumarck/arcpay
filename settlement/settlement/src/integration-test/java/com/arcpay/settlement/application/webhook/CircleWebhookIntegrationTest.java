package com.arcpay.settlement.application.webhook;

import com.arcpay.settlement.domain.event.TransferConfirmed;
import com.arcpay.settlement.domain.event.TransferReverted;
import com.arcpay.settlement.domain.model.TransferState;
import com.arcpay.settlement.domain.port.SettlementTransactionRepository;
import com.arcpay.settlement.test.RestControllerAbstractTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.settlement.fixtures.CircleKeyFixtures.SOME_KEY_ID;
import static com.arcpay.settlement.fixtures.CircleKeyFixtures.signWebhook;
import static com.arcpay.settlement.fixtures.CircleKeyFixtures.webhookPublicKeyPem;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_CIRCLE_TX_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.SOME_PAYMENT_ID;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.failedNotificationBody;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.notificationBody;
import static com.arcpay.settlement.fixtures.SettlementTransactionFixtures.someSettlementTransaction;
import static com.arcpay.settlement.test.stubs.CircleStubs.stubNotificationPublicKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
class CircleWebhookIntegrationTest extends RestControllerAbstractTest {

    @Autowired
    private SettlementTransactionRepository repository;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private WireMockServer circleServer;

    @BeforeEach
    void setUp() {
        wipe();
        circleServer = new WireMockServer(8089);
        circleServer.start();
        stubNotificationPublicKey(circleServer, SOME_KEY_ID, webhookPublicKeyPem());
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
    void shouldConfirmTransferAndPublishConfirmedEvent() throws Exception {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));
        var body = notificationBody(SOME_CIRCLE_TX_ID, "COMPLETE");

        // when
        postWebhook(body, signWebhook(body)).andExpect(status().isOk());

        // then
        assertThat(stateOf(SOME_PAYMENT_ID)).isEqualTo(TransferState.COMPLETED.name());
        var event = awaitEvent(TransferConfirmed.TOPIC, TransferConfirmed.class);
        assertThat(event.paymentId()).isEqualTo(SOME_PAYMENT_ID);
    }

    @Test
    void shouldRevertTransferAndPublishRevertedEventWhenFailed() throws Exception {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));
        var body = failedNotificationBody(SOME_CIRCLE_TX_ID, "FAILED");

        // when
        postWebhook(body, signWebhook(body)).andExpect(status().isOk());

        // then
        assertThat(stateOf(SOME_PAYMENT_ID)).isEqualTo(TransferState.FAILED.name());
        var event = awaitEvent(TransferReverted.TOPIC, TransferReverted.class);
        assertThat(event.paymentId()).isEqualTo(SOME_PAYMENT_ID);
    }

    @Test
    void shouldNotEmitWhenConfirmedNotificationReceived() throws Exception {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));
        var body = notificationBody(SOME_CIRCLE_TX_ID, "CONFIRMED");

        // when
        postWebhook(body, signWebhook(body)).andExpect(status().isOk());

        // then
        assertThat(stateOf(SOME_PAYMENT_ID)).isEqualTo(TransferState.CONFIRMED.name());
        assertThat(outboxCount()).isZero();
    }

    @Test
    void shouldBeNoOpForDuplicateTerminalNotification() throws Exception {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));
        var body = notificationBody(SOME_CIRCLE_TX_ID, "COMPLETE");
        postWebhook(body, signWebhook(body)).andExpect(status().isOk());
        awaitEvent(TransferConfirmed.TOPIC, TransferConfirmed.class);
        await().atMost(Duration.ofSeconds(10)).until(() -> publishedConfirmedCount() == 1);

        // when
        postWebhook(body, signWebhook(body)).andExpect(status().isOk());

        // then
        await().during(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10))
                .until(() -> publishedConfirmedCount() == 1);
        assertThat(stateOf(SOME_PAYMENT_ID)).isEqualTo(TransferState.COMPLETED.name());
    }

    @Test
    void shouldRejectInvalidSignatureWithoutStateChange() throws Exception {
        // given
        repository.save(someSettlementTransaction(TransferState.SENT));
        var body = notificationBody(SOME_CIRCLE_TX_ID, "COMPLETE");

        // when
        postWebhook(body, "aW52YWxpZC1zaWduYXR1cmU=").andExpect(status().isUnauthorized());

        // then
        assertThat(stateOf(SOME_PAYMENT_ID)).isEqualTo(TransferState.SENT.name());
        assertThat(outboxCount()).isZero();
    }

    private org.springframework.test.web.servlet.ResultActions postWebhook(String body, String signature)
            throws Exception {
        return mockMvc.perform(post("/api/v1/webhooks/circle")
                .header("X-Circle-Key-Id", SOME_KEY_ID)
                .header("X-Circle-Signature", signature)
                .contentType(APPLICATION_JSON)
                .content(body));
    }

    private String stateOf(UUID paymentId) {
        return repository.findByPaymentId(paymentId).orElseThrow().state().name();
    }

    private long outboxCount() {
        var count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_outbox_record", Long.class);
        return count == null ? 0 : count;
    }

    private long publishedConfirmedCount() {
        var count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM settlement_outbox_record "
                        + "WHERE record_type LIKE ? AND record_key = ?",
                Long.class, "%" + TransferConfirmed.class.getSimpleName(), SOME_PAYMENT_ID.toString());
        return count == null ? 0 : count;
    }

    private <T> T awaitEvent(String topic, Class<T> eventType) {
        try (var consumer = newConsumer()) {
            consumer.subscribe(List.of(topic));
            var captured = new AtomicReference<T>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            if (SOME_PAYMENT_ID.toString().equals(record.key())) {
                                captured.set(jsonMapper.readValue(record.value(), eventType));
                                return true;
                            }
                        }
                        return false;
                    });
            return captured.get();
        }
    }

    private KafkaConsumer<String, String> newConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "settlement-webhook-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }
}
