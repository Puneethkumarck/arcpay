package com.arcpay.identity.agentidentity;

import com.arcpay.identity.agentidentity.domain.port.BlockchainService;
import com.arcpay.identity.agentidentity.domain.port.CircleWalletService;
import com.arcpay.identity.agentidentity.test.BusinessTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class IdempotencyBusinessTest extends BusinessTest {

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
    void shouldReturnSameResponseForDuplicateIdempotencyKey() {
        // given
        var idempotencyKey = UUID.randomUUID().toString();
        var requestBody = """
                {"name": "idempotent-agent", "purpose": "Idempotency test", "policyHash": "0x%s"}
                """.formatted("c".repeat(64));

        // when — first request
        var firstResponse = restClient().post()
                .uri("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", idempotencyKey)
                .body(requestBody)
                .retrieve()
                .toEntity(Map.class);

        assertThat(firstResponse.getStatusCode().value()).isEqualTo(201);

        // when — second request with same idempotency key
        var secondResponse = restClient().post()
                .uri("/api/v1/agents")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .header("Idempotency-Key", idempotencyKey)
                .body(requestBody)
                .retrieve()
                .toEntity(Map.class);

        // then — same response returned (full body comparison)
        assertThat(secondResponse.getStatusCode().value()).isEqualTo(201);
        assertThat(secondResponse.getBody()).isEqualTo(firstResponse.getBody());
    }

    @SuppressWarnings("unchecked")
    private String registerOwner() {
        var response = restClient().post()
                .uri("/api/v1/owners/register")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"email": "idempotent@example.com", "walletAddress": "0x3333333333333333333333333333333333333333"}
                        """)
                .retrieve()
                .body(Map.class);
        return (String) response.get("apiKey");
    }
}
