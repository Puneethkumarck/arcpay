package com.arcpay.platform.infrastructure.security;

import com.arcpay.platform.api.OwnerPrincipal;

import java.util.Optional;

/**
 * Resolves an API key hash to an OwnerPrincipal.
 * Each service provides its own implementation (e.g., via OwnerRepository lookup).
 */
public interface ApiKeyResolver {

    Optional<OwnerPrincipal> resolve(String apiKeyHash);
}
