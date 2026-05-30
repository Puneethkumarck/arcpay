package com.arcpay.payment.paymentexecution.infrastructure.db;

import com.arcpay.payment.paymentexecution.domain.model.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PaymentEntityMapper {

    @Mapping(target = "metadata", ignore = true)
    PaymentEntity mapToEntity(Payment domain);

    Payment mapToDomain(PaymentEntity entity);
}
