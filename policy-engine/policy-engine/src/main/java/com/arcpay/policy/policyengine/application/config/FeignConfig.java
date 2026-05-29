package com.arcpay.policy.policyengine.application.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.arcpay.identity.client")
public class FeignConfig {
}
