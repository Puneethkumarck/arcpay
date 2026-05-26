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

import static org.assertj.core.api.Assertions.assertThat;

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
    void shouldRejectRateLimitedRegistrations() {
        // when — register 10 times (limit is 10/hour)
        for (var i = 0; i < 10; i++) {
            restClient().post()
                    .uri("/api/v1/owners/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("""
                            {"email": "user%d@example.com", "walletAddress": "0x%s"}
                            """.formatted(i, "a".repeat(38) + String.format("%02d", i)))
                    .exchange((req, resp) -> resp.getStatusCode().value());
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
