package com.arcpay.compliance.infrastructure.client.identity;

import com.arcpay.compliance.domain.exception.IdentityServiceUnavailableException;
import com.arcpay.identity.client.IdentityServiceClient;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.arcpay.compliance.fixtures.ComplianceFixtures.SOME_AGENT_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_AGENT_RESPONSE;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.compliance.fixtures.IdentityFixtures.clientUnavailable;
import static com.arcpay.compliance.fixtures.IdentityFixtures.feignNotFound;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class IdentityServiceAdapterTest {

    @Mock
    private IdentityServiceClient identityClient;

    private IdentityServiceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new IdentityServiceAdapter(identityClient);
    }

    @Test
    void shouldResolveOwnerForAgent() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID)).willReturn(SOME_AGENT_RESPONSE);

        // when
        var result = adapter.resolveOwner(SOME_AGENT_ID);

        // then
        assertThat(result).isEqualTo(SOME_OWNER_ID);
    }

    @Test
    void shouldRethrowNotFoundWhenAgentMissing() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID)).willThrow(feignNotFound());

        // when / then
        assertThatThrownBy(() -> adapter.resolveOwner(SOME_AGENT_ID))
                .isInstanceOf(FeignException.NotFound.class);
    }

    @Test
    void shouldThrowDomainUnavailableWhenClientUnavailable() {
        // given
        given(identityClient.getAgent(SOME_AGENT_ID)).willThrow(clientUnavailable());

        // when / then
        assertThatThrownBy(() -> adapter.resolveOwner(SOME_AGENT_ID))
                .isInstanceOf(IdentityServiceUnavailableException.class)
                .hasMessageContaining(SOME_AGENT_ID.toString());
    }
}
