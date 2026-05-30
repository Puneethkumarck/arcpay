package com.arcpay.payment.paymentexecution.infrastructure.db;

import com.arcpay.payment.paymentexecution.domain.model.Payment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentEntityMapper {

    PaymentEntity mapToEntity(Payment domain);

    Payment mapToDomain(PaymentEntity entity);
}
