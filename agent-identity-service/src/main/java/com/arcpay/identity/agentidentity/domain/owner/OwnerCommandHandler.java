package com.arcpay.identity.agentidentity.domain.owner;

import com.arcpay.identity.agentidentity.domain.event.OwnerRegistered;
import com.arcpay.identity.agentidentity.domain.port.EventPublisher;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class OwnerCommandHandler {

    private final OwnerValidator ownerValidator;
    private final OwnerCreationService ownerCreationService;
    private final OwnerRepository ownerRepository;
    private final EventPublisher eventPublisher;

    @Transactional
    public OwnerCreationService.OwnerWithApiKey registerOwner(String email, String walletAddress) {
        ownerValidator.validateRegistration(email, walletAddress);
        var result = ownerCreationService.createOwner(email, walletAddress);
        var savedOwner = ownerRepository.save(result.owner());
        eventPublisher.publish(new OwnerRegistered(
                savedOwner.ownerId(),
                savedOwner.email(),
                savedOwner.walletAddress(),
                Instant.now()));
        log.info("Owner registered ownerId={}", savedOwner.ownerId());
        return new OwnerCreationService.OwnerWithApiKey(savedOwner, result.rawApiKey());
    }
}
