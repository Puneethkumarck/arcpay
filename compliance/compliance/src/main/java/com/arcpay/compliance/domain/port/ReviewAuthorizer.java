package com.arcpay.compliance.domain.port;

import java.util.UUID;

public interface ReviewAuthorizer {

    boolean canReview(String principal, UUID agentId);
}
