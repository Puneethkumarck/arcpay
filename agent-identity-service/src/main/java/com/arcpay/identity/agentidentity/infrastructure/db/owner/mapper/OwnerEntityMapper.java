package com.arcpay.identity.agentidentity.infrastructure.db.owner.mapper;

import com.arcpay.identity.agentidentity.domain.model.Owner;
import com.arcpay.identity.agentidentity.infrastructure.db.owner.OwnerEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OwnerEntityMapper {

    OwnerEntity mapToEntity(Owner owner);

    Owner mapToDomain(OwnerEntity entity);
}
