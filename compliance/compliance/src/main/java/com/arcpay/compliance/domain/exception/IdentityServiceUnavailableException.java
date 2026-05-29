package com.arcpay.compliance.domain.exception;

import java.util.UUID;

public class IdentityServiceUnavailableException extends RuntimeException {

    public IdentityServiceUnavailableException(UUID agentId, Throwable cause) {
        super("Identity service unavailable while resolving owner for agent: " + agentId, cause);
    }
}
