package com.arcpay.identity.agentidentity.application.controller.agent;

import com.arcpay.identity.agentidentity.application.controller.agent.handler.IdempotencyHandler;
import com.arcpay.identity.agentidentity.application.controller.agent.mapper.AgentResponseMapper;
import com.arcpay.identity.agentidentity.application.security.OwnerPrincipal;
import com.arcpay.identity.agentidentity.application.security.Roles;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCommandHandler;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.test.RestControllerAbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentControllerIntegrationTest extends RestControllerAbstractTest {

    private static final UUID OWNER_ID = UUID.fromString("019718a0-1234-7def-8000-abcdef123456");

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

    private static UsernamePasswordAuthenticationToken ownerAuth() {
        var principal = new OwnerPrincipal(OWNER_ID, "test@example.com");
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void shouldGetAgentById() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        given(agentQueryHandler.getAgent(agent.agentId(), OWNER_ID)).willReturn(agent);

        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agent.agentId().toString()))
                .andExpect(jsonPath("$.name").value(agent.name()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldReturn404ForNonExistentAgent() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        given(agentQueryHandler.getAgent(agentId, OWNER_ID))
                .willThrow(new AgentNotFoundException(agentId));

        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}", agentId)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0002"));
    }

    @Test
    void shouldReturn403ForWrongOwner() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        given(agentQueryHandler.getAgent(agentId, OWNER_ID))
                .willThrow(new ForbiddenException("agent", OWNER_ID));

        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}", agentId)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0003"));
    }

    @Test
    void shouldDeactivateAgent() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var deactivated = agent.deactivate();
        given(agentCommandHandler.deactivate(agent.agentId(), OWNER_ID)).willReturn(deactivated);

        // when / then
        mockMvc.perform(post("/api/v1/agents/{agentId}/deactivate", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void shouldReactivateAgent() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE.deactivate();
        var reactivated = agent.reactivate();
        given(agentCommandHandler.reactivate(agent.agentId(), OWNER_ID)).willReturn(reactivated);

        // when / then
        mockMvc.perform(post("/api/v1/agents/{agentId}/reactivate", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void shouldUpdateMetadata() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var updated = agent.updateMetadata("new-name", "new-purpose", "0xnewhash");
        given(agentCommandHandler.updateMetadata(agent.agentId(), OWNER_ID, "new-name", "new-purpose"))
                .willReturn(updated);

        // when / then
        mockMvc.perform(put("/api/v1/agents/{agentId}", agent.agentId())
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "new-name", "purpose": "new-purpose"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("new-name"));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}", UUID.randomUUID()))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void shouldGetProvisioningStatus() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var provStatus = new AgentQueryHandler.ProvisioningStatus(
                agent.agentId(),
                agent.status(),
                AgentQueryHandler.StepStatus.COMPLETED,
                AgentQueryHandler.StepStatus.COMPLETED);
        given(agentQueryHandler.getProvisioningStatus(agent.agentId(), OWNER_ID)).willReturn(provStatus);

        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}/status", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agentId").value(agent.agentId().toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.steps[0].name").value("WALLET_CREATION"))
                .andExpect(jsonPath("$.steps[0].status").value("COMPLETED"));
    }
}
