package com.arcpay.identity.agentidentity.domain.exception;

import java.util.UUID;

public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String resource, UUID ownerId) {
        super("Access denied to " + resource + " for owner " + ownerId);
    }
}
