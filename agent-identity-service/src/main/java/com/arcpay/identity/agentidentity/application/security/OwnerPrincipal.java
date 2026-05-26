package com.arcpay.identity.agentidentity.application.security;

import java.util.UUID;

public record OwnerPrincipal(UUID ownerId, String email) {}
