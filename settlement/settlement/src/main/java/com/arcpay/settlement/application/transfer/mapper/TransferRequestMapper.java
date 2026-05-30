package com.arcpay.settlement.application.transfer.mapper;

import com.arcpay.settlement.api.model.TransferRequest;
import com.arcpay.settlement.domain.model.TransferCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransferRequestMapper {

    TransferCommand toDomain(TransferRequest request);
}
