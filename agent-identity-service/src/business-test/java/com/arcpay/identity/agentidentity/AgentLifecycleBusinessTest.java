package com.arcpay.identity.agentidentity;

import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.BlockchainService.RegistrationResult;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService.WalletCreationResult;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class AgentLifecycleBusinessTest extends BusinessTest {

    private static final String SOME_WALLET_ID = "circle-wallet-biz";
    private static final String SOME_WALLET_ADDRESS = "0xabcdef1234567890abcdef1234567890abcdef12";
    private static final String SOME_TX_HASH = "0xdeadbeef1234567890deadbeef1234567890deadbeef1234567890deadbeef12";

    @MockitoBean
    private CircleWalletService circleWalletService;

    @MockitoBean
    private BlockchainService blockchainService;

    private String apiKey;
    private UUID agentId;

    @BeforeEach
    void setUp() {
        cleanDatabase();
        apiKey = registerOwner();

        given(circleWalletService.createWallet(any()))
                .willReturn(new WalletCreationResult(SOME_WALLET_ID, SOME_WALLET_ADDRESS));
        given(blockchainService.registerAgent(any(), any(), any()))
                .willReturn(new RegistrationResult(SOME_TX_HASH, 42L));
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCompleteFullAgentLifecycle() {
        // register agent
        var idempotencyKey = UUID.randomUUID().toString();
        var createResponse = restClient().post()
                .uri("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", idempotencyKey)
                .body("""
                        {"name": "lifecycle-agent", "purpose": "E2E lifecycle test", "policyHash": "0x%s"}
                        """.formatted("a".repeat(64)))
                .retrieve()
                .toEntity(Map.class);

        assertThat(createResponse.getStatusCode().value()).isEqualTo(201);
        agentId = UUID.fromString((String) createResponse.getBody().get("agentId"));

        // poll until ACTIVE
        await().atMost(Duration.ofSeconds(30))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    var status = getAgent();
                    assertThat(status.get("status")).isEqualTo("ACTIVE");
                });

        // verify provisioned state
        var activeAgent = getAgent();
        assertThat(activeAgent.get("walletAddress")).isEqualTo(SOME_WALLET_ADDRESS);
        assertThat(activeAgent.get("onChainTxHash")).isEqualTo(SOME_TX_HASH);

        // deactivate
        var deactivateResponse = restClient().post()
                .uri("/api/v1/agents/{agentId}/deactivate", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .toEntity(Map.class);
        assertThat(deactivateResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(deactivateResponse.getBody().get("status")).isEqualTo("SUSPENDED");

        // reactivate
        var reactivateResponse = restClient().post()
                .uri("/api/v1/agents/{agentId}/reactivate", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .toEntity(Map.class);
        assertThat(reactivateResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(reactivateResponse.getBody().get("status")).isEqualTo("ACTIVE");

        // update metadata
        var updateResponse = restClient().put()
                .uri("/api/v1/agents/{agentId}", agentId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body("""
                        {"name": "updated-agent", "purpose": "Updated purpose"}
                        """)
                .retrieve()
                .toEntity(Map.class);
        assertThat(updateResponse.getStatusCode().value()).isEqualTo(200);
        assertThat(updateResponse.getBody().get("name")).isEqualTo("updated-agent");
        assertThat(updateResponse.getBody().get("metadataHash")).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getAgent() {
        return restClient().get()
                .uri("/api/v1/agents/{agentId}", agentId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private String registerOwner() {
        var response = restClient().post()
                .uri("/api/v1/owners/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "lifecycle@example.com", "walletAddress": "0x1111111111111111111111111111111111111111"}
                        """)
                .retrieve()
                .body(Map.class);
        return (String) response.get("apiKey");
    }
}
