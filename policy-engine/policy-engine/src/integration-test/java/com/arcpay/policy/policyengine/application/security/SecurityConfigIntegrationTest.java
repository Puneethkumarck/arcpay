package com.arcpay.policy.policyengine.application.security;

import com.arcpay.policy.policyengine.test.FullContextIntegrationTest;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter.hashApiKey;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the Policy Engine security filter chain end-to-end against a full Spring context,
 * with the Identity Service stubbed via WireMock.
 *
 * <p>Covers spec §9.4: public health endpoint, reject without API key, authenticate with a valid
 * API key, service-to-service auth via {@code X-Service-Auth}, and rejecting an internal endpoint
 * accessed with an owner API key.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(SecurityConfigIntegrationTest.TestEndpoints.class)
class SecurityConfigIntegrationTest extends FullContextIntegrationTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String VALID_API_KEY = "valid-owner-key";
    private static final String UNKNOWN_API_KEY = "unknown-key";

    private static WireMockServer identityServer;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

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
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        identityServer.resetAll();
    }

    @Test
    void shouldAllowHealthEndpointWithoutAuth() throws Exception {
        // given — no credentials

        // when / then — health is public
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectAuthenticatedEndpointWithoutApiKey() throws Exception {
        // given — no credentials

        // when / then — unauthenticated requests are rejected with 401 (not 403)
        mockMvc.perform(get("/api/v1/test/ping"))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void shouldAuthenticateWithValidApiKey() throws Exception {
        // given — Identity resolves the hashed API key to an owner
        stubResolveApiKey(hashApiKey(VALID_API_KEY));

        // when / then
        mockMvc.perform(get("/api/v1/test/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_API_KEY))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectUnknownApiKey() throws Exception {
        // given — Identity returns 404 for the unknown key hash
        identityServer.stubFor(WireMock.get(urlPathEqualTo(
                        "/api/v1/internal/owners/by-api-key-hash/" + hashApiKey(UNKNOWN_API_KEY)))
                .willReturn(aResponse().withStatus(404)));

        // when / then — fail-closed → 401
        mockMvc.perform(get("/api/v1/test/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + UNKNOWN_API_KEY))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void shouldAuthenticateInternalEndpointWithServiceToken() throws Exception {
        // given — a valid X-Service-Auth token

        // when / then — service-to-service auth grants ROLE_SERVICE
        mockMvc.perform(get("/api/v1/internal/test/ping")
                        .header("X-Service-Auth", SERVICE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInternalEndpointWithOwnerApiKey() throws Exception {
        // given — a valid owner API key (ROLE_OWNER, not ROLE_SERVICE)
        stubResolveApiKey(hashApiKey(VALID_API_KEY));

        // when / then — internal endpoints require ROLE_SERVICE → 403
        mockMvc.perform(get("/api/v1/internal/test/ping")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_API_KEY))
                .andExpect(status().is(HttpStatus.FORBIDDEN.value()));
    }

    private static void stubResolveApiKey(String hash) {
        identityServer.stubFor(WireMock.get(urlPathEqualTo(
                        "/api/v1/internal/owners/by-api-key-hash/" + hash))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "ownerId": "019576a0-0000-7000-8000-000000000003",
                                  "email": "owner@example.com"
                                }
                                """)));
    }

    @TestConfiguration
    static class TestEndpoints {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/test/ping")
        String ping() {
            return "pong";
        }

        @GetMapping("/api/v1/internal/test/ping")
        String internalPing() {
            return "internal-pong";
        }
    }
}
