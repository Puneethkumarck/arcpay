package com.arcpay.settlement.application.security;

import com.arcpay.settlement.test.FullContextIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Import(SecurityConfigIntegrationTest.TestEndpoints.class)
class SecurityConfigIntegrationTest extends FullContextIntegrationTest {

    private static final String SERVICE_TOKEN = "test-service-token";

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldAllowHealthEndpointWithoutAuth() throws Exception {
        // given — no credentials

        // when / then — health is public
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectInternalEndpointWithoutServiceToken() throws Exception {
        // given — no credentials

        // when / then — internal endpoints require ROLE_SERVICE → 401
        mockMvc.perform(get("/api/v1/internal/test/ping"))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void shouldRejectInternalEndpointWithInvalidServiceToken() throws Exception {
        // given — a wrong X-Service-Auth token

        // when / then — invalid token grants no authority → 401
        mockMvc.perform(get("/api/v1/internal/test/ping")
                        .header("X-Service-Auth", "wrong-token"))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void shouldAuthenticateInternalEndpointWithServiceToken() throws Exception {
        // given — a valid X-Service-Auth token

        // when / then — service-to-service auth grants ROLE_SERVICE
        mockMvc.perform(get("/api/v1/internal/test/ping")
                        .header("X-Service-Auth", SERVICE_TOKEN))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReachWebhookWithoutServiceToken() throws Exception {
        // given — Circle sends no service token, only its signature headers

        // when / then — the webhook chain is permitAll; a missing signature is rejected
        // by the signature verifier (401), not by Spring Security
        mockMvc.perform(post("/api/v1/webhooks/circle")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @Test
    void shouldDenyUnknownPath() throws Exception {
        // given — no credentials on a path outside the internal/webhook matchers

        // when / then — anyRequest().authenticated() → 401
        mockMvc.perform(get("/api/v1/unknown"))
                .andExpect(status().is(HttpStatus.UNAUTHORIZED.value()));
    }

    @TestConfiguration
    static class TestEndpoints {

        @Bean
        TestController testController() {
            return new TestController();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/api/v1/internal/test/ping")
        String internalPing() {
            return "internal-pong";
        }
    }
}
