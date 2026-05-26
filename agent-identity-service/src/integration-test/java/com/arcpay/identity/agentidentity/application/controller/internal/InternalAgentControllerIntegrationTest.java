package com.arcpay.identity.agentidentity.application.controller.internal;

import com.arcpay.identity.agentidentity.application.security.OwnerPrincipal;
import com.arcpay.identity.agentidentity.application.security.Roles;
import com.arcpay.identity.agentidentity.application.controller.agent.handler.IdempotencyHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCommandHandler;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.test.RestControllerAbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalAgentControllerIntegrationTest extends RestControllerAbstractTest {

    private static final UUID OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");
    private static final String VALID_POLICY_HASH = "0x" + "b".repeat(64);

    @MockitoBean
    private AgentCommandHandler agentCommandHandler;

    @MockitoBean
    private AgentQueryHandler agentQueryHandler;

    @MockitoBean
    private OwnerCommandHandler ownerCommandHandler;

    @MockitoBean
    private AgentRepository agentRepository;

    @MockitoBean
    private IdempotencyHandler idempotencyHandler;

    private static UsernamePasswordAuthenticationToken serviceAuth() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.SERVICE));
        return new UsernamePasswordAuthenticationToken("service", null, authorities);
    }

    private static UsernamePasswordAuthenticationToken ownerAuth() {
        var principal = new OwnerPrincipal(OWNER_ID, "test@example.com");
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void shouldUpdatePolicyHashWithValidServiceAuth() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var updated = agent.toBuilder().policyHash(VALID_POLICY_HASH).build();
        given(agentRepository.findById(agent.agentId())).willReturn(Optional.of(agent));
        given(agentCommandHandler.updatePolicy(agent.agentId(), agent.ownerId(), VALID_POLICY_HASH))
                .willReturn(updated);

        // when / then
        mockMvc.perform(put("/api/v1/internal/agents/{agentId}/policy", agent.agentId())
                        .with(authentication(serviceAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyHash": "%s"}
                                """.formatted(VALID_POLICY_HASH)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.policyHash").value(VALID_POLICY_HASH));
    }

    @Test
    void shouldReturn403ForUnauthenticatedRequestOnInternalEndpoint() throws Exception {
        // when / then
        mockMvc.perform(put("/api/v1/internal/agents/{agentId}/policy", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyHash": "%s"}
                                """.formatted(VALID_POLICY_HASH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403ForOwnerApiKeyOnInternalEndpoint() throws Exception {
        // when / then
        mockMvc.perform(put("/api/v1/internal/agents/{agentId}/policy", UUID.randomUUID())
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyHash": "%s"}
                                """.formatted(VALID_POLICY_HASH)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn400ForInvalidPolicyHashFormat() throws Exception {
        // when / then
        mockMvc.perform(put("/api/v1/internal/agents/{agentId}/policy", UUID.randomUUID())
                        .with(authentication(serviceAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyHash": "not-a-hash"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0001"));
    }

    @Test
    void shouldReturn404ForUnknownAgent() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        given(agentRepository.findById(agentId)).willReturn(Optional.empty());

        // when / then
        mockMvc.perform(put("/api/v1/internal/agents/{agentId}/policy", agentId)
                        .with(authentication(serviceAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"policyHash": "%s"}
                                """.formatted(VALID_POLICY_HASH)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0002"));
    }
}
