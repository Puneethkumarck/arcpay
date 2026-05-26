package com.arcpay.identity.agentidentity.infrastructure.db.idempotency.mapper;

import com.arcpay.identity.agentidentity.domain.model.IdempotencyKey;
import com.arcpay.identity.agentidentity.infrastructure.db.idempotency.IdempotencyKeyEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface IdempotencyKeyEntityMapper {

    IdempotencyKeyEntity mapToEntity(IdempotencyKey idempotencyKey);

    IdempotencyKey mapToDomain(IdempotencyKeyEntity entity);
}
