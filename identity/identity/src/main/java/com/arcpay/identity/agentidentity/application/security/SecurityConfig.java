package com.arcpay.identity.agentidentity.application.security;

import com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.platform.infrastructure.security.ServiceAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(IdentitySecurityProperties.class)
public class SecurityConfig {

    private final ApiKeyResolver apiKeyResolver;
    private final String serviceToken;

    public SecurityConfig(
            ApiKeyResolver apiKeyResolver,
            @Value("${arcpay.security.service-token:}") String serviceToken) {
        this.apiKeyResolver = apiKeyResolver;
        this.serviceToken = serviceToken;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/owners/register").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole(Roles.SERVICE)
                        .anyRequest().authenticated())
                .addFilterBefore(rateLimitFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(serviceAuthFilter(), ApiKeyAuthFilter.class)
                .build();
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(apiKeyResolver);
    }

    @Bean
    public ServiceAuthFilter serviceAuthFilter() {
        return new ServiceAuthFilter(serviceToken);
    }

    @Bean
    public RateLimitFilter rateLimitFilter() {
        return new RateLimitFilter();
    }
}
