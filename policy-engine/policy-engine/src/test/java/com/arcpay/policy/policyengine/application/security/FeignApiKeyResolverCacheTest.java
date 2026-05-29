package com.arcpay.policy.policyengine.application.security;

import com.arcpay.identity.client.IdentityServiceClient;
import com.arcpay.platform.api.OwnerPrincipal;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Optional;

import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_API_KEY_HASH;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_EMAIL;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_OWNER_ID;
import static com.arcpay.policy.policyengine.test.fixtures.IdentityFixtures.SOME_OWNER_PRINCIPAL_RESPONSE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.times;
import static org.mockito.Mockito.mock;

class FeignApiKeyResolverCacheTest {

    static IdentityServiceClient IDENTITY_CLIENT;

    private AnnotationConfigApplicationContext context;

    @BeforeEach
    void setUp() {
        IDENTITY_CLIENT = mock(IdentityServiceClient.class);
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    void shouldCacheResultsAndNotCallIdentityTwiceForSameHash() {
        // given
        given(IDENTITY_CLIENT.resolveApiKey(SOME_API_KEY_HASH))
                .willReturn(Optional.of(SOME_OWNER_PRINCIPAL_RESPONSE));
        // @EnableCaching produces a JDK proxy of the ApiKeyResolver interface, so fetch by interface
        ApiKeyResolver resolver = context.getBean(ApiKeyResolver.class);

        // when — resolve the same hash twice
        var first = resolver.resolve(SOME_API_KEY_HASH);
        var second = resolver.resolve(SOME_API_KEY_HASH);

        // then — Identity Service is called only once; second hit is served from cache
        var expected = Optional.of(new OwnerPrincipal(SOME_OWNER_ID, SOME_EMAIL));
        assertThat(first).usingRecursiveComparison().isEqualTo(expected);
        assertThat(second).usingRecursiveComparison().isEqualTo(expected);
        then(IDENTITY_CLIENT).should(times(1)).resolveApiKey(SOME_API_KEY_HASH);
    }

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        public CacheManager cacheManager() {
            var manager = new CaffeineCacheManager("apiKeyResolution");
            manager.setCaffeine(Caffeine.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(Duration.ofSeconds(60)));
            return manager;
        }

        @Bean
        public FeignApiKeyResolver feignApiKeyResolver() {
            return new FeignApiKeyResolver(IDENTITY_CLIENT);
        }
    }
}
