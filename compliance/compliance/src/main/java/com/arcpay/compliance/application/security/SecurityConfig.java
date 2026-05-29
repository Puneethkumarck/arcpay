package com.arcpay.compliance.application.security;

import com.arcpay.compliance.api.ErrorCodes;
import com.arcpay.platform.api.ApiError;
import com.arcpay.platform.infrastructure.security.ApiKeyAuthFilter;
import com.arcpay.platform.infrastructure.security.ApiKeyResolver;
import com.arcpay.platform.infrastructure.security.Roles;
import com.arcpay.platform.infrastructure.security.ServiceAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ApiKeyResolver apiKeyResolver;
    private final JsonMapper jsonMapper;
    private final String serviceToken;

    public SecurityConfig(
            ApiKeyResolver apiKeyResolver,
            JsonMapper jsonMapper,
            @Value("${arcpay.security.service-token:}") String serviceToken) {
        this.apiKeyResolver = apiKeyResolver;
        this.jsonMapper = jsonMapper;
        this.serviceToken = serviceToken;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                        .requestMatchers("/api/v1/internal/**").hasRole(Roles.SERVICE)
                        .requestMatchers("/compliance/watchlist/**", "/compliance/watchlist")
                        .hasRole(Roles.COMPLIANCE_OFFICER)
                        .requestMatchers(HttpMethod.GET, "/compliance/screenings/**")
                        .hasRole(Roles.COMPLIANCE_OFFICER)
                        .requestMatchers(HttpMethod.GET, "/compliance/holds", "/compliance/holds/**")
                        .hasRole(Roles.COMPLIANCE_OFFICER)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                        .accessDeniedHandler(accessDeniedHandler()))
                .addFilterBefore(apiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(serviceAuthFilter(), ApiKeyAuthFilter.class)
                .build();
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            var error = ApiError.builder()
                    .code(ErrorCodes.NOT_AUTHORIZED)
                    .status(HttpStatus.FORBIDDEN.getReasonPhrase())
                    .message(accessDeniedException.getMessage())
                    .build();
            jsonMapper.writeValue(response.getOutputStream(), error);
        };
    }

    @Bean
    public ApiKeyAuthFilter apiKeyAuthFilter() {
        return new ApiKeyAuthFilter(apiKeyResolver);
    }

    @Bean
    public ServiceAuthFilter serviceAuthFilter() {
        return new ServiceAuthFilter(serviceToken);
    }
}
