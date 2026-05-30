package com.arcpay.payment.paymentexecution.domain.model;

public enum PaymentStatus {
    PENDING,
    POLICY_CHECK,
    SCREENING,
    HELD,
    EXECUTING,
    COMPLETED,
    FAILED,
    REJECTED
}
