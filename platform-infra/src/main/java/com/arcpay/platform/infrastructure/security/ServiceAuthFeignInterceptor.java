package com.arcpay.platform.infrastructure.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;

/**
 * Attaches the {@code X-Service-Auth} header to every outbound Feign request so that
 * downstream services can authenticate this service via their inbound {@link ServiceAuthFilter}.
 *
 * <p>The header name and shared-secret convention mirror {@link ServiceAuthFilter} exactly:
 * the same {@code arcpay.security.service-token} value validated inbound is sent outbound.
 */
@RequiredArgsConstructor
public class ServiceAuthFeignInterceptor implements RequestInterceptor {

    private static final String SERVICE_AUTH_HEADER = "X-Service-Auth";

    private final String serviceToken;

    @Override
    public void apply(RequestTemplate template) {
        if (serviceToken != null && !serviceToken.isBlank()) {
            template.header(SERVICE_AUTH_HEADER, serviceToken);
        }
    }
}
