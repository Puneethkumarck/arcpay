package com.arcpay.identity.agentidentity.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_REQUESTS_PER_HOUR = 10;
    private static final long WINDOW_SECONDS = 3600;
    private static final String RATE_LIMITED_PATH = "/api/v1/owners/register";

    private final ConcurrentMap<String, RateLimitEntry> rateLimits = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if ("POST".equalsIgnoreCase(request.getMethod()) && RATE_LIMITED_PATH.equals(request.getRequestURI())) {
            evictExpiredEntries();
            var clientIp = getClientIp(request);
            var entry = rateLimits.compute(clientIp, (key, existing) -> {
                if (existing == null || existing.isExpired()) {
                    return new RateLimitEntry();
                }
                return existing;
            });
            if (entry.incrementAndCheck() > MAX_REQUESTS_PER_HOUR) {
                log.warn("Rate limit exceeded for IP={}", clientIp);
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"code\":\"ARCPAY-IDENTITY-0007\",\"status\":429,\"message\":\"Too many requests\"}");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void evictExpiredEntries() {
        rateLimits.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    private String getClientIp(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static class RateLimitEntry {
        private final Instant windowStart = Instant.now();
        private final AtomicInteger count = new AtomicInteger(0);

        boolean isExpired() {
            return Instant.now().isAfter(windowStart.plusSeconds(WINDOW_SECONDS));
        }

        int incrementAndCheck() {
            return count.incrementAndGet();
        }
    }
}
