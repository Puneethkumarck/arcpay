package com.arcpay.compliance.application.config;

import com.arcpay.compliance.domain.model.ScreeningThreshold;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ScreeningConfig {

    @Bean
    ScreeningThreshold screeningThreshold(@Value("${compliance.screening.hold-threshold:50}") int holdThreshold) {
        return ScreeningThreshold.builder().holdThreshold(holdThreshold).build();
    }
}
