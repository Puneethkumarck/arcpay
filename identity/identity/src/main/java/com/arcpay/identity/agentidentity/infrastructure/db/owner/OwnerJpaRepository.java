package com.arcpay.identity.agentidentity.infrastructure.db.owner;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OwnerJpaRepository extends JpaRepository<OwnerEntity, UUID> {

    Optional<OwnerEntity> findByApiKeyHash(String apiKeyHash);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByWalletAddressIgnoreCase(String walletAddress);
}
