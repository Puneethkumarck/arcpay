package com.arcpay.policy.policyengine.domain.spending;

import com.arcpay.policy.policyengine.domain.port.SpendingLockRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpendingLockService {

    private final SpendingLockRepository spendingLockRepository;

    @Transactional
    public void acquireLock(UUID agentId) {
        spendingLockRepository.createIfNotExists(agentId);
        spendingLockRepository.acquireLock(agentId);
        log.debug("Acquired spending lock for agentId={}", agentId);
    }
}
