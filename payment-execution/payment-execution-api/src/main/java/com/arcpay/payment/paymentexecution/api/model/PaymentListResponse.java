package com.arcpay.payment.paymentexecution.api.model;

import lombok.Builder;

import java.util.List;

@Builder
public record PaymentListResponse(
        List<PaymentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
