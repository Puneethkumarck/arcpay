package com.arcpay.compliance;

import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.CheckResult;
import com.arcpay.compliance.domain.model.CheckType;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.model.ScreeningCheck;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.compliance.test.BusinessTest;
import com.github.f4b6a3.uuid.UuidCreator;
import com.github.tomakehurst.wiremock.WireMockServer;
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
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.compliance.domain.model.CheckResult.CLEAR;
import static com.arcpay.compliance.domain.model.CheckResult.MATCH;
import static com.arcpay.compliance.domain.model.CheckType.ONCHAIN_INTERACTION;
import static com.arcpay.compliance.domain.model.CheckType.ONCHAIN_MIXER;
import static com.arcpay.compliance.domain.model.CheckType.ONCHAIN_NOVELTY;
import static com.arcpay.compliance.domain.model.CheckType.SANCTIONS_OFAC;
import static com.arcpay.compliance.domain.model.CheckType.WATCHLIST;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAN_COUNTERPARTY;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static com.arcpay.compliance.fixtures.OnChainFixtures.blockNumberJson;
import static com.arcpay.compliance.fixtures.OnChainFixtures.transferLogsJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "compliance.sanctions.poll-interval-ms=500")
class ScreeningVerdictPathsTest extends BusinessTest {

    private static final long LATEST_BLOCK = 100000;

    private static WireMockServer rpcServer;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private SanctionsSetProvider sanctionsSetProvider;

    @Autowired
    private WatchlistStore watchlistStore;

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
                .withRequestBody(matchingJsonPath("$.method", equalTo("eth_blockNumber")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(blockNumberJson(LATEST_BLOCK))));
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", equalTo("eth_getLogs")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(transferLogsJson(SOME_RECIPIENT_ADDRESS, SOME_CLEAN_COUNTERPARTY))));
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldProducePassVerdictWithZeroRiskForCleanAddress() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_RECIPIENT_ADDRESS));

        // then
        var completed = awaitCompleted(paymentId);
        assertThat(completed).usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .ignoringFields("checks")
                .isEqualTo(ScreeningCompleted.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .verdict(Verdict.PASS)
                        .riskScore(0)
                        .listVersionId(versionId)
                        .screenedAt(Instant.EPOCH)
                        .build());
        assertThat(completed.checks())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("details")
                .containsExactlyInAnyOrderElementsOf(List.of(
                        check(WATCHLIST, CLEAR, 0),
                        check(ONCHAIN_INTERACTION, CLEAR, 0),
                        check(ONCHAIN_NOVELTY, CLEAR, 0),
                        check(ONCHAIN_MIXER, CLEAR, 0)));
        assertThat(verdictRow(paymentId)).isEqualTo(Verdict.PASS.name());
        assertThat(riskScoreRow(paymentId)).isZero();
        assertThat(screeningResultCount(paymentId)).isEqualTo(1);
        assertThat(holdReviewState(paymentId)).isNull();
    }

    @Test
    void shouldProduceBlockVerdictForSanctionedAddress() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_SANCTIONED_ADDRESS));

        // then
        var completed = awaitCompleted(paymentId);
        assertThat(completed).usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .ignoringFields("checks")
                .isEqualTo(ScreeningCompleted.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .verdict(Verdict.BLOCK)
                        .riskScore(100)
                        .listVersionId(versionId)
                        .screenedAt(Instant.EPOCH)
                        .build());
        assertThat(completed.checks())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("details")
                .containsExactly(check(SANCTIONS_OFAC, MATCH, 100));
        assertThat(listVersionRow(paymentId)).isEqualTo(versionId);
        assertThat(holdReviewState(paymentId)).isNull();
    }

    @Test
    void shouldProduceHoldVerdictAndPendingReviewForWatchlistAddress() {
        // given
        var versionId = seedSanctions(SOME_SANCTIONED_ADDRESS);
        awaitSanctionsLoaded(versionId);
        watchlistStore.addAddress(SOME_WATCHLIST_ADDRESS, "operator-flagged", "officer@arcpay.io");
        var paymentId = UuidCreator.getTimeOrderedEpoch();

        // when
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                requestFor(paymentId, SOME_WATCHLIST_ADDRESS));

        // then
        var completed = awaitCompleted(paymentId);
        assertThat(completed).usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .ignoringFields("checks")
                .isEqualTo(ScreeningCompleted.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .verdict(Verdict.HOLD)
                        .riskScore(100)
                        .listVersionId(versionId)
                        .screenedAt(Instant.EPOCH)
                        .build());
        assertThat(completed.checks())
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("details")
                .containsExactlyInAnyOrderElementsOf(List.of(
                        check(WATCHLIST, MATCH, 100),
                        check(ONCHAIN_INTERACTION, CLEAR, 0),
                        check(ONCHAIN_NOVELTY, CLEAR, 0),
                        check(ONCHAIN_MIXER, CLEAR, 0)));
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> assertThat(holdReviewState(paymentId))
                        .isEqualTo(ReviewState.PENDING.name()));
    }

    private ScreeningCheck check(CheckType type, CheckResult result, int matchScore) {
        return ScreeningCheck.builder()
                .type(type)
                .result(result)
                .matchScore(matchScore)
                .build();
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

    private ScreeningCompleted awaitCompleted(UUID paymentId) {
        try (var consumer = newCompletedConsumer()) {
            consumer.subscribe(List.of(ScreeningCompleted.TOPIC));
            var captured = new AtomicReference<ScreeningCompleted>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), ScreeningCompleted.class);
                            if (event.paymentId().equals(paymentId)) {
                                captured.set(event);
                                return true;
                            }
                        }
                        return false;
                    });
            return captured.get();
        }
    }

    private KafkaConsumer<String, String> newCompletedConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "verdict-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private String verdictRow(UUID paymentId) {
        var verdicts = jdbcTemplate.queryForList(
                "SELECT verdict FROM screening_result WHERE payment_id = ?", String.class, paymentId);
        return verdicts.isEmpty() ? null : verdicts.getFirst();
    }

    private Integer riskScoreRow(UUID paymentId) {
        var scores = jdbcTemplate.queryForList(
                "SELECT risk_score FROM screening_result WHERE payment_id = ?", Integer.class, paymentId);
        return scores.isEmpty() ? null : scores.getFirst();
    }

    private UUID listVersionRow(UUID paymentId) {
        var versions = jdbcTemplate.queryForList(
                "SELECT list_version_id FROM screening_result WHERE payment_id = ?", UUID.class, paymentId);
        return versions.isEmpty() ? null : versions.getFirst();
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
}
