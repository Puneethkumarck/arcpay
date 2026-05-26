package com.arcpay.identity.agentidentity.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    private final RateLimitFilter rateLimitFilter = new RateLimitFilter();

    @Mock
    private FilterChain filterChain;

    @Test
    void shouldPassThroughUnderLimit() throws ServletException, IOException {
        // given
        var request = createRegistrationRequest("192.168.1.1");
        var response = new MockHttpServletResponse();

        // when
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // then
        then(filterChain).should().doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldReturn429AtLimit() throws ServletException, IOException {
        // given
        var ip = "192.168.1.2";
        for (int i = 0; i < 10; i++) {
            var req = createRegistrationRequest(ip);
            var resp = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(req, resp, filterChain);
        }

        var request = createRegistrationRequest(ip);
        var response = new MockHttpServletResponse();

        // when
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(429);
        then(filterChain).should(never()).doFilter(request, response);
    }

    @Test
    void shouldTrackDifferentIpsIndependently() throws ServletException, IOException {
        // given
        for (int i = 0; i < 10; i++) {
            rateLimitFilter.doFilterInternal(
                    createRegistrationRequest("192.168.1.3"),
                    new MockHttpServletResponse(), filterChain);
        }

        var request = createRegistrationRequest("192.168.1.4");
        var response = new MockHttpServletResponse();

        // when
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // then
        assertThat(response.getStatus()).isEqualTo(200);
        then(filterChain).should().doFilter(request, response);
    }

    @Test
    void shouldNotRateLimitNonRegistrationEndpoints() throws ServletException, IOException {
        // given
        var request = new MockHttpServletRequest("GET", "/api/v1/agents");
        request.setRemoteAddr("192.168.1.5");
        var response = new MockHttpServletResponse();

        // when
        rateLimitFilter.doFilterInternal(request, response, filterChain);

        // then
        then(filterChain).should().doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest createRegistrationRequest(String ip) {
        var request = new MockHttpServletRequest("POST", "/api/v1/owners/register");
        request.setRemoteAddr(ip);
        return request;
    }
}
