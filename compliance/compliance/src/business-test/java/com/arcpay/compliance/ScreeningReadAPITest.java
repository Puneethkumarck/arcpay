package com.arcpay.compliance;

import com.arcpay.compliance.api.ErrorCodes;
import com.arcpay.compliance.application.controller.ScreeningCheckResponse;
import com.arcpay.compliance.application.controller.ScreeningQueryResponse;
import com.arcpay.compliance.application.dto.HoldReviewResponse;
import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.model.ReviewState;
import com.arcpay.compliance.domain.model.Verdict;
import com.arcpay.compliance.domain.port.SanctionsSetProvider;
import com.arcpay.compliance.domain.port.WatchlistStore;
import com.arcpay.compliance.test.BusinessTest;
import com.arcpay.platform.api.ApiError;
import com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_CLEAN_COUNTERPARTY;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_RECIPIENT_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_SANCTIONED_ADDRESS;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.compliance.fixtures.OnChainFixtures.blockNumberJson;
import static com.arcpay.compliance.fixtures.OnChainFixtures.transferLogsJson;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "compliance.sanctions.poll-interval-ms=500")
class ScreeningReadAPITest extends BusinessTest {

    private static final String OFFICER_API_KEY = "officer-read-key";
    private static final long LATEST_BLOCK = 100000;

    private static WireMockServer identityServer;
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
    static void startStubs() {
        identityServer = new WireMockServer(0);
        identityServer.start();
        rpcServer = new WireMockServer(0);
        rpcServer.start();
    }

