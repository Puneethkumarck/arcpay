package com.arcpay.identity.agentidentity.infrastructure.db.owner;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.domain.port.OwnerRepository;
import com.arcpay.identity.agentidentity.infrastructure.db.owner.mapper.OwnerEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class OwnerRepositoryAdapter implements OwnerRepository {

    private final OwnerJpaRepository jpaRepository;
    private final OwnerEntityMapper mapper;

    @Override
    public Owner save(Owner owner) {
        var entity = mapper.mapToEntity(owner);
        return mapper.mapToDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<Owner> findById(UUID ownerId) {
        return jpaRepository.findById(ownerId).map(mapper::mapToDomain);
    }

    @Override
    public Optional<Owner> findByApiKeyHash(String apiKeyHash) {
        return jpaRepository.findByApiKeyHash(apiKeyHash).map(mapper::mapToDomain);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return jpaRepository.existsByEmailIgnoreCase(email);
    }

    @Override
    public boolean existsByWalletAddressIgnoreCase(String walletAddress) {
        return jpaRepository.existsByWalletAddressIgnoreCase(walletAddress);
    }
}
