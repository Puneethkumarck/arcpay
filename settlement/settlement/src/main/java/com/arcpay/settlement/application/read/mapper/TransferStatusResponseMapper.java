package com.arcpay.settlement.application.read.mapper;

import com.arcpay.settlement.api.model.TransferStatusResponse;
import com.arcpay.settlement.domain.model.SettlementTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferStatusResponseMapper {

    @Mapping(target = "state", expression = "java(transaction.state().name())")
    TransferStatusResponse toApi(SettlementTransaction transaction);
}