    @AfterAll
    static void stopStubs() {
        if (identityServer != null) {
            identityServer.stop();
        }
        if (rpcServer != null) {
            rpcServer.stop();
        }
    }

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("arcpay.identity-service.url", () -> "http://localhost:" + identityServer.port());
        registry.add("compliance.onchain.rpc-url", () -> "http://localhost:" + rpcServer.port());
    }

    @BeforeEach
    void resetState() {
        identityServer.resetAll();
        var hash = ApiKeyAuthFilter.hashApiKey(OFFICER_API_KEY);
        identityServer.stubFor(get(urlPathEqualTo("/api/v1/internal/owners/by-api-key-hash/" + hash))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "ownerId": "%s",
                                  "email": "%s",
                                  "authority": "COMPLIANCE_OFFICER"
                                }
                                """.formatted(SOME_OWNER_ID, SOME_OFFICER_EMAIL))));
        rpcServer.resetAll();
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", com.github.tomakehurst.wiremock.client.WireMock.equalTo("eth_blockNumber")))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(blockNumberJson(LATEST_BLOCK))));
        rpcServer.stubFor(post(urlEqualTo("/"))
                .withRequestBody(matchingJsonPath("$.method", com.github.tomakehurst.wiremock.client.WireMock.equalTo("eth_getLogs")))
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
    void shouldReturnScreeningMatchingSpecSchemaForCleanPass() {
        // given
        var versionId = seedSanctions();
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();
        publishScreening(paymentId, SOME_RECIPIENT_ADDRESS);
        var completed = awaitVerdict(paymentId, Verdict.PASS);

        // when
        var actual = restClient().get()
                .uri("/compliance/screenings/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .retrieve()
                .body(ScreeningQueryResponse.class);

        // then
        assertThat(actual).usingRecursiveComparison()
                .ignoringFields("screeningId", "timestamp", "durationMs", "checks")
                .isEqualTo(ScreeningQueryResponse.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .recipientAddress(SOME_RECIPIENT_ADDRESS)
                        .verdict("PASS")
                        .riskScore(0)
                        .build());
        assertThat(actual.checks()).containsExactlyInAnyOrderElementsOf(
                completed.checks().stream().map(this::toCheckResponse).toList());
    }

    @Test
    void shouldReturnNotFoundWhenScreeningAbsent() {
        // given
        var missing = UuidCreator.getTimeOrderedEpoch();

        // when
        var error = catchThrowableOfType(HttpClientErrorException.class, () -> restClient().get()
                .uri("/compliance/screenings/{paymentId}", missing)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .retrieve()
                .toBodilessEntity());

        // then
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(apiError(error).code()).isEqualTo(ErrorCodes.SCREENING_NOT_FOUND);
    }

    @Test
    void shouldReturnPendingHoldQueueAndFilterByState() {
        // given
        var versionId = seedSanctions();
        awaitSanctionsLoaded(versionId);
        watchlistStore.addAddress(SOME_WATCHLIST_ADDRESS, "operator-flagged", "officer@arcpay.io");
        for (var i = 0; i < 3; i++) {
            var paymentId = UuidCreator.getTimeOrderedEpoch();
            publishScreening(paymentId, SOME_WATCHLIST_ADDRESS);
            awaitVerdict(paymentId, Verdict.HOLD);
            awaitHoldPending(paymentId);
        }

        // when
        var pending = restClient().get()
                .uri("/compliance/holds?state=PENDING")
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .retrieve()
                .body(new ParameterizedTypeReference<List<HoldReviewResponse>>() {});
        var approved = restClient().get()
                .uri("/compliance/holds?state=APPROVED")
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .retrieve()
                .body(new ParameterizedTypeReference<List<HoldReviewResponse>>() {});

        // then
        assertThat(pending).hasSize(3).allMatch(hold -> hold.state() == ReviewState.PENDING);
        assertThat(approved).isEmpty();
    }

    @Test
    void shouldReturnHoldByPaymentId() {
        // given
        var versionId = seedSanctions();
        awaitSanctionsLoaded(versionId);
        watchlistStore.addAddress(SOME_WATCHLIST_ADDRESS, "operator-flagged", "officer@arcpay.io");
        var paymentId = UuidCreator.getTimeOrderedEpoch();
        publishScreening(paymentId, SOME_WATCHLIST_ADDRESS);
        awaitVerdict(paymentId, Verdict.HOLD);
        awaitHoldPending(paymentId);

        // when
        var hold = restClient().get()
                .uri("/compliance/holds/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .retrieve()
                .body(HoldReviewResponse.class);

        // then
        assertThat(hold).usingRecursiveComparison()
                .ignoringFields("reviewId", "screeningId", "createdAt", "decidedAt")
                .isEqualTo(HoldReviewResponse.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .state(ReviewState.PENDING)
                        .build());
    }

    @Test
    void shouldReturnNotFoundForHoldWhenPaymentHasNoHold() {
        // given
        var versionId = seedSanctions();
        awaitSanctionsLoaded(versionId);
        var paymentId = UuidCreator.getTimeOrderedEpoch();
        publishScreening(paymentId, SOME_RECIPIENT_ADDRESS);
        awaitVerdict(paymentId, Verdict.PASS);

        // when
        var error = catchThrowableOfType(HttpClientErrorException.class, () -> restClient().get()
                .uri("/compliance/holds/{paymentId}", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .retrieve()
                .toBodilessEntity());

        // then
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(apiError(error).code()).isEqualTo(ErrorCodes.HOLD_NOT_FOUND);
    }

    private ScreeningCheckResponse toCheckResponse(com.arcpay.compliance.domain.model.ScreeningCheck check) {
        return ScreeningCheckResponse.builder()
                .type(check.type().name())
                .result(check.result().name())
                .matchScore(check.matchScore())
                .details(jsonMapper.convertValue(check.details(),
                        new tools.jackson.core.type.TypeReference<Map<String, Object>>() {}))
                .build();
    }

    private void publishScreening(UUID paymentId, String recipientAddress) {
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                PaymentScreeningRequested.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .recipientAddress(recipientAddress)
                        .amount(new BigDecimal("100.00"))
                        .currency("USDC")
                        .requestedAt(Instant.now())
                        .build());
    }

    private ScreeningCompleted awaitVerdict(UUID paymentId, Verdict verdict) {
        try (var consumer = newConsumer()) {
            consumer.subscribe(List.of(ScreeningCompleted.TOPIC));
            var captured = new AtomicReference<ScreeningCompleted>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), ScreeningCompleted.class);
                            if (event.paymentId().equals(paymentId) && event.verdict() == verdict) {
                                captured.set(event);
                                return true;
                            }
                        }
                        return false;
                    });
            return captured.get();
        }
    }

    private void awaitHoldPending(UUID paymentId) {
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200))
                .until(() -> ReviewState.PENDING.name().equals(holdReviewState(paymentId)));
    }

    private KafkaConsumer<String, String> newConsumer() {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "read-api-probe-" + UUID.randomUUID());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new KafkaConsumer<>(props);
    }

    private ApiError apiError(HttpClientErrorException error) {
        return jsonMapper.readValue(error.getResponseBodyAsString(), ApiError.class);
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

    private UUID seedSanctions() {
        var versionId = UuidCreator.getTimeOrderedEpoch();
        jdbcTemplate.update(
                "INSERT INTO sanctions_list_version "
                        + "(version_id, source, downloaded_at, record_count, checksum, status) "
                        + "VALUES (?, ?, now(), ?, ?, 'ACTIVE')",
                versionId, "OFAC_SDN", 1, "checksum");
        jdbcTemplate.update(
                "INSERT INTO sanctioned_address (id, version_id, address, source) VALUES (?, ?, ?, ?)",
                UuidCreator.getTimeOrderedEpoch(), versionId, SOME_SANCTIONED_ADDRESS, "OFAC_SDN");
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
        return versionId;
    }
}
