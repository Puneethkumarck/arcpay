package com.arcpay.settlement.infrastructure.db;

import com.arcpay.settlement.domain.model.SettlementTransaction;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface SettlementTransactionEntityMapper {

    SettlementTransactionEntity mapToEntity(SettlementTransaction domain);

    SettlementTransaction mapToDomain(SettlementTransactionEntity entity);
}
