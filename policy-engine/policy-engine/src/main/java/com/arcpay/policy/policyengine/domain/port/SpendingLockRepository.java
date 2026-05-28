package com.arcpay.policy.policyengine.domain.port;

import java.util.UUID;

public interface SpendingLockRepository {

    void acquireLock(UUID agentId);

    void createIfNotExists(UUID agentId);
}
