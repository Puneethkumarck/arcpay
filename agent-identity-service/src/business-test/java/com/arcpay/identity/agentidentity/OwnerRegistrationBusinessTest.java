package com.arcpay.identity.agentidentity;

import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.test.BusinessTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OwnerRegistrationBusinessTest extends BusinessTest {

    @MockitoBean
    private CircleWalletService circleWalletService;

    @MockitoBean
    private BlockchainService blockchainService;

    @BeforeEach
    void setUp() {
        cleanDatabase();
    }

    @AfterEach
    void tearDown() {
        cleanDatabase();
    }

    @Test
    @Order(1)
    @SuppressWarnings("unchecked")
    void shouldRegisterOwnerAndReturnApiKey() {
        // when
        var response = restClient().post()
                .uri("/api/v1/owners/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "alice@example.com", "walletAddress": "0x1234567890abcdef1234567890abcdef12345678"}
                        """)
                .retrieve()
                .toEntity(Map.class);

        // then
        assertThat(response.getStatusCode().value()).isEqualTo(201);
        var body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("email")).isEqualTo("alice@example.com");
        assertThat(body.get("ownerId")).isNotNull();
        assertThat(body.get("apiKey")).asString().isNotBlank();
        assertThat(body.get("status")).isEqualTo("ACTIVE");
    }

    @Test
    @Order(2)
    @SuppressWarnings("unchecked")
    void shouldUseApiKeyToCreateAgent() {
        // given — register owner and extract apiKey
        var ownerResponse = restClient().post()
                .uri("/api/v1/owners/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "bob@example.com", "walletAddress": "0xabcdefabcdefabcdefabcdefabcdefabcdefabcd"}
                        """)
                .retrieve()
                .body(Map.class);
        var apiKey = (String) ownerResponse.get("apiKey");

        // when — use apiKey to create agent
        var agentResponse = restClient().post()
                .uri("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .body("""
                        {"name": "bobs-agent", "purpose": "Owner registration flow test", "policyHash": "0x%s"}
                        """.formatted("d".repeat(64)))
                .retrieve()
                .toEntity(Map.class);

        // then
        assertThat(agentResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(agentResponse.getBody().get("agentId")).isNotNull();
        assertThat(agentResponse.getBody().get("name")).isEqualTo("bobs-agent");
        assertThat(agentResponse.getBody().get("status")).isEqualTo("PROVISIONING");
    }

    @Test
    @Order(3)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void shouldRejectRateLimitedRegistrations() {
        // when — register 10 times (limit is 10/hour), each should succeed
        for (var i = 0; i < 10; i++) {
            var status = restClient().post()
                    .uri("/api/v1/owners/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"email": "user%d@example.com", "walletAddress": "0x%s"}
                            """.formatted(i, "a".repeat(38) + String.format("%02d", i)))
                    .exchange((req, resp) -> resp.getStatusCode().value());
            assertThat(status).as("Registration %d should succeed", i + 1).isEqualTo(201);
        }

        // then — 11th registration should be rate limited
        var status = restClient().post()
                .uri("/api/v1/owners/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "overflow@example.com", "walletAddress": "0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"}
                        """)
                .exchange((req, resp) -> resp.getStatusCode().value());
        assertThat(status).isEqualTo(429);
    }
}
