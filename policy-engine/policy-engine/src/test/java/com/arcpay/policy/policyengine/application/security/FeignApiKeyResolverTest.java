package com.arcpay.policy.policyengine.application.security;

import com.arcpay.identity.agentidentity.api.model.OwnerPrincipalResponse;
import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeignApiKeyResolverTest {

    @Mock
    private IdentityServiceClient identityClient;

    @InjectMocks
    private FeignApiKeyResolver feignApiKeyResolver;

    private static final String SOME_API_KEY_HASH = "abc123hash";
    private static final UUID SOME_OWNER_ID = UUID.fromString("019576a0-0000-7000-8000-000000000001");
    private static final String SOME_EMAIL = "owner@example.com";

    @Test
    void shouldResolveApiKeyAndReturnOwnerPrincipal() {
        // given
        var ownerResponse = OwnerPrincipalResponse.builder()
                .ownerId(SOME_OWNER_ID)
                .email(SOME_EMAIL)
                .build();
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.of(ownerResponse));

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        var expected = Optional.of(new OwnerPrincipal(SOME_OWNER_ID, SOME_EMAIL));
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyForUnknownKey() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.empty());

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        assertThat(result).isEmpty();
    }
}
