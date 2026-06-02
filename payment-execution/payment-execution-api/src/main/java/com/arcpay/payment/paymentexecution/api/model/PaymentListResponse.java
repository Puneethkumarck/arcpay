package com.arcpay.payment.paymentexecution.api.model;

import java.util.List;
import lombok.Builder;

@Builder
public record PaymentListResponse(
        List<PaymentResponse> content, int page, int size, long totalElements, int totalPages) {}
