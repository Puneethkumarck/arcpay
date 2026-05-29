package com.arcpay.compliance;

import com.arcpay.compliance.api.ErrorCodes;
import com.arcpay.compliance.application.dto.ReviewDecisionRequest;
import com.arcpay.compliance.domain.event.PaymentScreeningRequested;
import com.arcpay.compliance.domain.event.ScreeningApproved;
import com.arcpay.compliance.domain.event.ScreeningCompleted;
import com.arcpay.compliance.domain.event.ScreeningRejected;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_WATCHLIST_ADDRESS;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.awaitility.Awaitility.await;

@TestPropertySource(properties = "compliance.sanctions.poll-interval-ms=500")
class HoldReviewWorkflowTest extends BusinessTest {

    private static final String OFFICER_API_KEY = "officer-raw-key";
    private static final String OWNER_API_KEY = "owner-raw-key";
    private static final String OTHER_OWNER_API_KEY = "other-owner-raw-key";
    private static final UUID OTHER_OWNER_ID = UUID.fromString("0197aa00-cccc-7def-8000-cccccccccccc");

    private static WireMockServer identityServer;

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
    static void startIdentityStub() {
        identityServer = new WireMockServer(0);
        identityServer.start();
    }

    @AfterAll
    static void stopIdentityStub() {
        if (identityServer != null) {
            identityServer.stop();
        }
    }

    @DynamicPropertySource
    static void identityProperties(DynamicPropertyRegistry registry) {
        registry.add("arcpay.identity-service.url", () -> "http://localhost:" + identityServer.port());
    }

