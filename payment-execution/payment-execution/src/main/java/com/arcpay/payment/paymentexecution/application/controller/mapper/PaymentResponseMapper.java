package com.arcpay.payment.paymentexecution.application.controller.mapper;

import com.arcpay.payment.paymentexecution.api.model.PaymentListResponse;
import com.arcpay.payment.paymentexecution.api.model.PaymentResponse;
import com.arcpay.payment.paymentexecution.domain.model.Payment;
import com.arcpay.payment.paymentexecution.domain.model.PaymentStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface PaymentResponseMapper {

    @Mapping(target = "from", ignore = true)
    @Mapping(target = "to", source = "recipientAddress")
    @Mapping(target = "chain", constant = "ARC")
    @Mapping(target = "transactionHash", source = "txHash")
    @Mapping(target = "policyResult", ignore = true)
    @Mapping(target = "complianceResult", ignore = true)
    @Mapping(target = "receipt", ignore = true)
    PaymentResponse toApi(Payment payment);

    default String map(PaymentStatus status) {
        return status == null ? null : status.name();
    }

    default PaymentListResponse toApi(Page<Payment> page) {
        var content = page.getContent().stream().map(this::toApi).toList();
        return PaymentListResponse.builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
