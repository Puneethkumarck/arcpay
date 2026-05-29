package com.arcpay.compliance.application.stream;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.compliance.test.FullContextIntegrationTest;
import com.github.f4b6a3.uuid.UuidCreator;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static com.arcpay.compliance.fixtures.OnChainFixtures.blockNumberJson;
import static com.arcpay.compliance.fixtures.OnChainFixtures.emptyLogsJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = {
        "compliance.sanctions.poll-interval-ms=500"
})
class ScreeningRequestedListenerIntegrationTest extends FullContextIntegrationTest {

    private static final long LATEST_BLOCK = 100000;

    private static WireMockServer rpcServer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private SanctionsSetProvider sanctionsSetProvider;

    @Autowired
    private WatchlistStore watchlistStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JsonMapper jsonMapper;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @BeforeAll
    static void startRpcStub() {
        rpcServer = new WireMockServer(0);
        rpcServer.start();
    }

    @AfterAll
    static void stopRpcStub() {
        if (rpcServer != null) {
            rpcServer.stop();
        }
    }

    @DynamicPropertySource
    static void onChainProperties(DynamicPropertyRegistry registry) {
        registry.add("compliance.onchain.rpc-url", () -> "http://localhost:" + rpcServer.port());
    }

    @BeforeEach
    void resetState() {
        rpcServer.resetAll();
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_blockNumber")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(blockNumberJson(LATEST_BLOCK))));
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", WireMock.equalTo("eth_getLogs")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(emptyLogsJson())));
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldConsumeScreeningRequestedAndPublishCompleted() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_RECIPIENT_ADDRESS));

        // then
        assertThat(awaitVerdict(paymentId)).isEqualTo(Verdict.PASS);
        assertThat(screeningResultCount(paymentId)).isEqualTo(1);
    }

    @Test
    void shouldIdempotentlyPublishExistingVerdict() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_RECIPIENT_ADDRESS));
        awaitVerdict(paymentId);
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_RECIPIENT_ADDRESS));

        // then
        awaitCompletedCount(paymentId, 2);
        assertThat(screeningResultCount(paymentId)).isEqualTo(1);
    }

    @Test
    void shouldCreateHoldReviewForHoldVerdict() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        watchlistStore.addAddress(SOME_WATCHLIST_ADDRESS, "operator-flagged", "officer@arcpay.io");
        var paymentId = UuidCreator.getTimeOrderedEpoch();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_WATCHLIST_ADDRESS));

        // then
        assertThat(awaitVerdict(paymentId)).isEqualTo(Verdict.HOLD);
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(holdReviewState(paymentId)).isEqualTo(ReviewState.PENDING.name()));
    }

    private PaymentScreeningRequested requestFor(UUID paymentId, String recipientAddress) {
        return PaymentScreeningRequested.builder()
                .paymentId(paymentId)
                .agentId(SOME_AGENT_ID)
                .recipientAddress(recipientAddress)
                .amount(new BigDecimal("100.00"))
                .currency("USDC")
                .requestedAt(Instant.now())
                .build();
    }

    private Verdict awaitVerdict(UUID paymentId) {
        try (var consumer = newCompletedConsumer()) {
            consumer.subscribe(List.of(ScreeningCompleted.TOPIC));
            var verdict = new AtomicReference<Verdict>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), ScreeningCompleted.class);
                            if (event.paymentId().equals(paymentId)) {
                                verdict.set(event.verdict());
                                return true;
                            }
                        }
                        return false;
                    });
            return verdict.get();
        }
    }

    private void awaitCompletedCount(UUID paymentId, int min) {
        try (var consumer = newCompletedConsumer()) {
            consumer.subscribe(List.of(ScreeningCompleted.TOPIC));
            var count = new AtomicInteger();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), ScreeningCompleted.class);
                            if (event.paymentId().equals(paymentId)) {
                                count.incrementAndGet();
                            }
                        }
                        return count.get() >= min;
                    });
        }
    }

    private KafkaConsumer<String, String> newCompletedConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "completed-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private int screeningResultCount(UUID paymentId) {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM screening_result WHERE payment_id = ?", Integer.class, paymentId);
    }

    private String holdReviewState(UUID paymentId) {
        var states = jdbcTemplate.queryForList(
                "SELECT state FROM hold_review WHERE payment_id = ?", String.class, paymentId);
        return states.isEmpty() ? null : states.getFirst();
    }

    private void awaitSanctionsLoaded(UUID versionId) {
        await().atMost(Duration.ofSeconds(10))
                .until(() -> versionId.equals(sanctionsSetProvider.getCurrentSanctionsSet().versionId()));
    }

    private UUID seedSanctions(String address) {
        var versionId = UuidCreator.getTimeOrderedEpoch();
        jdbcTemplate.update(
                "INSERT INTO sanctions_list_version "
                        + "(version_id, source, downloaded_at, record_count, checksum, status) "
                        + "VALUES (?, ?, now(), ?, ?, 'ACTIVE')",
                versionId, "OFAC_SDN", 1, "checksum");
        jdbcTemplate.update(
                "INSERT INTO sanctioned_address (id, version_id, address, source) VALUES (?, ?, ?, ?)",
                UuidCreator.getTimeOrderedEpoch(), versionId, address, "OFAC_SDN");
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
        return versionId;
    }

    private void cleanDatabase() {
        jdbcTemplate.update("DELETE FROM compliance_outbox_record");
        jdbcTemplate.update("DELETE FROM screening_check");
        jdbcTemplate.update("DELETE FROM hold_review");
        jdbcTemplate.update("DELETE FROM screening_result");
        jdbcTemplate.update("DELETE FROM sanctioned_address");
        jdbcTemplate.update("DELETE FROM current_list_version");
        jdbcTemplate.update("DELETE FROM sanctions_list_version");
        jdbcTemplate.update("DELETE FROM watchlist_address");
    }
}
