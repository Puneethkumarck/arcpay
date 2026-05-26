package com.arcpay.identity.agentidentity.infrastructure.client.circle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CircleWalletAdapterTest {

    private static final CircleApiProperties PROPERTIES = new CircleApiProperties(
            "https://api.circle.com", "test-api-key", "wallet-set-1", "ARC", 5000, 10000);

    private CircleWalletAdapter adapter;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PROPERTIES.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        adapter = new CircleWalletAdapter(PROPERTIES, builder.build());
    }

    @Test
    void shouldReturnWalletCreationResultOnSuccess() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
        mockServer.expect(requestTo("https://api.circle.com/v1/w3s/developer/wallets"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                            "data": {
                                "wallets": [{
                                    "id": "wallet-abc-123",
                                    "address": "0xAbCdEf1234567890AbCdEf1234567890AbCdEf12",
                                    "blockchain": "ARC"
                                }]
                            }
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        var result = adapter.createWallet(agentId);

        // then
        assertThat(result.walletId()).isEqualTo("wallet-abc-123");
        assertThat(result.walletAddress()).isEqualTo("0xabcdef1234567890abcdef1234567890abcdef12");
        mockServer.verify();
    }

    @Test
    void shouldNormalizeWalletAddressToLowercase() {
        // given
        var agentId = UUID.randomUUID();
        mockServer.expect(requestTo("https://api.circle.com/v1/w3s/developer/wallets"))
                .andRespond(withSuccess("""
                        {
                            "data": {
                                "wallets": [{
                                    "id": "w-1",
                                    "address": "0xABCDEF1234567890ABCDEF1234567890ABCDEF12",
                                    "blockchain": "ARC"
                                }]
                            }
                        }
                        """, MediaType.APPLICATION_JSON));

        // when
        var result = adapter.createWallet(agentId);

        // then
        assertThat(result.walletAddress()).isEqualTo("0xabcdef1234567890abcdef1234567890abcdef12");
    }

    @Test
    void shouldSendAgentIdAsIdempotencyKeyInRequestBody() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
        mockServer.expect(requestTo("https://api.circle.com/v1/w3s/developer/wallets"))
                .andExpect(content().json("""
                        {"idempotencyKey": "019718a0-5678-7def-8000-abcdef567890"}
                        """))
                .andRespond(withSuccess("""
                        {"data": {"wallets": [{"id": "w-1", "address": "0xabc", "blockchain": "ARC"}]}}
                        """, MediaType.APPLICATION_JSON));

        // when
        adapter.createWallet(agentId);

        // then
        mockServer.verify();
    }

    @Test
    void shouldSendBearerAuthHeader() {
        // given
        var agentId = UUID.randomUUID();
        mockServer.expect(requestTo("https://api.circle.com/v1/w3s/developer/wallets"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-api-key"))
                .andRespond(withSuccess("""
                        {"data": {"wallets": [{"id": "w-1", "address": "0xabc", "blockchain": "ARC"}]}}
                        """, MediaType.APPLICATION_JSON));

        // when
        adapter.createWallet(agentId);

        // then
        mockServer.verify();
    }

    @Test
    void shouldThrowCircleApiExceptionOnServerError() {
        // given
        var agentId = UUID.randomUUID();
        mockServer.expect(requestTo("https://api.circle.com/v1/w3s/developer/wallets"))
                .andRespond(withServerError());

        // when / then
        assertThatThrownBy(() -> adapter.createWallet(agentId))
                .isInstanceOf(CircleApiException.class)
                .hasMessageContaining(agentId.toString());
    }

    @Test
    void shouldThrowCircleApiExceptionOnEmptyWalletResponse() {
        // given
        var agentId = UUID.randomUUID();
        mockServer.expect(requestTo("https://api.circle.com/v1/w3s/developer/wallets"))
                .andRespond(withSuccess("""
                        {"data": {"wallets": []}}
                        """, MediaType.APPLICATION_JSON));

        // when / then
        assertThatThrownBy(() -> adapter.createWallet(agentId))
                .isInstanceOf(CircleApiException.class)
                .hasMessageContaining("Empty wallet response");
    }
}
