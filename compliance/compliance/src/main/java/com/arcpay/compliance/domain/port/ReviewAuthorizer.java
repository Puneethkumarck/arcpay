package com.arcpay.compliance.domain.port;

import java.util.UUID;

public interface ReviewAuthorizer {

    boolean canReview(String principal, String role, UUID agentId);
}
