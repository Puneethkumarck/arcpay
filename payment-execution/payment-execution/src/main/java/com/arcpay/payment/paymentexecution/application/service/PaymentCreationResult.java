package com.arcpay.payment.paymentexecution.application.service;

import com.arcpay.payment.paymentexecution.domain.model.Payment;

public record PaymentCreationResult(Payment payment, boolean created) {
}
