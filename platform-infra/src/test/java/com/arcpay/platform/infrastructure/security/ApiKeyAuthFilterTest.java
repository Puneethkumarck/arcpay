package com.arcpay.platform.infrastructure.security;

import com.arcpay.platform.api.OwnerPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private ApiKeyResolver apiKeyResolver;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private ApiKeyAuthFilter apiKeyAuthFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldGrantComplianceOfficerRoleWhenPrincipalAuthorityIsComplianceOfficer() throws ServletException, IOException {
        // given
        var rawApiKey = "ak_officer_key";
        var hash = ApiKeyAuthFilter.hashApiKey(rawApiKey);
        var principal = new OwnerPrincipal(UUID.randomUUID(), "officer@example.com", Roles.COMPLIANCE_OFFICER);
        given(apiKeyResolver.resolve(hash)).willReturn(Optional.of(principal));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + rawApiKey);
        var response = new MockHttpServletResponse();

        // when
        apiKeyAuthFilter.doFilter(request, response, filterChain);

        // then
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_COMPLIANCE_OFFICER");
    }

    @Test
    void shouldGrantOwnerRoleWhenPrincipalAuthorityIsOwner() throws ServletException, IOException {
        // given
        var rawApiKey = "ak_owner_key";
        var hash = ApiKeyAuthFilter.hashApiKey(rawApiKey);
        var principal = new OwnerPrincipal(UUID.randomUUID(), "owner@example.com");
        given(apiKeyResolver.resolve(hash)).willReturn(Optional.of(principal));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + rawApiKey);
        var response = new MockHttpServletResponse();

        // when
        apiKeyAuthFilter.doFilter(request, response, filterChain);

        // then
        var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_OWNER");
    }
}
