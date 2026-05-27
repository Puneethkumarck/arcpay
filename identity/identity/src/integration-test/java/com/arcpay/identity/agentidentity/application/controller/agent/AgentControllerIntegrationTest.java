package com.arcpay.identity.agentidentity.application.controller.agent;

import com.arcpay.identity.agentidentity.api.model.AgentResponse;
import com.arcpay.identity.agentidentity.api.model.AgentStatusEnum;
import com.arcpay.platform.api.ApiError;
import com.arcpay.identity.agentidentity.api.model.ProvisioningStatusResponse;
import com.arcpay.identity.agentidentity.api.model.ProvisioningStepResponse;
import com.arcpay.identity.agentidentity.api.model.StepStatusEnum;
import com.arcpay.identity.agentidentity.application.controller.agent.handler.IdempotencyHandler;
import com.arcpay.identity.agentidentity.application.controller.agent.mapper.AgentResponseMapper;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.exception.AgentNotFoundException;
import com.arcpay.identity.agentidentity.domain.exception.ForbiddenException;
import com.arcpay.identity.agentidentity.domain.model.ProvisioningStatus;
import com.arcpay.identity.agentidentity.domain.model.StepStatus;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCommandHandler;
import com.arcpay.identity.agentidentity.domain.port.AgentRepository;
import com.arcpay.identity.agentidentity.test.RestControllerAbstractTest;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.AgentFixtures.SOME_AGENT_ACTIVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

    @Autowired
    private JsonMapper jsonMapper;

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

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, AgentResponse.class);
        var expected = AgentResponse.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(AgentStatusEnum.ACTIVE)
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .createdAt(agent.createdAt())
                .updatedAt(agent.updatedAt())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn404ForNonExistentAgent() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        given(agentQueryHandler.getAgent(agentId, OWNER_ID))
                .willThrow(new AgentNotFoundException(agentId));

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}", agentId)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-IDENTITY-0002");
    }

    @Test
    void shouldReturn403ForWrongOwner() throws Exception {
        // given
        var agentId = UUID.randomUUID();
        given(agentQueryHandler.getAgent(agentId, OWNER_ID))
                .willThrow(new ForbiddenException("agent", OWNER_ID));

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}", agentId)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-IDENTITY-0003");
    }

    @Test
    void shouldDeactivateAgent() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var deactivated = agent.deactivate();
        given(agentCommandHandler.deactivate(agent.agentId(), OWNER_ID)).willReturn(deactivated);

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/deactivate", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, AgentResponse.class);
        var expected = AgentResponse.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(AgentStatusEnum.SUSPENDED)
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .createdAt(deactivated.createdAt())
                .updatedAt(deactivated.updatedAt())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReactivateAgent() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE.deactivate();
        var reactivated = agent.reactivate();
        given(agentCommandHandler.reactivate(agent.agentId(), OWNER_ID)).willReturn(reactivated);

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/reactivate", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, AgentResponse.class);
        var expected = AgentResponse.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name(agent.name())
                .purpose(agent.purpose())
                .status(AgentStatusEnum.ACTIVE)
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(agent.metadataHash())
                .createdAt(reactivated.createdAt())
                .updatedAt(reactivated.updatedAt())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldUpdateMetadata() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var updated = agent.updateMetadata("new-name", "new-purpose", "0xnewhash");
        given(agentCommandHandler.updateMetadata(agent.agentId(), OWNER_ID, "new-name", "new-purpose"))
                .willReturn(updated);

        // when
        var response = mockMvc.perform(put("/api/v1/agents/{agentId}", agent.agentId())
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "new-name", "purpose": "new-purpose"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, AgentResponse.class);
        var expected = AgentResponse.builder()
                .agentId(agent.agentId())
                .ownerId(agent.ownerId())
                .name("new-name")
                .purpose("new-purpose")
                .status(AgentStatusEnum.ACTIVE)
                .walletAddress(agent.walletAddress())
                .onChainTxHash(agent.onChainTxHash())
                .policyHash(agent.policyHash())
                .metadataHash(updated.metadataHash())
                .createdAt(updated.createdAt())
                .updatedAt(updated.updatedAt())
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn403ForUnauthenticatedRequest() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}", UUID.randomUUID()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldGetProvisioningStatus() throws Exception {
        // given
        var agent = SOME_AGENT_ACTIVE;
        var provStatus = new ProvisioningStatus(
                agent.agentId(),
                agent.status(),
                StepStatus.COMPLETED,
                StepStatus.COMPLETED);
        given(agentQueryHandler.getProvisioningStatus(agent.agentId(), OWNER_ID)).willReturn(provStatus);

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}/status", agent.agentId())
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ProvisioningStatusResponse.class);
        var expected = ProvisioningStatusResponse.builder()
                .agentId(agent.agentId())
                .status(AgentStatusEnum.ACTIVE)
                .steps(List.of(
                        ProvisioningStepResponse.builder()
                                .name("WALLET_CREATION")
                                .status(StepStatusEnum.COMPLETED)
                                .build(),
                        ProvisioningStepResponse.builder()
                                .name("ON_CHAIN_REGISTRATION")
                                .status(StepStatusEnum.COMPLETED)
                                .build()))
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }
}
