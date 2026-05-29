package com.arcpay.compliance.domain.exception;

import java.util.UUID;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String principal, UUID agentId) {
        super("Principal " + principal + " is not authorized to review holds for agent: " + agentId);
    }
}