    @BeforeEach
    void resetState() {
        identityServer.resetAll();
        stubPrincipal(OFFICER_API_KEY, SOME_OWNER_ID, SOME_OFFICER_EMAIL, "COMPLIANCE_OFFICER");
        stubPrincipal(OWNER_API_KEY, SOME_OWNER_ID, SOME_OWNER_EMAIL, "OWNER");
        stubPrincipal(OTHER_OWNER_API_KEY, OTHER_OWNER_ID, "other-owner@arcpay.io", "OWNER");
        stubAgentOwner(SOME_AGENT_ID, SOME_OWNER_ID);
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    void shouldApproveHoldAsOfficerWithoutResolvingOwnerViaIdentity() {
        // given
        var paymentId = createHold();

        // when
        var response = restClient().post()
                .uri("/compliance/holds/{paymentId}/approve", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewDecisionRequest("Counterparty verified via off-chain KYC."))
                .retrieve()
                .toBodilessEntity();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(holdReviewState(paymentId)).isEqualTo(ReviewState.APPROVED.name());
        assertThat(reviewerRole(paymentId)).isEqualTo("COMPLIANCE_OFFICER");
        assertThat(awaitEvent(paymentId, ScreeningApproved.TOPIC, ScreeningApproved.class))
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(ScreeningApproved.builder()
                        .paymentId(paymentId)
                        .reviewer(SOME_OFFICER_EMAIL)
                        .reason("Counterparty verified via off-chain KYC.")
                        .decidedAt(Instant.EPOCH)
                        .build());
        identityServer.verify(0, getRequestedFor(urlPathEqualTo(
                "/api/v1/internal/agents/" + SOME_AGENT_ID)));
    }

    @Test
    void shouldApproveHoldAsOwnerResolvedViaIdentity() {
        // given
        var paymentId = createHold();

        // when
        var response = restClient().post()
                .uri("/compliance/holds/{paymentId}/approve", paymentId)
                .header("Authorization", "Bearer " + OWNER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewDecisionRequest("Owner confirms this is a legitimate vendor payout."))
                .retrieve()
                .toBodilessEntity();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(holdReviewState(paymentId)).isEqualTo(ReviewState.APPROVED.name());
        assertThat(reviewerRole(paymentId)).isEqualTo("OWNER");
        assertThat(awaitEvent(paymentId, ScreeningApproved.TOPIC, ScreeningApproved.class))
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(ScreeningApproved.builder()
                        .paymentId(paymentId)
                        .reviewer(SOME_OWNER_EMAIL)
                        .reason("Owner confirms this is a legitimate vendor payout.")
                        .decidedAt(Instant.EPOCH)
                        .build());
        identityServer.verify(getRequestedFor(urlPathEqualTo(
                "/api/v1/internal/agents/" + SOME_AGENT_ID)));
    }

    @Test
    void shouldRejectApprovalWhenPrincipalIsNeitherOwnerNorOfficer() {
        // given
        var paymentId = createHold();

        // when
        var error = catchThrowableOfType(HttpClientErrorException.class, () -> restClient().post()
                .uri("/compliance/holds/{paymentId}/approve", paymentId)
                .header("Authorization", "Bearer " + OTHER_OWNER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewDecisionRequest("Attempting to approve another owner's hold."))
                .retrieve()
                .toBodilessEntity());

        // then
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(apiError(error).code()).isEqualTo(ErrorCodes.NOT_AUTHORIZED);
        assertThat(holdReviewState(paymentId)).isEqualTo(ReviewState.PENDING.name());
    }

    @Test
    void shouldRejectHoldWithValidReasonAndPublishRejectedEvent() {
        // given
        var paymentId = createHold();

        // when
        var response = restClient().post()
                .uri("/compliance/holds/{paymentId}/reject", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewDecisionRequest("Recipient linked to flagged mixer."))
                .retrieve()
                .toBodilessEntity();

        // then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(holdReviewState(paymentId)).isEqualTo(ReviewState.REJECTED.name());
        assertThat(awaitEvent(paymentId, ScreeningRejected.TOPIC, ScreeningRejected.class))
                .usingRecursiveComparison()
                .ignoringFieldsOfTypes(Instant.class)
                .isEqualTo(ScreeningRejected.builder()
                        .paymentId(paymentId)
                        .reviewer(SOME_OFFICER_EMAIL)
                        .reason("Recipient linked to flagged mixer.")
                        .decidedAt(Instant.EPOCH)
                        .build());
    }

    @Test
    void shouldReturnBadRequestWhenReasonIsTooShort() {
        // given
        var paymentId = createHold();

        // when
        var error = catchThrowableOfType(HttpClientErrorException.class, () -> restClient().post()
                .uri("/compliance/holds/{paymentId}/approve", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"reason\":\"Short\"}")
                .retrieve()
                .toBodilessEntity());

        // then
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(apiError(error).code()).isEqualTo(ErrorCodes.REVIEW_REASON_INVALID);
    }

    @Test
    void shouldReturnConflictWhenHoldAlreadyDecided() {
        // given
        var paymentId = createHold();
        restClient().post()
                .uri("/compliance/holds/{paymentId}/approve", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewDecisionRequest("Counterparty verified via off-chain KYC."))
                .retrieve()
                .toBodilessEntity();

        // when
        var error = catchThrowableOfType(HttpClientErrorException.class, () -> restClient().post()
                .uri("/compliance/holds/{paymentId}/reject", paymentId)
                .header("Authorization", "Bearer " + OFFICER_API_KEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new ReviewDecisionRequest("Trying to reject an already-approved hold."))
                .retrieve()
                .toBodilessEntity());

        // then
        assertThat(error.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(apiError(error).code()).isEqualTo(ErrorCodes.HOLD_ALREADY_DECIDED);
    }

    private UUID createHold() {
        var versionId = seedSanctions();
        awaitSanctionsLoaded(versionId);
        watchlistStore.addAddress(SOME_WATCHLIST_ADDRESS, "operator-flagged", "officer@arcpay.io");
        var paymentId = UuidCreator.getTimeOrderedEpoch();
        kafkaTemplate.send(PaymentScreeningRequested.TOPIC, paymentId.toString(),
                PaymentScreeningRequested.builder()
                        .paymentId(paymentId)
                        .agentId(SOME_AGENT_ID)
                        .recipientAddress(SOME_WATCHLIST_ADDRESS)
                        .amount(new BigDecimal("100.00"))
                        .currency("USDC")
                        .requestedAt(Instant.now())
                        .build());
        awaitVerdict(paymentId);
        await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(200))
                .until(() -> ReviewState.PENDING.name().equals(holdReviewState(paymentId)));
        return paymentId;
    }

    private void stubPrincipal(String rawApiKey, UUID ownerId, String email, String authority) {
        var hash = ApiKeyAuthFilter.hashApiKey(rawApiKey);
        identityServer.stubFor(get(urlPathEqualTo("/api/v1/internal/owners/by-api-key-hash/" + hash))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "ownerId": "%s",
                                  "email": "%s",
                                  "authority": "%s"
                                }
                                """.formatted(ownerId, email, authority))));
    }

    private void stubAgentOwner(UUID agentId, UUID ownerId) {
        identityServer.stubFor(get(urlPathEqualTo("/api/v1/internal/agents/" + agentId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "agentId": "%s",
                                  "ownerId": "%s",
                                  "status": "ACTIVE",
                                  "policyHash": "0xabc123def456",
                                  "name": "test-agent",
                                  "createdAt": "2026-01-01T00:00:00Z"
                                }
                                """.formatted(agentId, ownerId))));
    }

    private void awaitVerdict(UUID paymentId) {
        try (var consumer = newConsumer("hold-verdict-probe")) {
            consumer.subscribe(List.of(ScreeningCompleted.TOPIC));
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            var event = jsonMapper.readValue(record.value(), ScreeningCompleted.class);
                            if (event.paymentId().equals(paymentId) && event.verdict() == Verdict.HOLD) {
                                return true;
                            }
                        }
                        return false;
                    });
        }
    }

    private <T> T awaitEvent(UUID paymentId, String topic, Class<T> eventType) {
        try (var consumer = newConsumer("hold-event-probe")) {
            consumer.subscribe(List.of(topic));
            var captured = new AtomicReference<T>();
            await().atMost(Duration.ofSeconds(30)).pollInterval(Duration.ofMillis(500))
                    .until(() -> {
                        for (var record : consumer.poll(Duration.ofSeconds(2))) {
                            if (paymentId.toString().equals(record.key())) {
                                captured.set(jsonMapper.readValue(record.value(), eventType));
                                return true;
                            }
                        }
                        return false;
                    });
            return captured.get();
        }
    }

    private KafkaConsumer<String, String> newConsumer(String group) {
        var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, group + "-" + UUID.randomUUID());
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

    private String reviewerRole(UUID paymentId) {
        var roles = jdbcTemplate.queryForList(
                "SELECT reviewer_role FROM hold_review WHERE payment_id = ?", String.class, paymentId);
        return roles.isEmpty() ? null : roles.getFirst();
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
        jdbcTemplate.update("DELETE FROM current_list_version WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO current_list_version (id, version_id, updated_at) VALUES (1, ?, now())",
                versionId);
        return versionId;
    }
}
