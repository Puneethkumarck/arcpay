package com.arcpay.identity.agentidentity.infrastructure.client.circle;

import com.arcpay.platform.infrastructure.circle.EntitySecretCiphertextProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.CircleKeyFixtures.SOME_ENTITY_SECRET_HEX;
import static com.arcpay.identity.agentidentity.fixtures.CircleKeyFixtures.entityPublicKeyResponseJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CircleWalletAdapterTest {

    private static final CircleApiProperties PROPERTIES = new CircleApiProperties(
            "https://api.circle.com", "test-api-key", "wallet-set-1", "ARC",
            SOME_ENTITY_SECRET_HEX, 5000, 10000);

    private static final String WALLETS_URL = "https://api.circle.com/v1/w3s/developer/wallets";
    private static final String PUBLIC_KEY_URL = "https://api.circle.com/v1/w3s/config/entity/publicKey";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CircleWalletAdapter adapter;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl(PROPERTIES.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + PROPERTIES.apiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        mockServer = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        var restClient = builder.build();
        var ciphertextProvider = new EntitySecretCiphertextProvider(SOME_ENTITY_SECRET_HEX, restClient);
        adapter = new CircleWalletAdapter(PROPERTIES, restClient, ciphertextProvider);
    }

    private void stubEntityPublicKey() {
        mockServer.expect(requestTo(PUBLIC_KEY_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(entityPublicKeyResponseJson(), MediaType.APPLICATION_JSON));
    }

    @Test
    void shouldReturnWalletCreationResultOnSuccess() {
        // given
        var agentId = UUID.fromString("019718a0-5678-7def-8000-abcdef567890");
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
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
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
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
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
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
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
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
    void shouldSendNonEmptyEntitySecretCiphertextInRequestBody() {
        // given
        var agentId = UUID.randomUUID();
        var capturedBody = new ArrayList<String>();
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
                .andExpect(captureBody(capturedBody))
                .andRespond(withSuccess("""
                        {"data": {"wallets": [{"id": "w-1", "address": "0xabc", "blockchain": "ARC"}]}}
                        """, MediaType.APPLICATION_JSON));

        // when
        adapter.createWallet(agentId);

        // then
        assertThat(ciphertextOf(capturedBody.getFirst())).isNotBlank();
    }

    @Test
    void shouldSendFreshCiphertextOnEachCreateWalletCall() {
        // given
        var capturedBodies = new ArrayList<String>();
        stubEntityPublicKey();
        mockServer.expect(once(), requestTo(WALLETS_URL))
                .andExpect(captureBody(capturedBodies))
                .andRespond(withSuccess(walletResponse("w-1"), MediaType.APPLICATION_JSON));
        mockServer.expect(once(), requestTo(WALLETS_URL))
                .andExpect(captureBody(capturedBodies))
                .andRespond(withSuccess(walletResponse("w-2"), MediaType.APPLICATION_JSON));

        // when
        adapter.createWallet(UUID.randomUUID());
        adapter.createWallet(UUID.randomUUID());

        // then
        var firstCiphertext = ciphertextOf(capturedBodies.get(0));
        var secondCiphertext = ciphertextOf(capturedBodies.get(1));
        assertThat(firstCiphertext).isNotBlank();
        assertThat(secondCiphertext).isNotBlank();
        assertThat(firstCiphertext).isNotEqualTo(secondCiphertext);
    }

    @Test
    void shouldThrowCircleApiExceptionOnServerError() {
        // given
        var agentId = UUID.randomUUID();
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
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
        stubEntityPublicKey();
        mockServer.expect(requestTo(WALLETS_URL))
                .andRespond(withSuccess("""
                        {"data": {"wallets": []}}
                        """, MediaType.APPLICATION_JSON));

        // when / then
        assertThatThrownBy(() -> adapter.createWallet(agentId))
                .isInstanceOf(CircleApiException.class)
                .hasMessageContaining("Empty wallet response");
    }

    private RequestMatcher captureBody(List<String> sink) {
        return request -> sink.add(((MockClientHttpRequest) request).getBodyAsString());
    }

    private String ciphertextOf(String body) {
        try {
            return objectMapper.readTree(body).get("entitySecretCiphertext").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read entitySecretCiphertext from request body", e);
        }
    }

    private String walletResponse(String walletId) {
        return """
                {"data": {"wallets": [{"id": "%s", "address": "0xabc", "blockchain": "ARC"}]}}
                """.formatted(walletId);
    }
}
