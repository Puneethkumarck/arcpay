package com.arcpay.policy.policyengine.application.controller;

import com.arcpay.platform.api.ApiError;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.policy.policyengine.api.PolicyRule;
import com.arcpay.policy.policyengine.api.model.PolicyListResponse;
import com.arcpay.policy.policyengine.api.model.PolicyResponse;
import com.arcpay.policy.policyengine.domain.exception.AgentNotActiveException;
import com.arcpay.policy.policyengine.domain.exception.AgentNotFoundException;
import com.arcpay.policy.policyengine.domain.exception.AgentOwnershipException;
import com.arcpay.policy.policyengine.domain.exception.InvalidPolicyException;
import com.arcpay.policy.policyengine.domain.exception.PolicyNotFoundException;
import com.arcpay.policy.policyengine.domain.model.Policy;
import com.arcpay.policy.policyengine.domain.policy.PolicyCommandHandler;
import com.arcpay.policy.policyengine.domain.policy.PolicyQueryHandler;
import com.arcpay.policy.policyengine.test.RestControllerAbstractTest;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.UUID;

import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_ACTIVE_POLICY;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_AGENT_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_EMAIL;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_POLICY_ID;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_RULES;
import static com.arcpay.policy.policyengine.test.fixtures.PolicyFixtures.SOME_SUPERSEDED_POLICY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PolicyControllerIntegrationTest extends RestControllerAbstractTest {

    private static final String CREATE_BODY = """
            {
              "rules": [
                { "type": "DAILY_LIMIT", "amount": 1000.00 },
                { "type": "PER_TX_LIMIT", "amount": 100.00 }
              ]
            }
            """;

    @MockitoBean
    private PolicyCommandHandler policyCommandHandler;

    @MockitoBean
    private PolicyQueryHandler policyQueryHandler;

    @Autowired
    private JsonMapper jsonMapper;

    private static UsernamePasswordAuthenticationToken ownerAuth() {
        var principal = new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private static PolicyResponse expectedResponse(Policy policy) {
        return PolicyResponse.builder()
                .policyId(policy.policyId())
                .agentId(policy.agentId())
                .version(policy.version())
                .rules(policy.rules())
                .policyHash(policy.policyHash())
                .status(policy.status().name())
                .createdAt(policy.createdAt())
                .build();
    }

    @Test
    void shouldCreatePolicyAndReturn201() throws Exception {
        // given
        given(policyCommandHandler.createOrUpdatePolicy(eqAgent(), eqPrincipal(), eqRules()))
                .willReturn(SOME_ACTIVE_POLICY);

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PolicyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse(SOME_ACTIVE_POLICY));
    }

    @Test
    void shouldReturn400WhenRulesEmpty() throws Exception {
        // given — request body with empty rules list violates @Size(min = 1)

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rules\": []}"))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0002");
    }

    @Test
    void shouldReturn400WhenPolicyInvalid() throws Exception {
        // given
        given(policyCommandHandler.createOrUpdatePolicy(eqAgent(), eqPrincipal(), eqRules()))
                .willThrow(new InvalidPolicyException("weekly limit must be >= daily limit"));

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isBadRequest())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0002");
    }

    @Test
    void shouldReturn403WhenAgentNotOwned() throws Exception {
        // given
        given(policyCommandHandler.createOrUpdatePolicy(eqAgent(), eqPrincipal(), eqRules()))
                .willThrow(new AgentOwnershipException(SOME_AGENT_ID, SOME_OWNER_ID));

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0006");
    }

    @Test
    void shouldReturn404WhenAgentNotFound() throws Exception {
        // given
        given(policyCommandHandler.createOrUpdatePolicy(eqAgent(), eqPrincipal(), eqRules()))
                .willThrow(new AgentNotFoundException(SOME_AGENT_ID));

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0005");
    }

    @Test
    void shouldReturn422WhenAgentNotActive() throws Exception {
        // given
        given(policyCommandHandler.createOrUpdatePolicy(eqAgent(), eqPrincipal(), eqRules()))
                .willThrow(new AgentNotActiveException(SOME_AGENT_ID, "SUSPENDED"));

        // when
        var response = mockMvc.perform(post("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andExpect(status().isUnprocessableEntity())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0004");
    }

    @Test
    void shouldGetActivePolicy() throws Exception {
        // given
        given(policyQueryHandler.getActivePolicy(SOME_AGENT_ID, SOME_OWNER_ID)).willReturn(SOME_ACTIVE_POLICY);

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}/policies/active", SOME_AGENT_ID)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PolicyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse(SOME_ACTIVE_POLICY));
    }

    @Test
    void shouldReturn404WhenNoActivePolicy() throws Exception {
        // given
        given(policyQueryHandler.getActivePolicy(SOME_AGENT_ID, SOME_OWNER_ID))
                .willThrow(new PolicyNotFoundException(SOME_AGENT_ID, "no active policy"));

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}/policies/active", SOME_AGENT_ID)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, ApiError.class);
        assertThat(actual.code()).isEqualTo("ARCPAY-POLICY-0001");
    }

    @Test
    void shouldGetPolicyById() throws Exception {
        // given
        given(policyQueryHandler.getPolicy(SOME_AGENT_ID, SOME_POLICY_ID, SOME_OWNER_ID))
                .willReturn(SOME_ACTIVE_POLICY);

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}/policies/{policyId}",
                        SOME_AGENT_ID, SOME_POLICY_ID)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PolicyResponse.class);
        assertThat(actual).usingRecursiveComparison().isEqualTo(expectedResponse(SOME_ACTIVE_POLICY));
    }

    @Test
    void shouldListPolicyHistory() throws Exception {
        // given
        Page<Policy> page = new PageImpl<>(
                List.of(SOME_ACTIVE_POLICY, SOME_SUPERSEDED_POLICY), PageRequest.of(0, 20), 2);
        given(policyQueryHandler.listPolicyHistory(SOME_AGENT_ID, SOME_OWNER_ID, PageRequest.of(0, 20)))
                .willReturn(page);

        // when
        var response = mockMvc.perform(get("/api/v1/agents/{agentId}/policies", SOME_AGENT_ID)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // then
        var actual = jsonMapper.readValue(response, PolicyListResponse.class);
        var expected = PolicyListResponse.builder()
                .content(List.of(expectedResponse(SOME_ACTIVE_POLICY), expectedResponse(SOME_SUPERSEDED_POLICY)))
                .page(0)
                .size(20)
                .totalElements(2)
                .totalPages(1)
                .build();
        assertThat(actual).usingRecursiveComparison().isEqualTo(expected);
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/agents/{agentId}/policies/active", SOME_AGENT_ID))
                .andExpect(status().isUnauthorized());
    }

    private static UUID eqAgent() {
        return SOME_AGENT_ID;
    }

    private static OwnerPrincipal eqPrincipal() {
        return new OwnerPrincipal(SOME_OWNER_ID, SOME_OWNER_EMAIL);
    }

    private static List<PolicyRule> eqRules() {
        return SOME_RULES;
    }
}
