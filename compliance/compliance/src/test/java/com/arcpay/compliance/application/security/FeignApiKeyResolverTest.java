package com.arcpay.compliance.application.security;

import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_API_KEY_HASH;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_EMAIL;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OFFICER_PRINCIPAL_RESPONSE;
import static com.arcpay.compliance.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FeignApiKeyResolverTest {

    @Mock
    private IdentityServiceClient identityClient;

    @InjectMocks
    private FeignApiKeyResolver feignApiKeyResolver;

    @Test
    void shouldResolveApiKeyAndPropagateAuthority() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.of(SOME_OFFICER_PRINCIPAL_RESPONSE));

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        var expected = Optional.of(new OwnerPrincipal(SOME_OWNER_ID, SOME_OFFICER_EMAIL, "COMPLIANCE_OFFICER"));
        assertThat(result)
                .usingRecursiveComparison()
                .isEqualTo(expected);
    }

    @Test
    void shouldReturnEmptyWhenClientResolvesToNoOwner() {
        // given
        given(identityClient.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.empty());

        // when
        var result = feignApiKeyResolver.resolve(SOME_API_KEY_HASH);

        // then
        assertThat(result).isEmpty();
    }
}
