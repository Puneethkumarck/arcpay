package com.arcpay.payment.paymentexecution.application.config;

import com.arcpay.identity.client.IdentityClientFallbackFactory;
import com.arcpay.platform.infrastructure.security.ServiceAuthFeignInterceptor;
import com.arcpay.policy.client.PolicyEngineClientFallbackFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = {
        "com.arcpay.identity.client",
        "com.arcpay.policy.client",
        "com.arcpay.payment.paymentexecution.infrastructure.client.settlement"
})
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

    @Bean
    public PolicyEngineClientFallbackFactory policyEngineClientFallbackFactory() {
        return new PolicyEngineClientFallbackFactory();
    }
}
