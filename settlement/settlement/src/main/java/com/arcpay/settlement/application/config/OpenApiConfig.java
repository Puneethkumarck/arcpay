package com.arcpay.settlement.application.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi(@Value("${spring.application.name}") String applicationName) {
        return new OpenAPI().info(new Info().title(applicationName + " API").version("v1"));
    }
}
