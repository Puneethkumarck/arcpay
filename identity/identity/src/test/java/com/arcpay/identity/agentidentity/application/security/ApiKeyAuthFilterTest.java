package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
    void shouldSetAuthenticationForValidApiKey() throws ServletException, IOException {
        // given
        var rawApiKey = "ak_test_valid123";
        var hash = hashKey(rawApiKey);
        var ownerId = UUID.randomUUID();
        var principal = new OwnerPrincipal(ownerId, "alice@example.com");
        given(apiKeyResolver.resolve(hash)).willReturn(Optional.of(principal));
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + rawApiKey);
        var response = new MockHttpServletResponse();

        // when
        apiKeyAuthFilter.doFilter(request, response, filterChain);

        // then
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isInstanceOf(OwnerPrincipal.class);
        var resolvedPrincipal = (OwnerPrincipal) auth.getPrincipal();
        assertThat(resolvedPrincipal.ownerId()).isEqualTo(ownerId);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void shouldReturn401ForInvalidApiKey() throws ServletException, IOException {
        // given
        var rawApiKey = "ak_test_invalid";
        var hash = hashKey(rawApiKey);
        given(apiKeyResolver.resolve(hash)).willReturn(Optional.empty());
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + rawApiKey);
        var response = new MockHttpServletResponse();

        // when
        apiKeyAuthFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void shouldContinueFilterChainWhenNoAuthorizationHeader() throws ServletException, IOException {
        // given
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();

        // when
        apiKeyAuthFilter.doFilter(request, response, filterChain);

        // then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        then(filterChain).should().doFilter(request, response);
    }

    private String hashKey(String rawKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawKey.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
