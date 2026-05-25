package com.arcpay.identity.agentidentity.infrastructure.db.gasusage;

import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import com.arcpay.identity.agentidentity.domain.port.GasUsageRepository;
import com.arcpay.identity.agentidentity.infrastructure.db.gasusage.mapper.GasUsageEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class GasUsageRepositoryAdapter implements GasUsageRepository {

    private final GasUsageJpaRepository jpaRepository;
    private final GasUsageEntityMapper mapper;

    @Override
    public GasUsage save(GasUsage gasUsage) {
        var entity = mapper.mapToEntity(gasUsage);
        return mapper.mapToDomain(jpaRepository.save(entity));
    }

    @Override
    public Page<GasUsage> findByOwnerId(UUID ownerId, Pageable pageable) {
        return jpaRepository.findByOwnerId(ownerId, pageable).map(mapper::mapToDomain);
    }
}
