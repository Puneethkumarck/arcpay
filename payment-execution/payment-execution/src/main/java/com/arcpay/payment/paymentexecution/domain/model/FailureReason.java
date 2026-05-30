package com.arcpay.payment.paymentexecution.domain.model;

public enum FailureReason {
    POLICY_UNAVAILABLE,
    INSUFFICIENT_BALANCE,
    EXECUTION_ERROR,
    CHAIN_TIMEOUT,
    EXECUTION_REVERTED,
    SCREENING_UNAVAILABLE
}
