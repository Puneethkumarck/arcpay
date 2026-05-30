package com.arcpay.payment.paymentexecution.application.controller;

import com.arcpay.payment.paymentexecution.api.model.PaymentListResponse;
import com.arcpay.payment.paymentexecution.api.model.PaymentResponse;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import com.arcpay.payment.paymentexecution.test.RestControllerAbstractTest;
import com.arcpay.platform.api.ApiError;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_AGENT_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_IDEMPOTENCY_KEY;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_OWNER_ID;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_RECIPIENT;
import static com.arcpay.payment.paymentexecution.fixtures.PaymentFixtures.SOME_WALLET_ADDRESS;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerIntegrationTest extends RestControllerAbstractTest {

    private static final String AGENT_PATH = "/api/v1/internal/agents/" + SOME_AGENT_ID;

    private static WireMockServer identityServer;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        jdbcTemplate.update("DELETE FROM paymentexecution_outbox_record");
        jdbcTemplate.update("DELETE FROM payment");
    }

    private long paymentCount() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM payment", Long.class);
    }

    private static UsernamePasswordAuthenticationToken ownerAuth() {
        var principal = new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private static String createBody(String idempotencyKey, String amount) {
        return """
                {
                  "agentId": "%s",
                  "idempotencyKey": "%s",
                  "recipientAddress": "%s",
                  "amount": %s,
                  "currency": "USDC",
                  "memo": "GPT-4 API credits"
                }
                """.formatted(SOME_AGENT_ID, idempotencyKey, SOME_RECIPIENT, amount);
    }

    private static void stubAgent(UUID ownerId, String status) {
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(agentJson(ownerId, status))));
    }

    private static String agentJson(UUID ownerId, String status) {
        return """
                {
                  "agentId": "%s",
                  "ownerId": "%s",
                  "name": "research-bot",
                  "status": "%s",
                  "walletId": "wallet-abc-123",
                  "walletAddress": "%s"
                }
                """.formatted(SOME_AGENT_ID, ownerId, status, SOME_WALLET_ADDRESS);
    }

    private long outboxCount() {
        return jdbcTemplate.queryForObject(
                "SELECT count(*) FROM paymentexecution_outbox_record WHERE record_type LIKE ?",
                Long.class, "%PaymentRequested");
    }

    @Test
    void shouldCreatePaymentAndPublishEvent() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PaymentResponse.class);
        assertThat(actual.status()).isEqualTo("PENDING");
        assertThat(actual.transactionHash()).isNull();
        assertThat(actual.to()).isEqualTo(SOME_RECIPIENT);
        assertThat(actual.chain()).isEqualTo("ARC");
        assertThat(paymentCount()).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    void shouldReturn200OnIdempotentReplayWithoutSecondRow() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");
        mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isAccepted());

        // when
        mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isOk());

        // then
        assertThat(paymentCount()).isEqualTo(1);
        assertThat(outboxCount()).isEqualTo(1);
    }

    @Test
    void shouldReturn422OnIdempotencyConflict() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");
        mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isAccepted());

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "50.00")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0005");
        assertThat(paymentCount()).isEqualTo(1);
    }

    @Test
    void shouldReturn422WhenAgentNotActive() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "SUSPENDED");

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0004");
        assertThat(paymentCount()).isZero();
    }

    @Test
    void shouldReturn422WhenAgentNotFound() throws Exception {
        // given
        identityServer.stubFor(WireMock.get(urlPathEqualTo(AGENT_PATH))
                .willReturn(aResponse().withStatus(404)));

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0009");
    }

    @Test
    void shouldReturn422WhenAgentNotOwned() throws Exception {
        // given
        stubAgent(UUID.randomUUID(), "ACTIVE");

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0009");
    }

    @Test
    void shouldReturn422WhenAmountExceedsScale() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "1.1234567")))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0003");
    }

    @Test
    void shouldReturn422OnSelfPayment() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");

        // when
        var response = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentId": "%s",
                                  "idempotencyKey": "%s",
                                  "recipientAddress": "%s",
                                  "amount": 25.00,
                                  "currency": "USDC"
                                }
                                """.formatted(SOME_AGENT_ID, SOME_IDEMPOTENCY_KEY, SOME_WALLET_ADDRESS)))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0003");
    }

    @Test
    void shouldGetPaymentByIdForOwner() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");
        var created = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        var paymentId = jsonMapper.readValue(created, PaymentResponse.class).paymentId();

        // when
        var response = mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PaymentResponse.class);
        assertThat(actual.paymentId()).isEqualTo(paymentId);
    }

    @Test
    void shouldReturn404WhenPaymentMissing() throws Exception {
        // when
        var response = mockMvc.perform(get("/api/v1/payments/{paymentId}", UUID.randomUUID())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0001");
    }

    @Test
    void shouldReturn403WhenReadingAnotherOwnersPayment() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");
        var created = mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        var paymentId = jsonMapper.readValue(created, PaymentResponse.class).paymentId();
        var otherOwner = new UsernamePasswordAuthenticationToken(
                new OwnerPrincipal(UUID.randomUUID(), "other@arcpay.dev"),
                null, List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER)));

        // when
        var response = mockMvc.perform(get("/api/v1/payments/{paymentId}", paymentId)
                        .with(authentication(otherOwner)))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-PAYMENT-0002");
    }

    @Test
    void shouldListPaymentsScopedToOwner() throws Exception {
        // given
        stubAgent(SOME_OWNER_ID, "ACTIVE");
        mockMvc.perform(post("/api/v1/payments")
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody(SOME_IDEMPOTENCY_KEY, "25.00")))
                .andExpect(status().isAccepted());

        // when
        var response = mockMvc.perform(get("/api/v1/payments")
                        .param("agentId", SOME_AGENT_ID.toString())
                        .param("status", PaymentStatus.PENDING.name())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PaymentListResponse.class);
        assertThat(actual.content()).hasSize(1);
        assertThat(actual.totalElements()).isEqualTo(1);
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/payments/{paymentId}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }
}
