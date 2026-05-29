package com.arcpay.policy.policyengine.application.config;

import com.arcpay.identity.client.IdentityClientFallbackFactory;
import com.arcpay.platform.infrastructure.security.ServiceAuthFeignInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.arcpay.identity.client")
public class FeignConfig {

    @Bean
    public ServiceAuthFeignInterceptor serviceAuthFeignInterceptor(
            @Value("${arcpay.security.service-token:}") String serviceToken) {
        return new ServiceAuthFeignInterceptor(serviceToken);
    }

    @Bean
    public IdentityClientFallbackFactory identityClientFallbackFactory() {
        return new IdentityClientFallbackFactory();
    }
}
