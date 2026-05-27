package com.arcpay.identity.agentidentity;

import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.test.BusinessTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class AgentProvisioningFailureBusinessTest extends BusinessTest {

    @MockitoBean
    private CircleWalletService circleWalletService;

    @MockitoBean
    private BlockchainService blockchainService;

    private String apiKey;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        apiKey = registerOwner();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReachFailedStatusOnCircleApiFailure() {
        // given — Circle wallet creation fails
        // any() required: agentId is server-generated and stubs must be in place before Temporal workflow runs
        given(circleWalletService.createWallet(any()))
                .willThrow(new RuntimeException("Circle API unavailable"));

        // when — register agent
        var createResponse = restClient().post()
                .uri("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"name": "failure-agent", "purpose": "Failure path test", "policyHash": "0x%s"}
                        """.formatted("b".repeat(64)))
                .retrieve()
                .toEntity(Map.class);

        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
        var agentId = UUID.fromString((String) createResponse.getBody().get("agentId"));

        // then — poll until FAILED (allow time for Temporal activity retries + compensation)
        await().atMost(Duration.ofSeconds(60))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var agent = restClient().get()
                            .uri("/api/v1/agents/{agentId}", agentId)
                            .header("Authorization", "Bearer " + apiKey)
                            .retrieve()
                            .body(Map.class);
                    assertThat(agent.get("status")).isEqualTo("FAILED");
                    assertThat((String) agent.get("failureReason")).isNotBlank();
                });
    }

    @SuppressWarnings("unchecked")
    private String registerOwner() {
        var response = restClient().post()
                .uri("/api/v1/owners/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "failure@example.com", "walletAddress": "0x2222222222222222222222222222222222222222"}
                        """)
                .retrieve()
                .body(Map.class);
        return (String) response.get("apiKey");
    }
}
