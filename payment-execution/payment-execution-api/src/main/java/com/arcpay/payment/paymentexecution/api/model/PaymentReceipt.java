package com.arcpay.payment.paymentexecution.api.model;

import java.time.Instant;
import lombok.Builder;

@Builder
public record PaymentReceipt(String onChainRef, Long blockNumber, Instant timestamp) {}
