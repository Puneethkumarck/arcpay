package com.arcpay.policy.policyengine;

import com.arcpay.platform.api.ApiError;
import com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter;
import com.arcpay.policy.policyengine.api.model.PolicyEvaluationResponse;
import com.arcpay.policy.policyengine.api.model.PolicyResponse;
import com.arcpay.policy.policyengine.api.model.SpendingSummaryResponse;
import com.arcpay.policy.policyengine.test.BusinessTest;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineBusinessTest extends BusinessTest {

    private static final String RECIPIENT = "0x1234567890abcdef1234567890abcdef12345678";
    private static final String BLOCKED_RECIPIENT = "0x000000000000000000000000000000000000dead";

    @BeforeEach
    void setUp() {
        cleanDatabase();
        identityService().resetAll();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
        identityService().resetAll();
    }

    @Test
    void shouldCreatePolicyWithMultipleRuleTypes() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xunset");
        stubUpdatePolicy(agentId);

        // when
        var response = createPolicy(apiKey, agentId, """
                {"rules": [
                  {"type": "DAILY_LIMIT", "amount": 1000.00},
                  {"type": "PER_TX_LIMIT", "amount": 100.00},
                  {"type": "RECIPIENT_ALLOWLIST", "addresses": ["%s"]}
                ]}
                """.formatted(RECIPIENT));

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var body = response.getBody();
        assertThat(body.agentId()).isEqualTo(agentId);
        assertThat(body.version()).isEqualTo(1);
        assertThat(body.status()).isEqualTo("ACTIVE");
        assertThat(body.rules()).hasSize(3);
        assertThat(body.policyHash()).startsWith("0x");
    }

    @Test
    void shouldComputeDeterministicPolicyHash() {
        // given
        var first = createSimplePolicy("""
                {"rules": [
                  {"type": "DAILY_LIMIT", "amount": 1000.00},
                  {"type": "PER_TX_LIMIT", "amount": 100.00}
                ]}
                """);
        var second = createSimplePolicy("""
                {"rules": [
                  {"type": "DAILY_LIMIT", "amount": 1000.00},
                  {"type": "PER_TX_LIMIT", "amount": 100.00}
                ]}
                """);
        var different = createSimplePolicy("""
                {"rules": [
                  {"type": "DAILY_LIMIT", "amount": 2000.00},
                  {"type": "PER_TX_LIMIT", "amount": 100.00}
                ]}
                """);

        // when
        // then
        assertThat(first.policyHash()).isEqualTo(second.policyHash());
        assertThat(first.policyHash()).isNotEqualTo(different.policyHash());
    }

    @Test
    void shouldComputeSameHashRegardlessOfRuleOrder() {
        // given
        var ordered = createSimplePolicy("""
                {"rules": [
                  {"type": "PER_TX_LIMIT", "amount": 100.00},
                  {"type": "DAILY_LIMIT", "amount": 1000.00}
                ]}
                """);
        var reversed = createSimplePolicy("""
                {"rules": [
                  {"type": "DAILY_LIMIT", "amount": 1000.00},
                  {"type": "PER_TX_LIMIT", "amount": 100.00}
                ]}
                """);

        // when
        // then
        assertThat(ordered.policyHash()).isEqualTo(reversed.policyHash());
    }

    @Test
    void shouldSyncPolicyHashToIdentityService() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xunset");
        stubUpdatePolicy(agentId);

        // when
        var response = createPolicy(apiKey, agentId, """
                {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                """);

        // then
        var policyHash = response.getBody().policyHash();
        identityService().verify(WireMock.putRequestedFor(
                        urlPathEqualTo("/api/v1/internal/agents/" + agentId + "/policy"))
                .withRequestBody(WireMock.matchingJsonPath("$.policyHash", WireMock.equalTo(policyHash))));
    }

    @Test
    void shouldCreateNewVersionAndSupersedeOld() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xunset");
        stubUpdatePolicy(agentId);

        var v1 = createPolicy(apiKey, agentId, """
                {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                """).getBody();

        // when
        var v2 = createPolicy(apiKey, agentId, """
                {"rules": [{"type": "PER_TX_LIMIT", "amount": 50.00}]}
                """).getBody();

        // then
        assertThat(v2.version()).isEqualTo(2);
        assertThat(v2.status()).isEqualTo("ACTIVE");

        var v1Reloaded = restClient().get()
                .uri("/api/v1/agents/{agentId}/policies/{policyId}", agentId, v1.policyId())
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(PolicyResponse.class);
        assertThat(v1Reloaded.status()).isEqualTo("SUPERSEDED");

        var active = restClient().get()
                .uri("/api/v1/agents/{agentId}/policies/active", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(PolicyResponse.class);
        assertThat(active.policyId()).isEqualTo(v2.policyId());
    }

    @Test
    void shouldRejectInvalidRuleCombinations() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xunset");

        // when
        var status = restClient().post()
                .uri("/api/v1/agents/{agentId}/policies", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"rules": [
                          {"type": "DAILY_LIMIT", "amount": 1000.00},
                          {"type": "WEEKLY_LIMIT", "amount": 500.00}
                        ]}
                        """)
                .exchange((req, resp) -> resp.getStatusCode().value());

        // then
        assertThat(status).isEqualTo(400);
    }

    @Test
    void shouldRejectPolicyCreationForNonOwnedAgent() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var otherOwnerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, otherOwnerId, "ACTIVE", "0xunset");

        // when
        var status = restClient().post()
                .uri("/api/v1/agents/{agentId}/policies", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                        """)
                .exchange((req, resp) -> resp.getStatusCode().value());

        // then
        assertThat(status).isEqualTo(403);
    }

    @Test
    void shouldRejectPolicyCreationForSuspendedAgent() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "SUSPENDED", "0xunset");

        // when
        var status = restClient().post()
                .uri("/api/v1/agents/{agentId}/policies", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                        """)
                .exchange((req, resp) -> resp.getStatusCode().value());

        // then
        assertThat(status).isEqualTo(422);
    }

    @Test
    void shouldReturn503WhenIdentityServiceUnavailable() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        identityService().stubFor(WireMock.get(urlPathEqualTo("/api/v1/internal/agents/" + agentId))
                .willReturn(aResponse().withStatus(500)));

        // when
        var result = restClient().post()
                .uri("/api/v1/agents/{agentId}/policies", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                        """)
                .exchange((req, resp) -> new ErrorResult(
                        resp.getStatusCode().value(), resp.bodyTo(ApiError.class)));

        // then
        assertThat(result.status()).isEqualTo(503);
        assertThat(result.error().code()).isEqualTo("ARCPAY-POLICY-0008");
    }

    @Test
    void shouldApprovePaymentWithinAllLimits() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [
                  {"type": "DAILY_LIMIT", "amount": 1000.00},
                  {"type": "PER_TX_LIMIT", "amount": 100.00}
                ]}
                """);

        // when
        var verdict = dryRun(ctx, RECIPIENT, "50.00");

        // then
        assertThat(verdict.verdict()).isEqualTo("APPROVED");
        assertThat(verdict.dryRun()).isTrue();
    }

    @Test
    void shouldRejectPaymentExceedingPerTransactionLimit() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                """);

        // when
        var verdict = dryRun(ctx, RECIPIENT, "150.00");

        // then
        assertThat(verdict.verdict()).isEqualTo("REJECTED");
        assertThat(verdict.ruleResults()).anyMatch(r -> r.ruleType().equals("PER_TX_LIMIT"));
    }

    @Test
    void shouldReturnRequiresApprovalForLargeAmount() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "APPROVAL_THRESHOLD", "amount": 500.00}]}
                """);

        // when
        var verdict = dryRun(ctx, RECIPIENT, "750.00");

        // then
        assertThat(verdict.verdict()).isEqualTo("REQUIRES_APPROVAL");
    }

    @Test
    void shouldRejectBlocklistedRecipient() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "RECIPIENT_BLOCKLIST", "addresses": ["%s"]}]}
                """.formatted(BLOCKED_RECIPIENT));

        // when
        var verdict = dryRun(ctx, BLOCKED_RECIPIENT, "10.00");

        // then
        assertThat(verdict.verdict()).isEqualTo("REJECTED");
        assertThat(verdict.ruleResults()).anyMatch(r -> r.ruleType().equals("RECIPIENT_BLOCKLIST"));
    }

    @Test
    void shouldRejectDryRunForNonOwnedAgent() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var otherOwnerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, otherOwnerId, "ACTIVE", "0xhash");

        // when
        var status = restClient().post()
                .uri("/api/v1/policies/evaluate")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(evaluateBody(agentId, RECIPIENT, "10.00"))
                .exchange((req, resp) -> resp.getStatusCode().value());

        // then
        assertThat(status).isEqualTo(403);
    }

    @Test
    void shouldRejectWhenAgentHasNoPolicy() {
        // given
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xhash");

        // when
        var verdict = restClient().post()
                .uri("/api/v1/policies/evaluate")
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(evaluateBody(agentId, RECIPIENT, "10.00"))
                .retrieve()
                .body(PolicyEvaluationResponse.class);

        // then
        assertThat(verdict.verdict()).isEqualTo("REJECTED");
        assertThat(verdict.ruleResults()).anyMatch(r -> r.ruleType().equals("NO_ACTIVE_POLICY"));
    }

    @Test
    void shouldRejectPaymentExceedingDailyLimitAfterRecordingSpending() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "DAILY_LIMIT", "amount": 100.00}]}
                """);
        recordSpending(ctx.agentId(), UUID.randomUUID(), "80.00");

        // when
        var verdict = internalEvaluate(ctx.agentId(), RECIPIENT, "30.00");

        // then
        assertThat(verdict.verdict()).isEqualTo("REJECTED");
        assertThat(verdict.dryRun()).isFalse();
        assertThat(verdict.ruleResults()).anyMatch(r -> r.ruleType().equals("DAILY_LIMIT"));
    }

    @Test
    void shouldReflectRecordedSpendingInSpendingSummary() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "DAILY_LIMIT", "amount": 1000.00}]}
                """);
        recordSpending(ctx.agentId(), UUID.randomUUID(), "40.00");
        recordSpending(ctx.agentId(), UUID.randomUUID(), "60.00");

        // when
        var summary = restClient().get()
                .uri("/api/v1/internal/agents/{agentId}/spending-summary", ctx.agentId())
                .header("X-Service-Auth", SERVICE_TOKEN)
                .retrieve()
                .body(SpendingSummaryResponse.class);

        // then
        assertThat(summary.agentId()).isEqualTo(ctx.agentId());
        assertThat(summary.dailyTotal()).isEqualByComparingTo("100.00");
        assertThat(summary.transactionCount24h()).isEqualTo(2);
    }

    @Test
    void shouldHandleDuplicateSpendingRecording() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "DAILY_LIMIT", "amount": 1000.00}]}
                """);
        var paymentId = UUID.randomUUID();

        // when
        recordSpending(ctx.agentId(), paymentId, "40.00");
        recordSpending(ctx.agentId(), paymentId, "40.00");

        // then
        var summary = restClient().get()
                .uri("/api/v1/internal/agents/{agentId}/spending-summary", ctx.agentId())
                .header("X-Service-Auth", SERVICE_TOKEN)
                .retrieve()
                .body(SpendingSummaryResponse.class);
        assertThat(summary.dailyTotal()).isEqualByComparingTo("40.00");
        assertThat(summary.transactionCount24h()).isEqualTo(1);
    }

    @Test
    void shouldNotResetSpendingCountersOnPolicyUpdate() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "DAILY_LIMIT", "amount": 1000.00}]}
                """);
        recordSpending(ctx.agentId(), UUID.randomUUID(), "80.00");

        // when
        var v2 = createPolicy(ctx.apiKey(), ctx.agentId(), """
                {"rules": [{"type": "DAILY_LIMIT", "amount": 100.00}]}
                """).getBody();
        stubAgent(ctx.agentId(), ctx.ownerId(), "ACTIVE", v2.policyHash());
        var verdict = internalEvaluate(ctx.agentId(), RECIPIENT, "30.00");

        // then
        assertThat(verdict.verdict()).isEqualTo("REJECTED");
        assertThat(verdict.ruleResults()).anyMatch(r -> r.ruleType().equals("DAILY_LIMIT"));
    }

    @Test
    void shouldRequireServiceAuthForInternalEvaluation() {
        // given
        var ctx = provisionAgentWithPolicy("""
                {"rules": [{"type": "PER_TX_LIMIT", "amount": 100.00}]}
                """);

        // when
        var status = restClient().post()
                .uri("/api/v1/internal/policies/evaluate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(internalEvaluateBody(ctx.agentId(), RECIPIENT, "10.00"))
                .exchange((req, resp) -> resp.getStatusCode().value());

        // then
        assertThat(status).isEqualTo(401);
    }

    private AgentContext provisionAgentWithPolicy(String rulesJson) {
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xunset");
        stubUpdatePolicy(agentId);
        var policy = createPolicy(apiKey, agentId, rulesJson).getBody();
        stubAgent(agentId, ownerId, "ACTIVE", policy.policyHash());
        return new AgentContext(agentId, ownerId, apiKey, policy.policyHash());
    }

    private PolicyResponse createSimplePolicy(String rulesJson) {
        var agentId = UUID.randomUUID();
        var ownerId = UUID.randomUUID();
        var apiKey = stubOwner(ownerId);
        stubAgent(agentId, ownerId, "ACTIVE", "0xunset");
        stubUpdatePolicy(agentId);
        return createPolicy(apiKey, agentId, rulesJson).getBody();
    }

    private org.springframework.http.ResponseEntity<PolicyResponse> createPolicy(
            String apiKey, UUID agentId, String rulesJson) {
        return restClient().post()
                .uri("/api/v1/agents/{agentId}/policies", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(rulesJson)
                .retrieve()
                .toEntity(PolicyResponse.class);
    }

    private PolicyEvaluationResponse dryRun(AgentContext ctx, String recipient, String amount) {
        return restClient().post()
                .uri("/api/v1/policies/evaluate")
                .header("Authorization", "Bearer " + ctx.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(evaluateBody(ctx.agentId(), recipient, amount))
                .retrieve()
                .body(PolicyEvaluationResponse.class);
    }

    private PolicyEvaluationResponse internalEvaluate(UUID agentId, String recipient, String amount) {
        return restClient().post()
                .uri("/api/v1/internal/policies/evaluate")
                .header("X-Service-Auth", SERVICE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(internalEvaluateBody(agentId, recipient, amount))
                .retrieve()
                .body(PolicyEvaluationResponse.class);
    }

    private void recordSpending(UUID agentId, UUID paymentId, String amount) {
        restClient().post()
                .uri("/api/v1/internal/policies/reservations")
                .header("X-Service-Auth", SERVICE_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "paymentId", paymentId.toString(),
                        "agentId", agentId.toString(),
                        "recipientAddress", RECIPIENT,
                        "amount", amount,
                        "requestedAt", Instant.now().toString()))
                .retrieve()
                .toBodilessEntity();
        restClient().post()
                .uri("/api/v1/internal/policies/reservations/{paymentId}/commit", paymentId)
                .header("X-Service-Auth", SERVICE_TOKEN)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> evaluateBody(UUID agentId, String recipient, String amount) {
        return Map.of(
                "agentId", agentId.toString(),
                "recipientAddress", recipient,
                "amount", amount);
    }

    private Map<String, Object> internalEvaluateBody(UUID agentId, String recipient, String amount) {
        return Map.of(
                "agentId", agentId.toString(),
                "recipientAddress", recipient,
                "amount", amount,
                "requestedAt", Instant.now().toString());
    }

    private String stubOwner(UUID ownerId) {
        var apiKey = "biz-key-" + UUID.randomUUID();
        var hash = ApiKeyAuthFilter.hashApiKey(apiKey);
        identityService().stubFor(WireMock.get(urlPathEqualTo(
                        "/api/v1/internal/owners/by-api-key-hash/" + hash))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"ownerId": "%s", "email": "owner@example.com"}
                                """.formatted(ownerId))));
        return apiKey;
    }

    private void stubAgent(UUID agentId, UUID ownerId, String status, String policyHash) {
        identityService().stubFor(WireMock.get(urlPathEqualTo("/api/v1/internal/agents/" + agentId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"agentId": "%s", "ownerId": "%s", "status": "%s",
                                 "policyHash": "%s", "name": "biz-agent",
                                 "createdAt": "2026-01-01T00:00:00Z"}
                                """.formatted(agentId, ownerId, status, policyHash))));
    }

    private void stubUpdatePolicy(UUID agentId) {
        identityService().stubFor(WireMock.put(urlPathEqualTo("/api/v1/internal/agents/" + agentId + "/policy"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"agentId": "%s", "status": "ACTIVE", "name": "biz-agent",
                                 "createdAt": "2026-01-01T00:00:00Z"}
                                """.formatted(agentId))));
    }

    private record AgentContext(UUID agentId, UUID ownerId, String apiKey, String policyHash) {}

    private record ErrorResult(int status, ApiError error) {}
}
