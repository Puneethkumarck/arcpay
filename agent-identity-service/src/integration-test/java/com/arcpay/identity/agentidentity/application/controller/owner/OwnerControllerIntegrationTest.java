package com.arcpay.identity.agentidentity.application.controller.owner;

import com.arcpay.identity.agentidentity.application.controller.agent.handler.IdempotencyHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.exception.InvalidEmailException;
import com.arcpay.identity.agentidentity.domain.exception.OwnerEmailAlreadyExistsException;
import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.model.OwnerStatus;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCommandHandler;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCreationService;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.test.RestControllerAbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OwnerControllerIntegrationTest extends RestControllerAbstractTest {

    @MockitoBean
    private OwnerCommandHandler ownerCommandHandler;

    @MockitoBean
    private AgentCommandHandler agentCommandHandler;

    @MockitoBean
    private AgentQueryHandler agentQueryHandler;

    @MockitoBean
    private AgentRepository agentRepository;

    @MockitoBean
    private IdempotencyHandler idempotencyHandler;

    @Test
    void shouldRegisterOwnerAndReturn201() throws Exception {
        // given
        var ownerId = UUID.randomUUID();
        var now = Instant.now();
        var owner = Owner.builder()
                .ownerId(ownerId)
                .email("alice@example.com")
                .walletAddress("0x1234567890abcdef1234567890abcdef12345678")
                .apiKeyHash("hash")
                .status(OwnerStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        var rawApiKey = "ak_test_secretkey123";
        given(ownerCommandHandler.registerOwner("alice@example.com", "0x1234567890abcdef1234567890abcdef12345678"))
                .willReturn(new OwnerCreationService.OwnerWithApiKey(owner, rawApiKey));

        // when / then
        mockMvc.perform(post("/api/v1/owners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alice@example.com", "walletAddress": "0x1234567890abcdef1234567890abcdef12345678"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.apiKey").value(rawApiKey))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn400ForInvalidEmail() throws Exception {
        // when / then
        mockMvc.perform(post("/api/v1/owners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "", "walletAddress": "0x1234567890abcdef1234567890abcdef12345678"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0001"));
    }

    @Test
    void shouldReturn400ForMissingWalletAddress() throws Exception {
        // when / then
        mockMvc.perform(post("/api/v1/owners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alice@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0001"));
    }

    @Test
    void shouldReturn400ForInvalidWalletAddressFormat() throws Exception {
        // when / then
        mockMvc.perform(post("/api/v1/owners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alice@example.com", "walletAddress": "not-a-wallet"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0001"));
    }

    @Test
    void shouldReturn409ForDuplicateEmail() throws Exception {
        // given
        given(ownerCommandHandler.registerOwner("alice@example.com", "0x1234567890abcdef1234567890abcdef12345678"))
                .willThrow(new OwnerEmailAlreadyExistsException("alice@example.com"));

        // when / then
        mockMvc.perform(post("/api/v1/owners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "alice@example.com", "walletAddress": "0x1234567890abcdef1234567890abcdef12345678"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0004"));
    }

    @Test
    void shouldReturn400ForDomainValidationFailure() throws Exception {
        // given
        given(ownerCommandHandler.registerOwner("bad-email", "0x1234567890abcdef1234567890abcdef12345678"))
                .willThrow(new InvalidEmailException("bad-email"));

        // when / then
        mockMvc.perform(post("/api/v1/owners/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "bad-email", "walletAddress": "0x1234567890abcdef1234567890abcdef12345678"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0001"));
    }
}
