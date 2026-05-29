package com.arcpay.identity.client;

import com.arcpay.identity.agentidentity.api.model.UpdateAgentPolicyRequest;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityClientFallbackFactoryTest {

    private static final UUID SOME_AGENT_ID = UUID.fromString("019576a0-0000-7000-8000-000000000002");
    private static final String SOME_HASH = "abc123hash";
    private static final String SOME_POLICY_HASH = "0xabc123def456";

    private final IdentityClientFallbackFactory factory = new IdentityClientFallbackFactory();

    @Test
    void shouldRethrowNotFoundFromGetAgentWhenCauseIsNotFound() {
        // given
        var notFound = feignNotFound();
        var fallback = factory.create(notFound);

        // when / then
        assertThatThrownBy(() -> fallback.getAgent(SOME_AGENT_ID))
                .isSameAs(notFound);
    }

    @Test
    void shouldThrowUnavailableFromGetAgentWhenCauseIsOtherFailure() {
        // given
        var cause = feignServerError();
        var fallback = factory.create(cause);

        // when / then
        assertThatThrownBy(() -> fallback.getAgent(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceCallException.class)
                .hasCause(cause);
    }

    @Test
    void shouldThrowUnavailableFromUpdatePolicyWhenCauseIsOtherFailure() {
        // given
        var cause = new IllegalStateException("boom");
        var fallback = factory.create(cause);
        var request = new UpdateAgentPolicyRequest(SOME_POLICY_HASH);

        // when / then
        assertThatThrownBy(() -> fallback.updatePolicy(SOME_AGENT_ID, request))
                .isInstanceOf(IdentityServiceCallException.class)
                .hasCause(cause);
    }

    @Test
    void shouldReturnEmptyFromResolveApiKeyForNotFoundCause() {
        // given
        var fallback = factory.create(feignNotFound());

        // when
        var result = fallback.resolveApiKey(SOME_HASH);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnEmptyFromResolveApiKeyForOtherCause() {
        // given
        var fallback = factory.create(feignServerError());

        // when
        var result = fallback.resolveApiKey(SOME_HASH);

        // then
        assertThat(result).isEmpty();
    }

    private static FeignException.NotFound feignNotFound() {
        return new FeignException.NotFound("Not Found", someRequest(), null, Collections.emptyMap());
    }

    private static FeignException feignServerError() {
        return new FeignException.InternalServerError("Internal Server Error",
                someRequest(), null, Collections.emptyMap());
    }

    private static Request someRequest() {
        return Request.create(Request.HttpMethod.GET, "/test",
                Collections.emptyMap(), null, StandardCharsets.UTF_8, null);
    }
}
