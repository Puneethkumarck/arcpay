package com.arcpay.identity.agentidentity.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RequiredArgsConstructor
public class ServiceAuthFilter extends OncePerRequestFilter {

    private static final String SERVICE_AUTH_HEADER = "X-Service-Auth";

    private final String serviceToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null
                && serviceToken != null && !serviceToken.isBlank()) {
            var token = request.getHeader(SERVICE_AUTH_HEADER);
            if (token != null && constantTimeEquals(token, serviceToken)) {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.SERVICE));
                var authentication = new UsernamePasswordAuthenticationToken("service", null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
