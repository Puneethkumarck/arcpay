package com.arcpay.policy.policyengine.application.security;

import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.platform.infrastructure.security.ServiceAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final String serviceToken;

    public SecurityConfig(
            @Value("${arcpay.security.service-token:}") String serviceToken) {
        this.serviceToken = serviceToken;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole(Roles.SERVICE)
                        .anyRequest().authenticated())
                .addFilterBefore(serviceAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public ServiceAuthFilter serviceAuthFilter() {
        return new ServiceAuthFilter(serviceToken);
    }
}
