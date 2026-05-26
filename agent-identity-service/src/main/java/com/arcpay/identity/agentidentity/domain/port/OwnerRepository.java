package com.arcpay.identity.agentidentity.domain.port;

import com.arcpay.identity.agentidentity.domain.model.Owner;

import java.util.Optional;
import java.util.UUID;

public interface OwnerRepository {

    Owner save(Owner owner);

    Optional<Owner> findById(UUID ownerId);

    Optional<Owner> findByApiKeyHash(String apiKeyHash);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByWalletAddressIgnoreCase(String walletAddress);
}
