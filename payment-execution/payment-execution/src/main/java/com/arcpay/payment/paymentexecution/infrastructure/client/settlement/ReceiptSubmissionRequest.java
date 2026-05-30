package com.arcpay.payment.paymentexecution.infrastructure.client.settlement;

import lombok.Builder;

import java.util.UUID;

@Builder
record ReceiptSubmissionRequest(UUID paymentId) {}
