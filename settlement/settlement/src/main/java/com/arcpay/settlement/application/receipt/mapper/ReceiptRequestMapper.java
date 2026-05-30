package com.arcpay.settlement.application.receipt.mapper;

import com.arcpay.settlement.api.model.ReceiptRequest;
import com.arcpay.settlement.domain.model.ReceiptCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ReceiptRequestMapper {

    ReceiptCommand toDomain(ReceiptRequest request);
}
