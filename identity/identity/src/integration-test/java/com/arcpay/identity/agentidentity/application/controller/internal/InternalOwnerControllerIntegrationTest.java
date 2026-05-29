package com.arcpay.identity.agentidentity.application.controller.internal;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.identity.agentidentity.application.controller.agent.handler.IdempotencyHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentCommandHandler;
import com.arcpay.identity.agentidentity.domain.agent.AgentQueryHandler;
import com.arcpay.identity.agentidentity.domain.owner.OwnerCommandHandler;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import com.arcpay.identity.agentidentity.test.RestControllerAbstractTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_API_KEY_HASH;
import static com.arcpay.identity.agentidentity.fixtures.OwnerFixtures.SOME_OWNER;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InternalOwnerControllerIntegrationTest extends RestControllerAbstractTest {

    private static final String OFFICER_API_KEY_HASH = "officer-key-hash";

    @MockitoBean
    private OwnerRepository ownerRepository;

    @MockitoBean
    private OwnerCommandHandler ownerCommandHandler;

    @MockitoBean
    private AgentCommandHandler agentCommandHandler;

    @MockitoBean
    private AgentQueryHandler agentQueryHandler;

    @MockitoBean
    private IdempotencyHandler idempotencyHandler;

    private static UsernamePasswordAuthenticationToken serviceAuth() {
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.SERVICE));
        return new UsernamePasswordAuthenticationToken("service", null, authorities);
    }

    private static UsernamePasswordAuthenticationToken ownerAuth() {
        var principal = new OwnerPrincipal(UUID.randomUUID(), "test@example.com");
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    @Test
    void shouldResolveOwnerByApiKeyHash() throws Exception {
        // given
        given(ownerRepository.findByApiKeyHash(SOME_API_KEY_HASH)).willReturn(Optional.of(SOME_OWNER));

        // when / then
        mockMvc.perform(get("/api/v1/internal/owners/by-api-key-hash/{hash}", SOME_API_KEY_HASH)
                        .with(authentication(serviceAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(SOME_OWNER.ownerId().toString()))
                .andExpect(jsonPath("$.email").value(SOME_OWNER.email()))
                .andExpect(jsonPath("$.authority").value(Roles.OWNER));
    }

    @Test
    void shouldResolveComplianceOfficerAuthorityForConfiguredKeyHash() throws Exception {
        // given
        given(ownerRepository.findByApiKeyHash(OFFICER_API_KEY_HASH)).willReturn(Optional.of(SOME_OWNER));

        // when / then
        mockMvc.perform(get("/api/v1/internal/owners/by-api-key-hash/{hash}", OFFICER_API_KEY_HASH)
                        .with(authentication(serviceAuth())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(SOME_OWNER.ownerId().toString()))
                .andExpect(jsonPath("$.authority").value(Roles.COMPLIANCE_OFFICER));
    }

    @Test
    void shouldReturn404ForUnknownApiKeyHash() throws Exception {
        // given
        given(ownerRepository.findByApiKeyHash("unknown-hash")).willReturn(Optional.empty());

        // when / then
        mockMvc.perform(get("/api/v1/internal/owners/by-api-key-hash/{hash}", "unknown-hash")
                        .with(authentication(serviceAuth())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ARCPAY-IDENTITY-0002"));
    }

    @Test
    void shouldReturn403WithoutServiceAuth() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/internal/owners/by-api-key-hash/{hash}", SOME_API_KEY_HASH))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn403WithOwnerAuth() throws Exception {
        // when / then
        mockMvc.perform(get("/api/v1/internal/owners/by-api-key-hash/{hash}", SOME_API_KEY_HASH)
                        .with(authentication(ownerAuth())))
                .andExpect(status().isForbidden());
    }
}
