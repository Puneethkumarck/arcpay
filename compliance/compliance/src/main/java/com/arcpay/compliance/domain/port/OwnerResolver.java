package com.arcpay.compliance.domain.port;

import java.util.UUID;

public interface OwnerResolver {

    UUID resolveOwner(UUID agentId);
}
