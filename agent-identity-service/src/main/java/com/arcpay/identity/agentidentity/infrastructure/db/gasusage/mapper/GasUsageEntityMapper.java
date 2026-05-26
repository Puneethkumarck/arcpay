package com.arcpay.identity.agentidentity.infrastructure.db.gasusage.mapper;

import com.arcpay.identity.agentidentity.domain.model.GasUsage;
import com.arcpay.identity.agentidentity.infrastructure.db.gasusage.GasUsageEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GasUsageEntityMapper {

    GasUsageEntity mapToEntity(GasUsage gasUsage);

    GasUsage mapToDomain(GasUsageEntity entity);
}
