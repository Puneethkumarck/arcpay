package com.arcpay.platform.infrastructure.security;

import com.arcpay.platform.api.OwnerPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyResolver apiKeyResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            var rawApiKey = authHeader.substring(BEARER_PREFIX.length());
            var hash = hashApiKey(rawApiKey);
            var principalOpt = apiKeyResolver.resolve(hash);
            if (principalOpt.isPresent()) {
                var principal = principalOpt.get();
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + Roles.OWNER));
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("API key authentication failed — no owner found for provided key");
            }
        }
        filterChain.doFilter(request, response);
    }

    public static String hashApiKey(String rawApiKey) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var hashBytes = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
